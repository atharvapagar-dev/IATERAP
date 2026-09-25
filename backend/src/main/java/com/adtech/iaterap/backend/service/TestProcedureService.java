package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.ApprovalState;
import com.adtech.iaterap.backend.approval.ApprovalStateMachine;
import com.adtech.iaterap.backend.approval.CoverageThresholdNotMetException;
import com.adtech.iaterap.backend.coverage.rule.RuleEngine;
import com.adtech.iaterap.backend.coverage.rule.RuleCategory;
import com.adtech.iaterap.backend.coverage.rule.RuleResult;
import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.dto.TestStepResponse;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Part L / Part O: Procedure Builder backend + Version Management.
 *
 * <p>Every {@link #save(TestProcedureRequest)} and
 * {@link #update(Long, TestProcedureRequest)} call is atomic with its
 * {@code procedure_history} write (PART AD): the procedure/steps update
 * and the revision insert happen in the same {@code @Transactional}
 * boundary, so a failure never leaves a partially-created revision.</p>
 *
 * <p>{@link #rollbackToRevision(Long, Integer)} (AI-187 / T7-B3) is a third
 * content-changing write alongside {@link #save} and {@link #update}: it
 * replays a historical {@link ProcedureHistory} snapshot back onto the live
 * procedure and, in the same transaction, writes yet another brand new
 * revision -- it never edits or removes any existing revision.</p>
 */
@Service
public class TestProcedureService {

    private static final Logger log = LoggerFactory.getLogger(TestProcedureService.class);

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
    private final AuditService auditService;
    private final RuleEngine ruleEngine;
    private ProcedureSnapshotService procedureSnapshotService;
    private ApprovalStateMachine approvalStateMachine;

    @org.springframework.beans.factory.annotation.Autowired
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
            ObjectMapper objectMapper,
            AuditService auditService,
            RuleEngine ruleEngine
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
        this.auditService = auditService;
        this.ruleEngine = ruleEngine;
        this.approvalStateMachine = new ApprovalStateMachine();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setProcedureSnapshotService(ProcedureSnapshotService procedureSnapshotService) {
        this.procedureSnapshotService = procedureSnapshotService;
    }

    /** Kept for existing callers and isolated tests without a rule engine. */
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
            ObjectMapper objectMapper,
            AuditService auditService
    ) {
        this(testProcedureRepository, testStepRepository, procedureHistoryRepository,
                lruRepository, interfaceRepository, connectorRepository,
                signalLibraryRepository, protocolTagRepository, faultTemplateRepository,
                objectMapper, auditService, null);
    }

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
            ObjectMapper objectMapper,
            ApprovalStateMachine approvalStateMachine
    ) {
        this(testProcedureRepository, testStepRepository, procedureHistoryRepository,
                lruRepository, interfaceRepository, connectorRepository,
                signalLibraryRepository, protocolTagRepository, faultTemplateRepository,
                objectMapper, null, null);
        this.approvalStateMachine = approvalStateMachine;
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

    // ---------- submit for review (AI-188 / AI-189) ----------

    @Transactional
    public TestProcedureResponse submitForReview(Long id) {
        TestProcedure procedure = requireProcedure(id);
        List<TestStep> steps = orderedSteps(procedure);
        validateStructuralWellFormedness(procedure, steps);

        ApprovalState currentState = ApprovalState.valueOf(procedure.getApprovalStatus());
        ApprovalState nextState = approvalStateMachine.transition(currentState, ApprovalState.IN_REVIEW);
        procedure.setApprovalStatus(nextState.name());
        procedure.setVersionNumber(procedure.getVersionNumber() + 1);
        procedure.setUpdatedAt(java.time.LocalDateTime.now());
        testProcedureRepository.save(procedure);
        writeRevision(procedure, procedure.getVersionNumber());
        if (procedureSnapshotService != null) {
            // Submission freezes reviewed content and its score atomically with the state transition.
            procedureSnapshotService.createSubmissionArtifacts(procedure, steps, resolveActor());
        }
        return TestProcedureResponse.fromEntity(procedure, steps);
    }

    // ---------- approval (AI-124 / AI-220) ----------

    /**
     * Approves an in-review procedure only when the coverage thresholds
     * persisted in its submission-time report are satisfied. The report is
     * deliberately not recalculated here: later shared-asset changes must not
     * alter the decision for the reviewed version.
     */
    @Transactional
    public TestProcedureResponse approve(Long id) {
        TestProcedure procedure = requireProcedure(id);
        ApprovalState currentState = ApprovalState.valueOf(procedure.getApprovalStatus());
        ApprovalState nextState = approvalStateMachine.transition(currentState, ApprovalState.APPROVED);

        assertFrozenCoverageThresholdsMet(procedure);

        procedure.setApprovalStatus(nextState.name());
        procedure.setUpdatedAt(java.time.LocalDateTime.now());
        testProcedureRepository.save(procedure);
        return TestProcedureResponse.fromEntity(procedure, orderedSteps(procedure));
    }

    private void assertFrozenCoverageThresholdsMet(TestProcedure procedure) {
        if (procedureSnapshotService == null) {
            throw new IllegalStateException("Frozen coverage reports are required before approving a procedure.");
        }

        CoverageReport report = procedureSnapshotService.getReport(
                procedure.getId(), procedure.getVersionNumber());
        try {
            JsonNode thresholds = objectMapper.readTree(report.getReportPayload()).path("thresholdResults");
            if (!thresholds.isObject()) {
                throw new IllegalStateException("Frozen coverage report has no threshold results.");
            }
            List<String> failedRules = new ArrayList<>();
            thresholds.fields().forEachRemaining(entry -> {
                // null is the CoverageEngine's established unavailable/not-applicable result.
                if (entry.getValue().isBoolean() && !entry.getValue().asBoolean()) {
                    failedRules.add(entry.getKey());
                }
            });
            if (!failedRules.isEmpty()) {
                throw new CoverageThresholdNotMetException(
                        procedure.getId(), procedure.getVersionNumber(), String.join(", ", failedRules));
            }
        } catch (CoverageThresholdNotMetException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read frozen coverage report for approval.", exception);
        }
    }

    private void validateStructuralWellFormedness(TestProcedure procedure, List<TestStep> steps) {
        if (steps.isEmpty()) {
            throw new IllegalStateException("Cannot submit a procedure with no Test Steps for review.");
        }
        if (ruleEngine != null) {
            for (RuleResult result : ruleEngine.evaluate(procedure, procedure.getLru(), steps).values()) {
                if (result.getCategory() == RuleCategory.STRUCTURAL && !result.isPassed()) {
                    throw new IllegalStateException("Structural validation failed for rule "
                            + result.getRuleId() + ": " + result.getExplanation());
                }
            }
        }
        for (TestStep step : steps) {
            boolean boundToSomething = step.getInterfaceEntity() != null
                    || step.getConnector() != null
                    || step.getSignalLibrary() != null
                    || step.getProtocolTag() != null
                    || step.getFaultCategory() != null;
            if (!boundToSomething) {
                throw new IllegalStateException(
                        "Test step id=" + step.getId() + " (execution order "
                                + step.getExecutionOrder() + ") is not bound to any Interface, "
                                + "Connector, Signal Library, Protocol Tag, or Fault Category. "
                                + "Every step must exercise something to be submitted for review."
                );
            }
        }
    }

    // ---------- review queue (AI-191 / T8-B1) ----------

    /**
     * Returns all TestProcedures currently awaiting review (approval_status = IN_REVIEW).
     * Uses the partial index on (approval_status) WHERE IN_REVIEW for efficient querying.
     * @return List of procedures in IN_REVIEW state with their test steps.
     */
    public List<TestProcedureResponse> getReviewQueue() {
        return testProcedureRepository.findByApprovalStatus(ApprovalState.IN_REVIEW.name()).stream()
                .map(p -> TestProcedureResponse.fromEntity(p, orderedSteps(p)))
                .toList();
    }

    // ---------- rollback (AI-187 / T7-B3) ----------

    /**
     * Rolls the procedure back to the recorded content of a previous
     * revision.
     *
     * <p>{@code sourceRevision} is treated strictly as read-only input --
     * this method never calls a setter on it and never saves it. The
     * source revision's name/description/LRU/Test Steps are replayed onto
     * the live procedure (the same replace-in-place mechanism {@link #update}
     * uses for every other content-changing save) and, in the same
     * {@code @Transactional} boundary, a brand new {@code procedure_history}
     * row is appended with the next version number. Every existing
     * revision -- including the one rolled back to -- is left completely
     * untouched; rollback only ever appends a new revision, matching
     * Sec 4 / Sec 6 of the T7-B3 rollback requirements.</p>
     *
     * <p>Unlike {@link #update}, this method deliberately does NOT run the
     * "no-op resave" suppression check: a rollback is an explicit,
     * intentional action targeting a specific historical revision and must
     * always produce a new, auditable revision (Sec 6 / Sec 15 "Version
     * numbering" requirement), even in the edge case where the target
     * revision's content happens to already match the procedure's current
     * live content.</p>
     *
     * <p>approvalStatus is intentionally left untouched, exactly as
     * {@link #update} already leaves it untouched on every other
     * content-changing save -- rollback does not invent a new approval
     * transition; the resulting revision carries forward whatever
     * approval state the live procedure already has and remains subject to
     * the existing approval workflow.</p>
     *
     * @param procedureId          the procedure to roll back
     * @param sourceVersionNumber  the revision number to roll back to
     * @return the live procedure state after rollback, whose
     *         {@code versionNumber} is the newly created revision
     */
    @Transactional
    public TestProcedureResponse rollbackToRevision(Long procedureId, Integer sourceVersionNumber) {
        if (sourceVersionNumber == null || sourceVersionNumber < 1) {
            throw new IllegalArgumentException("sourceVersionNumber must be a positive revision number.");
        }

        TestProcedure procedure = requireProcedure(procedureId);

        ProcedureHistory sourceRevision = procedureHistoryRepository
                .findByTestProcedureIdAndVersionNumber(procedureId, sourceVersionNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Revision " + sourceVersionNumber + " not found for procedure " + procedureId));

        Lru lru = lruRepository.findById(sourceRevision.getLruId())
                .orElseThrow(() -> new NoSuchElementException("LRU not found: " + sourceRevision.getLruId()));

        procedure.setLru(lru);
        procedure.setName(sourceRevision.getName());
        procedure.setDescription(sourceRevision.getDescription());
        procedure.setUpdatedAt(java.time.LocalDateTime.now());

        restoreSteps(procedure, deserializeSnapshotSteps(sourceRevision));
        testProcedureRepository.save(procedure);

        int newVersion = procedure.getVersionNumber() + 1;
        procedure.setVersionNumber(newVersion);
        testProcedureRepository.save(procedure);

        writeRevision(procedure, newVersion);

        TestProcedureResponse response = TestProcedureResponse.fromEntity(procedure, orderedSteps(procedure));

        recordRollbackAudit(procedure, sourceRevision, response);

        return response;
    }

    /**
     * Rebuilds the live TestStep rows from a historical snapshot.
     *
     * <p>Deliberately a separate method from {@link #replaceSteps}, not a
     * reuse of it, because rollback has different semantics for a snapshot
     * step id that no longer exists live: {@link #replaceSteps} treats a
     * missing id as a client error (an ordinary Procedure Builder edit
     * should never reference a step id that isn't currently live) and
     * throws; rollback must instead recreate that step, because the
     * snapshot is allowed to describe steps a later edit has since
     * deleted. Where a snapshot step's id IS still live, it is reused
     * in place (not recreated), so Version Comparison (AI-182 /
     * {@link VersionComparisonService}) continues to match that step by id
     * across revisions and correctly reports it as unchanged rather than
     * as a spurious removed/added pair.</p>
     */
    private void restoreSteps(TestProcedure procedure, List<TestStepResponse> snapshotSteps) {
        List<TestStep> existing = orderedSteps(procedure);
        Map<Long, TestStep> existingById = existing.stream()
                .collect(Collectors.toMap(TestStep::getId, Function.identity()));

        Set<Long> restoredIds = new HashSet<>();
        int order = 1;
        for (TestStepResponse snapshotStep : snapshotSteps) {
            TestStep step = (snapshotStep.getId() != null && existingById.containsKey(snapshotStep.getId()))
                    ? existingById.get(snapshotStep.getId())
                    : new TestStep();

            step.setTestProcedure(procedure);
            step.setInterfaceEntity(interfaceRepository.findById(snapshotStep.getInterfaceId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Interface not found: " + snapshotStep.getInterfaceId())));
            step.setConnector(connectorRepository.findById(snapshotStep.getConnectorId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Connector not found: " + snapshotStep.getConnectorId())));
            step.setSignalLibrary(signalLibraryRepository.findById(snapshotStep.getSignalLibraryId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Signal library not found: " + snapshotStep.getSignalLibraryId())));
            step.setProtocolTag(protocolTagRepository.findById(snapshotStep.getProtocolTagId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Protocol tag not found: " + snapshotStep.getProtocolTagId())));
            step.setFaultCategory(faultTemplateRepository.findById(snapshotStep.getFaultCategoryId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Fault category not found: " + snapshotStep.getFaultCategoryId())));

            step.setExpectedBehaviour(snapshotStep.getExpectedBehaviour());
            step.setExpectedRange(snapshotStep.getExpectedRange());
            if (snapshotStep.getExpectedUnit() != null) {
                step.setExpectedUnit(com.adtech.iaterap.backend.validation.EngineeringUnit
                        .valueOf(snapshotStep.getExpectedUnit()));
            }
            step.setCalibrationRequirement(snapshotStep.getCalibrationRequirement());
            step.setSafetyNotes(snapshotStep.getSafetyNotes());
            step.setAcceptanceCriteria(snapshotStep.getAcceptanceCriteria());
            step.setPreconditions(snapshotStep.getPreconditions());
            step.setDependencies(snapshotStep.getDependencies());
            step.setExecutionOrder(order++);
            step.setPostConditions(snapshotStep.getPostConditions());
            step.setPassFailCriteria(snapshotStep.getPassFailCriteria());
            step.setExpectedObservation(snapshotStep.getExpectedObservation());
            step.setEngineerRemarks(snapshotStep.getEngineerRemarks());
            step.setUpdatedAt(java.time.LocalDateTime.now());

            TestStep saved = testStepRepository.save(step);
            restoredIds.add(saved.getId());
        }

        for (TestStep step : existing) {
            if (!restoredIds.contains(step.getId())) {
                testStepRepository.delete(step);
            }
        }
    }

    /**
     * Mirrors {@link VersionComparisonService}'s own snapshot parsing: the
     * response DTO exposes no setters, so the snapshot is parsed as a
     * generic JSON tree and rebuilt via {@link TestStepResponse#fromSnapshot}
     * rather than through Jackson bean binding. Defensively re-sorted by
     * the snapshot's own recorded {@code executionOrder} so restoration
     * order is correct even if the stored array order were ever not
     * already ascending.
     */
    private List<TestStepResponse> deserializeSnapshotSteps(ProcedureHistory revision) {
        try {
            JsonNode root = objectMapper.readTree(revision.getStepsSnapshot());
            List<TestStepResponse> steps = new ArrayList<>();
            for (JsonNode stepNode : root) {
                steps.add(TestStepResponse.fromSnapshot(stepNode));
            }
            steps.sort(Comparator.comparing(
                    TestStepResponse::getExecutionOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return steps;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize TestStep snapshot for procedure " + revision.getTestProcedureId()
                            + " revision " + revision.getVersionNumber(), e);
        }
    }

    /**
     * Writes the ROLLBACK audit entry directly (rather than relying on
     * {@link com.adtech.iaterap.backend.aop.AuditAspect}'s generic
     * save/update/delete pointcut, which this method's name does not
     * match) so the audit action is recorded precisely as {@code ROLLBACK}
     * rather than a generic {@code UPDATE}, per Sec 7 of the rollback
     * requirements. Mirrors {@code AuditAspect}'s own failure handling:
     * an audit-write failure is logged and swallowed, never allowed to
     * fail or roll back an already-successful rollback.
     */
    private void recordRollbackAudit(
            TestProcedure procedure,
            ProcedureHistory sourceRevision,
            TestProcedureResponse result
    ) {
        try {
            auditService.record(
                    resolveActor(),
                    "ROLLBACK",
                    "TestProcedure",
                    String.valueOf(procedure.getId()),
                    safeSerialize(ProcedureHistoryResponse.fromEntity(sourceRevision)),
                    safeSerialize(result),
                    "Rolled back procedure " + procedure.getId() + " to revision "
                            + sourceRevision.getVersionNumber() + ", creating revision "
                            + result.getVersionNumber()
            );
        } catch (Exception auditException) {
            log.warn(
                    "Failed to persist ROLLBACK audit log for procedure {}: {}",
                    procedure.getId(), auditException.getMessage()
            );
        }
    }

    private String safeSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
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
            step.setExpectedUnit(sr.getExpectedUnit());
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
        history.setLruName(procedure.getLru().getName());
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
