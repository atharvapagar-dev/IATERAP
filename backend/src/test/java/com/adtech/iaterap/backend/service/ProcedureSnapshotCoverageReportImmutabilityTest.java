package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.InvalidApprovalTransitionException;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the T11 submission artifacts retain their serialized values after live assets change. */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class ProcedureSnapshotCoverageReportImmutabilityTest {
    @Autowired private TestProcedureService procedures;
    @Autowired private ProcedureSnapshotService artifacts;
    @Autowired private ProcedureSnapshotRepository snapshots;
    @Autowired private CoverageReportRepository reports;
    @Autowired private TestStepRepository steps;
    @Autowired private TestProcedureRepository procedureRepository;
    @Autowired private LruRepository lrus;
    @Autowired private InterfaceRepository interfaces;
    @Autowired private ConnectorRepository connectors;
    @Autowired private SignalLibraryRepository signals;
    @Autowired private ProtocolTagRepository protocols;
    @Autowired private FaultTemplateRepository faults;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;

    private Long procedureId;
    private Interface iface; private Connector connector; private SignalLibrary signal;
    private String interfaceName; private String connectorName; private String signalName; private String signalDescription;

    @AfterEach
    void cleanUp() {
        if (procedureId != null) {
            jdbc.update("DELETE FROM coverage_report WHERE test_procedure_id = ?", procedureId);
            jdbc.update("DELETE FROM procedure_snapshot WHERE test_procedure_id = ?", procedureId);
            jdbc.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", procedureId);
            steps.deleteAll(steps.findByTestProcedureIdOrderByExecutionOrderAsc(procedureId));
            procedureRepository.deleteById(procedureId);
        }
        if (iface != null) { iface.setName(interfaceName); interfaces.save(iface); }
        if (connector != null) { connector.setName(connectorName); connectors.save(connector); }
        if (signal != null) { signal.setName(signalName); signal.setDescription(signalDescription); signals.save(signal); }
    }

    @Test
    void submissionArtifacts_areCreatedOnce_andRemainFrozenAfterSharedAssetEdits() throws Exception {
        Lru lru = lrus.findAll().get(0);
        iface = interfaces.findByLruId(lru.getId()).get(0);
        connector = connectors.findByLruId(lru.getId()).get(0);
        signal = signals.findByLruId(lru.getId()).get(0);
        ProtocolTag protocol = protocols.findByLruId(lru.getId()).get(0);
        FaultTemplate fault = faults.findByLruId(lru.getId()).get(0);
        interfaceName = iface.getName(); connectorName = connector.getName(); signalName = signal.getName(); signalDescription = signal.getDescription();

        TestProcedureResponse created = procedures.save(request(lru, protocol, fault));
        procedureId = created.getId();
        TestProcedureResponse submitted = procedures.submitForReview(procedureId);
        assertEquals("IN_REVIEW", submitted.getApprovalStatus());
        assertEquals(2, submitted.getVersionNumber());

        ProcedureSnapshot snapshot = artifacts.getSnapshot(procedureId, 2);
        CoverageReport report = artifacts.getReport(procedureId, 2);
        assertEquals(procedureId, snapshot.getTestProcedureId());
        assertEquals(2, snapshot.getProcedureVersionNumber());
        assertEquals(snapshot.getId(), report.getProcedureSnapshotId());
        assertEquals(procedureId, report.getTestProcedureId());
        assertEquals(2, report.getProcedureVersionNumber());
        assertEquals(1, report.getReportVersion());

        JsonNode payload = mapper.readTree(snapshot.getSnapshotPayload());
        JsonNode frozenStep = payload.path("procedure").path("steps").get(0);
        assertEquals("T11 frozen procedure", payload.path("procedure").path("name").asText());
        assertEquals(interfaceName, frozenStep.path("interface").path("name").asText());
        assertEquals(connectorName, frozenStep.path("connector").path("name").asText());
        assertEquals(signalName, frozenStep.path("signal").path("name").asText());
        assertEquals(signalDescription, frozenStep.path("signal").path("description").asText());
        String frozenSnapshot = snapshot.getSnapshotPayload();
        String frozenReport = report.getReportPayload();
        JsonNode originalReport = mapper.readTree(frozenReport);

        iface.setName(interfaceName + " changed"); interfaces.save(iface);
        connector.setName(connectorName + " changed"); connectors.save(connector);
        signal.setName(signalName + " changed"); signal.setDescription("changed after submission"); signals.save(signal);

        ProcedureSnapshot afterEdits = artifacts.getSnapshot(procedureId, 2);
        CoverageReport reportAfterEdits = artifacts.getReport(procedureId, 2);
        assertEquals(frozenSnapshot, afterEdits.getSnapshotPayload());
        assertEquals(frozenReport, reportAfterEdits.getReportPayload());
        assertEquals(originalReport.path("compositeScore"), mapper.readTree(reportAfterEdits.getReportPayload()).path("compositeScore"));
        assertEquals(1, snapshots.findAll().stream().filter(s -> procedureId.equals(s.getTestProcedureId())).count());
        assertEquals(1, reports.findAll().stream().filter(r -> procedureId.equals(r.getTestProcedureId())).count());
        assertThrows(InvalidApprovalTransitionException.class, () -> procedures.submitForReview(procedureId));
        assertEquals(1, snapshots.findAll().stream().filter(s -> procedureId.equals(s.getTestProcedureId())).count());
        assertEquals(1, reports.findAll().stream().filter(r -> procedureId.equals(r.getTestProcedureId())).count());
    }

    private TestProcedureRequest request(Lru lru, ProtocolTag protocol, FaultTemplate fault) {
        TestStepRequest step = new TestStepRequest();
        step.setInterfaceId(iface.getId()); step.setConnectorId(connector.getId()); step.setSignalLibraryId(signal.getId()); step.setProtocolTagId(protocol.getId()); step.setFaultCategoryId(fault.getId());
        step.setExpectedBehaviour("Frozen expected behaviour"); step.setAcceptanceCriteria("Frozen acceptance criteria"); step.setPassFailCriteria("Frozen pass/fail criteria"); step.setExecutionOrder(1);
        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId()); request.setName("T11 frozen procedure"); request.setDescription("submission artifact isolation"); request.setSteps(List.of(step));
        return request;
    }
}
