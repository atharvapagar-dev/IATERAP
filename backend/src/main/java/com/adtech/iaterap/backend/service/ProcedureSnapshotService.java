package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.coverage.CoverageEngine;
import com.adtech.iaterap.backend.coverage.CoverageScore;
import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.dto.TestStepResponse;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Creates submission-time, append-only artifacts. Payloads deliberately contain values, not live entity links. */
@Service
public class ProcedureSnapshotService {
    private final ProcedureSnapshotRepository snapshots; private final CoverageReportRepository reports;
    private final InterfaceRepository interfaces; private final SignalLibraryRepository signals; private final FaultTemplateRepository faults;
    private final FmeaBranchMappingRepository fmea; private final ObjectMapper mapper; private final CoverageEngine coverageEngine;
    public ProcedureSnapshotService(ProcedureSnapshotRepository snapshots, CoverageReportRepository reports, InterfaceRepository interfaces, SignalLibraryRepository signals, FaultTemplateRepository faults, FmeaBranchMappingRepository fmea, ObjectMapper mapper, CoverageEngine coverageEngine) { this.snapshots=snapshots; this.reports=reports; this.interfaces=interfaces; this.signals=signals; this.faults=faults; this.fmea=fmea; this.mapper=mapper; this.coverageEngine=coverageEngine; }

    public void createSubmissionArtifacts(TestProcedure procedure, List<TestStep> steps, String actor) {
        ProcedureSnapshot snapshot = snapshots.findByTestProcedureIdAndProcedureVersionNumber(procedure.getId(), procedure.getVersionNumber()).orElseGet(() -> snapshots.save(new ProcedureSnapshot(procedure.getId(), procedure.getVersionNumber(), procedure.getApprovalStatus(), payload(procedure, steps), actor)));
        if (reports.findByTestProcedureIdAndProcedureVersionNumberAndReportVersion(procedure.getId(), procedure.getVersionNumber(), 1).isEmpty()) reports.save(new CoverageReport(snapshot.getId(), procedure.getId(), procedure.getVersionNumber(), report(snapshot)));
    }
    public ProcedureSnapshot getSnapshot(Long procedureId, Integer version) { return snapshots.findByTestProcedureIdAndProcedureVersionNumber(procedureId, version).orElseThrow(() -> new NoSuchElementException("Procedure snapshot not found for procedure " + procedureId + " version " + version)); }
    public CoverageReport getReport(Long procedureId, Integer version) { return reports.findByTestProcedureIdAndProcedureVersionNumberAndReportVersion(procedureId, version, 1).orElseThrow(() -> new NoSuchElementException("Coverage report not found for procedure " + procedureId + " version " + version)); }

    private String payload(TestProcedure p, List<TestStep> steps) {
        ObjectNode root=mapper.createObjectNode(), procedure=root.putObject("procedure"); procedure.put("id",p.getId()); procedure.put("versionNumber",p.getVersionNumber()); procedure.put("approvalStatus",p.getApprovalStatus()); procedure.put("name",p.getName()); procedure.put("description",p.getDescription()); procedure.putObject("lru").put("id",p.getLru().getId()).put("name",p.getLru().getName());
        ArrayNode snapshotSteps=procedure.putArray("steps"); for(TestStep s:steps){ ObjectNode n=snapshotSteps.addObject(); n.setAll((ObjectNode) mapper.valueToTree(TestStepResponse.fromEntity(s))); asset(n,"interface",s.getInterfaceEntity()==null?null:s.getInterfaceEntity().getId(),s.getInterfaceEntity()==null?null:s.getInterfaceEntity().getName()); asset(n,"connector",s.getConnector()==null?null:s.getConnector().getId(),s.getConnector()==null?null:s.getConnector().getName()); if(s.getSignalLibrary()!=null){ObjectNode a=n.putObject("signal");a.put("id",s.getSignalLibrary().getId());a.put("name",s.getSignalLibrary().getName());a.put("description",s.getSignalLibrary().getDescription());a.put("engineeringUnit",s.getSignalLibrary().getEngineeringUnit());} if(s.getFaultCategory()!=null){ObjectNode a=n.putObject("fault");a.put("id",s.getFaultCategory().getId());a.put("name",s.getFaultCategory().getName());a.put("faultCode",s.getFaultCategory().getFaultCode());a.put("description",s.getFaultCategory().getDescription());a.put("severity",s.getFaultCategory().getSeverity());} }
        ObjectNode context=root.putObject("coverageContext"); ids(context.putArray("interfaces"),interfaces.findByLruId(p.getLru().getId()).stream().map(Interface::getId).toList()); ids(context.putArray("signals"),signals.findByLruId(p.getLru().getId()).stream().map(SignalLibrary::getId).toList()); ids(context.putArray("faults"),faults.findByLruId(p.getLru().getId()).stream().map(FaultTemplate::getId).toList()); ArrayNode mappings=context.putArray("fmeaMappings"); for(FmeaBranchMapping m:fmea.findByLruId(p.getLru().getId())) mappings.addObject().put("id",m.getId()).put("faultId",m.getFaultTemplate().getId()).put("branchCode",m.getFmeaBranchCode()); try{return mapper.writeValueAsString(root);}catch(Exception e){throw new IllegalStateException("Failed to serialize procedure snapshot",e);}
    }
    private void asset(ObjectNode step,String field,Long id,String name){if(id!=null)step.putObject(field).put("id",id).put("name",name);}
    private void ids(ArrayNode target,List<Long> ids){ids.forEach(target::add);}
    private String report(ProcedureSnapshot snapshot) { try { JsonNode root=mapper.readTree(snapshot.getSnapshotPayload()); JsonNode steps=root.path("procedure").path("steps"), ctx=root.path("coverageContext"); Map<String,RuleResult> results=new LinkedHashMap<>(); results.put("COVERAGE-INTERFACE",measured(ctx.path("interfaces"),refs(steps,"interface"))); results.put("COVERAGE-SIGNAL",measured(ctx.path("signals"),refs(steps,"signal"))); results.put("COVERAGE-FAULT",measured(ctx.path("faults"),refs(steps,"fault"))); Set<Long> faultRefs=refs(steps,"fault"), mapped=new HashSet<>(); for(JsonNode m:ctx.path("fmeaMappings"))if(faultRefs.contains(m.path("faultId").asLong()))mapped.add(m.path("id").asLong()); results.put("COVERAGE-FMEA",measured(ctx.path("fmeaMappings"),mapped)); results.put("COVERAGE-REQUIREMENT",unavailable()); results.put("COVERAGE-FUNCTIONAL",unavailable()); CoverageScore score=coverageEngine.calculate(results); ObjectNode out=mapper.valueToTree(score); out.put("procedureSnapshotId",snapshot.getId()); out.put("procedureVersionNumber",snapshot.getProcedureVersionNumber()); return mapper.writeValueAsString(out); } catch(Exception e){throw new IllegalStateException("Failed to generate frozen coverage report",e);} }
    private Set<Long> refs(JsonNode steps,String field){Set<Long> out=new HashSet<>();for(JsonNode s:steps)if(s.path(field).hasNonNull("id"))out.add(s.path(field).path("id").asLong());return out;}
    private RuleResult measured(JsonNode defined,Set<Long> references){Set<Long> ids=new HashSet<>();for(JsonNode n:defined)ids.add(n.isObject()?n.path("id").asLong():n.asLong()); Map<String,Object>d=new LinkedHashMap<>();if(ids.isEmpty()){d.put("percentage",null);d.put("reason","No defined LRU elements exist for this category");}else d.put("percentage",BigDecimal.valueOf(references.stream().filter(ids::contains).count()*100D/ids.size()).setScale(2,RoundingMode.HALF_UP));return RuleResult.pass("Frozen coverage",d).withMetadata("",RuleCategory.COVERAGE);}
    private RuleResult unavailable(){Map<String,Object> details=new LinkedHashMap<>(); details.put("percentage",null); details.put("reason","No LRU-scoped catalog exists"); return RuleResult.pass("Frozen coverage unavailable",details).withMetadata("",RuleCategory.COVERAGE);}
}
