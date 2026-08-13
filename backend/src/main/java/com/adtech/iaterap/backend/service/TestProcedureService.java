package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Part L / Part O: Procedure Builder backend + Version Management.
 *
 * <p>Every {@link #save(TestProcedureRequest)} and
 * {@link #update(Long, TestProcedureRequest)} call is atomic with its
 * {@code procedure_history} write (PART AD): the procedure/steps update
 * and the revision insert happen in the same {@code @Transactional}
 * boundary, so a failure never leaves a partially-created revision.</p>
 */
@Service
public class TestProcedureService {

    private final TestProcedureRepository testProcedureRepository;
    private final TestStepRepository testStepRepository;
    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final LruRepository lruRepository;
    private final InterfaceRepository interfaceRepository;
    private final ConnectorRepository connectorRepository;
    private final SignalLibraryRepository signalLibraryRepository;
    private final ProtocolTagRepository protocolTagRepository;
    private final FaultTemplateRepository faultTemplateRepository;
    private final ObjectMapper objectMapper;

    public TestProcedureService(
            TestProcedureRepository testProcedureRepository,
            TestStepRepository testStepRepository,
            ProcedureHistoryRepository procedureHistoryRepository,
            LruRepository lruRepository,
            InterfaceRepository interfaceRepository,
            ConnectorRepository connectorRepository,
            SignalLibraryRepository signalLibraryRepository,
            ProtocolTagRepository protocolTagRepository,
            FaultTemplateRepository faultTemplateRepository,
            ObjectMapper objectMapper
    ) {
        this.testProcedureRepository = testProcedureRepository;
        this.testStepRepository = testStepRepository;
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.lruRepository = lruRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectorRepository = connectorRepository;
        this.signalLibraryRepository = signalLibraryRepository;
        this.protocolTagRepository = protocolTagRepository;
        this.faultTemplateRepository = faultTemplateRepository;
        this.objectMapper = objectMapper;
    }

    // ---------- reads ----------

    public TestProcedureResponse getById(Long id) {
        TestProcedure procedure = requireProcedure(id);
        return TestProcedureResponse.fromEntity(procedure, orderedSteps(procedure));
    }

    public List<TestProcedureResponse> getAll() {
        return testProcedureRepository.findAll().stream()
                .map(p -> TestProcedureResponse.fromEntity(p, orderedSteps(p)))
                .toList();
    }

    // ---------- writes (content-changing saves) ----------

    /** Creates a new TestProcedure with its initial TestSteps and revision 1. */
    @Transactional
    public TestProcedureResponse save(TestProcedureRequest request) {
        Lru lru = lruRepository.findById(request.getLruId())
                .orElseThrow(() -> new NoSuchElementException("LRU not found: " + request.getLruId()));

        TestProcedure procedure = new TestProcedure();
        procedure.setLru(lru);
        procedure.setName(request.getName());
        procedure.setDescription(request.getDescription());
        procedure.setVersionNumber(1);
        procedure = testProcedureRepository.save(procedure);

        replaceSteps(procedure, request.getSteps());

        writeRevision(procedure, 1);

        return TestProcedureResponse.fromEntity(procedure, orderedSteps(procedure));
    }

    /**
     * Applies add/edit/reorder/remove TestStep changes and procedure field
     * changes, and -- if anything actually changed (PART Q) -- writes a new
     * immutable revision in the same transaction (PART R / PART AD).
     */
    @Transactional
    public TestProcedureResponse update(Long id, TestProcedureRequest request) {
        TestProcedure procedure = requireProcedure(id);

        Lru lru = lruRepository.findById(request.getLruId())
                .orElseThrow(() -> new NoSuchElementException("LRU not found: " + request.getLruId()));

        procedure.setLru(lru);
        procedure.setName(request.getName());
        procedure.setDescription(request.getDescription());
        procedure.setUpdatedAt(java.time.LocalDateTime.now());

        replaceSteps(procedure, request.getSteps());
        testProcedureRepository.save(procedure);

        int nextVersion = procedure.getVersionNumber() + 1;
        String candidateSnapshot = serializeSteps(orderedSteps(procedure));

        boolean isNoOpResave = procedureHistoryRepository
                .findTopByTestProcedureIdOrderByVersionNumberDesc(procedure.getId())
                .map(latest -> sameContent(latest, procedure, candidateSnapshot))
                .orElse(false);

        if (!isNoOpResave) {
            procedure.setVersionNumber(nextVersion);
            testProcedureRepository.save(procedure);
            writeRevision(procedure, nextVersion);
        }

        return TestProcedureResponse.fromEntity(procedure, orderedSteps(procedure));
    }

    // ---------- revision retrieval ----------

    public List<com.adtech.iaterap.backend.dto.ProcedureHistoryResponse> getHistory(Long procedureId) {
        requireProcedure(procedureId); // 404 if the procedure itself doesn't exist
        return procedureHistoryRepository.findByTestProcedureIdOrderByVersionNumberAsc(procedureId).stream()
                .map(com.adtech.iaterap.backend.dto.ProcedureHistoryResponse::fromEntity)
                .toList();
    }

    public com.adtech.iaterap.backend.dto.ProcedureHistoryResponse getRevision(Long procedureId, Integer versionNumber) {
        ProcedureHistory history = procedureHistoryRepository
                .findByTestProcedureIdAndVersionNumber(procedureId, versionNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Revision " + versionNumber + " not found for procedure " + procedureId));
        return com.adtech.iaterap.backend.dto.ProcedureHistoryResponse.fromEntity(history);
    }

    // ---------- internals ----------

    private TestProcedure requireProcedure(Long id) {
        return testProcedureRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Test procedure not found: " + id));
    }

    private List<TestStep> orderedSteps(TestProcedure procedure) {
        return testStepRepository.findByTestProcedureIdOrderByExecutionOrderAsc(procedure.getId());
    }

    /**
     * Add / edit / reorder / remove (PART N, PART Q): the incoming request
     * carries the complete desired ordered step list. Steps present in the
     * request with an id are updated in place; steps without an id are
     * created; existing steps not present in the request are removed. The
     * existing {@code executionOrder} column (PART N) is reused, not
     * duplicated, driven directly by each step's position in the payload.
     */
    private void replaceSteps(TestProcedure procedure, List<TestStepRequest> stepRequests) {
        List<TestStep> existing = orderedSteps(procedure);
        Set<Long> keepIds = stepRequests.stream()
                .map(TestStepRequest::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        for (TestStep step : existing) {
            if (!keepIds.contains(step.getId())) {
                testStepRepository.delete(step);
            }
        }

        int order = 1;
        for (TestStepRequest sr : stepRequests) {
            TestStep step = (sr.getId() != null)
                    ? testStepRepository.findById(sr.getId())
                        .orElseThrow(() -> new NoSuchElementException("Test step not found: " + sr.getId()))
                    : new TestStep();

            step.setTestProcedure(procedure);
            step.setInterfaceEntity(interfaceRepository.findById(sr.getInterfaceId())
                    .orElseThrow(() -> new NoSuchElementException("Interface not found: " + sr.getInterfaceId())));
            step.setConnector(connectorRepository.findById(sr.getConnectorId())
                    .orElseThrow(() -> new NoSuchElementException("Connector not found: " + sr.getConnectorId())));
            step.setSignalLibrary(signalLibraryRepository.findById(sr.getSignalLibraryId())
                    .orElseThrow(() -> new NoSuchElementException("Signal library not found: " + sr.getSignalLibraryId())));
            step.setProtocolTag(protocolTagRepository.findById(sr.getProtocolTagId())
                    .orElseThrow(() -> new NoSuchElementException("Protocol tag not found: " + sr.getProtocolTagId())));
            step.setFaultCategory(faultTemplateRepository.findById(sr.getFaultCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Fault category not found: " + sr.getFaultCategoryId())));

            step.setExpectedBehaviour(sr.getExpectedBehaviour());
            step.setExpectedRange(sr.getExpectedRange());
            step.setCalibrationRequirement(sr.getCalibrationRequirement());
            step.setSafetyNotes(sr.getSafetyNotes());
            step.setAcceptanceCriteria(sr.getAcceptanceCriteria());
            step.setPreconditions(sr.getPreconditions());
            step.setDependencies(sr.getDependencies());
            step.setExecutionOrder(order++);
            step.setPostConditions(sr.getPostConditions());
            step.setPassFailCriteria(sr.getPassFailCriteria());
            step.setExpectedObservation(sr.getExpectedObservation());
            step.setEngineerRemarks(sr.getEngineerRemarks());
            step.setUpdatedAt(java.time.LocalDateTime.now());

            testStepRepository.save(step);
        }
    }

    private void writeRevision(TestProcedure procedure, int versionNumber) {
        ProcedureHistory history = new ProcedureHistory();
        history.setTestProcedureId(procedure.getId());
        history.setVersionNumber(versionNumber);
        history.setName(procedure.getName());
        history.setDescription(procedure.getDescription());
        history.setLruId(procedure.getLru().getId());
        history.setApprovalStatus(procedure.getApprovalStatus());
        history.setStepsSnapshot(serializeSteps(orderedSteps(procedure)));
        history.setCreatedBy(resolveActor());
        procedureHistoryRepository.save(history);
    }

    private String serializeSteps(List<TestStep> steps) {
        try {
            List<com.adtech.iaterap.backend.dto.TestStepResponse> dtos =
                    steps.stream().map(com.adtech.iaterap.backend.dto.TestStepResponse::fromEntity).toList();
            return objectMapper.writeValueAsString(dtos);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize TestStep snapshot", e);
        }
    }

    private boolean sameContent(ProcedureHistory latest, TestProcedure procedure, String candidateStepsSnapshot) {
        return java.util.Objects.equals(latest.getName(), procedure.getName())
                && java.util.Objects.equals(latest.getDescription(), procedure.getDescription())
                && java.util.Objects.equals(latest.getLruId(), procedure.getLru().getId())
                && java.util.Objects.equals(latest.getApprovalStatus(), procedure.getApprovalStatus())
                && java.util.Objects.equals(latest.getStepsSnapshot(), candidateStepsSnapshot);
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
