package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.ChangeType;
import com.adtech.iaterap.backend.dto.FieldChangeResponse;
import com.adtech.iaterap.backend.dto.StepComparisonResponse;
import com.adtech.iaterap.backend.dto.TestStepResponse;
import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.entity.ProcedureHistory;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * AI-182 / T7-B1: Version Comparison engine.
 *
 * <p>Diffs two immutable {@link ProcedureHistory} revisions of the same
 * TestProcedure, Test Step by Test Step, field by field (Sec 19). This is
 * an application-layer, read-only comparison: it never mutates
 * ProcedureHistory, TestStep, or the current mutable procedure rows, and it
 * always compares the historical {@code stepsSnapshot} JSON rather than the
 * current mutable {@code test_steps} table, so a comparison remains valid
 * even after the procedure has since moved on to newer revisions.</p>
 */
@Service
public class VersionComparisonService {

    private final TestProcedureRepository testProcedureRepository;
    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final ObjectMapper objectMapper;

    public VersionComparisonService(
            TestProcedureRepository testProcedureRepository,
            ProcedureHistoryRepository procedureHistoryRepository,
            ObjectMapper objectMapper
    ) {
        this.testProcedureRepository = testProcedureRepository;
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * The Test Step fields that participate in comparison, matching the
     * engineering + reference fields carried by {@link TestStepResponse}.
     * Declared as an ordered list of (name, extractor) pairs so the same
     * definition drives both the diffing logic and the field ordering in
     * the response.
     */
    private static final List<FieldSpec> FIELD_SPECS = List.of(
            new FieldSpec("interfaceId", TestStepResponse::getInterfaceId),
            new FieldSpec("interfaceName", TestStepResponse::getInterfaceName),
            new FieldSpec("connectorId", TestStepResponse::getConnectorId),
            new FieldSpec("connectorName", TestStepResponse::getConnectorName),
            new FieldSpec("signalLibraryId", TestStepResponse::getSignalLibraryId),
            new FieldSpec("signalLibraryName", TestStepResponse::getSignalLibraryName),
            new FieldSpec("protocolTagId", TestStepResponse::getProtocolTagId),
            new FieldSpec("protocolTagName", TestStepResponse::getProtocolTagName),
            new FieldSpec("faultCategoryId", TestStepResponse::getFaultCategoryId),
            new FieldSpec("faultCategoryName", TestStepResponse::getFaultCategoryName),
            new FieldSpec("expectedBehaviour", TestStepResponse::getExpectedBehaviour),
            new FieldSpec("expectedRange", TestStepResponse::getExpectedRange),
            new FieldSpec("expectedUnit", TestStepResponse::getExpectedUnit),
            new FieldSpec("calibrationRequirement", TestStepResponse::getCalibrationRequirement),
            new FieldSpec("safetyNotes", TestStepResponse::getSafetyNotes),
            new FieldSpec("acceptanceCriteria", TestStepResponse::getAcceptanceCriteria),
            new FieldSpec("preconditions", TestStepResponse::getPreconditions),
            new FieldSpec("dependencies", TestStepResponse::getDependencies),
            new FieldSpec("executionOrder", TestStepResponse::getExecutionOrder),
            new FieldSpec("postConditions", TestStepResponse::getPostConditions),
            new FieldSpec("passFailCriteria", TestStepResponse::getPassFailCriteria),
            new FieldSpec("expectedObservation", TestStepResponse::getExpectedObservation),
            new FieldSpec("engineerRemarks", TestStepResponse::getEngineerRemarks),
            new FieldSpec("approvalStatus", TestStepResponse::getApprovalStatus),
            new FieldSpec("versionNumber", TestStepResponse::getVersionNumber)
    );

    public VersionComparisonResponse compare(Long procedureId, Integer fromVersion, Integer toVersion) {
        if (fromVersion == null || toVersion == null) {
            throw new IllegalArgumentException("Both fromVersion and toVersion are required.");
        }
        if (fromVersion < 1 || toVersion < 1) {
            throw new IllegalArgumentException("fromVersion and toVersion must be positive revision numbers.");
        }

        String procedureName = testProcedureRepository.findById(procedureId)
                .orElseThrow(() -> new NoSuchElementException("Test procedure not found: " + procedureId))
                .getName();

        ProcedureHistory fromRevision = requireRevision(procedureId, fromVersion);
        ProcedureHistory toRevision = requireRevision(procedureId, toVersion);

        List<TestStepResponse> fromSteps = deserializeSteps(fromRevision);
        List<TestStepResponse> toSteps = deserializeSteps(toRevision);

        Map<Long, TestStepResponse> fromById = indexById(fromSteps);
        Map<Long, TestStepResponse> toById = indexById(toSteps);

        // Preserve "from" ordering first, then any step ids newly introduced in "to",
        // so genuinely new steps still appear (at the end of the merge, before final sort).
        Set<Long> stepIds = new LinkedHashSet<>(fromById.keySet());
        stepIds.addAll(toById.keySet());

        List<StepComparisonResponse> stepComparisons = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int changed = 0;
        int unchanged = 0;

        for (Long stepId : stepIds) {
            TestStepResponse fromStep = fromById.get(stepId);
            TestStepResponse toStep = toById.get(stepId);

            if (fromStep == null) {
                stepComparisons.add(new StepComparisonResponse(ChangeType.ADDED, null, toStep, List.of()));
                added++;
            } else if (toStep == null) {
                stepComparisons.add(new StepComparisonResponse(ChangeType.REMOVED, fromStep, null, List.of()));
                removed++;
            } else {
                List<FieldChangeResponse> fieldChanges = diffFields(fromStep, toStep);
                ChangeType stepChangeType = fieldChanges.isEmpty() ? ChangeType.UNCHANGED : ChangeType.CHANGED;
                stepComparisons.add(new StepComparisonResponse(stepChangeType, fromStep, toStep, fieldChanges));
                if (stepChangeType == ChangeType.CHANGED) {
                    changed++;
                } else {
                    unchanged++;
                }
            }
        }

        stepComparisons.sort(Comparator.comparingInt(VersionComparisonService::effectiveExecutionOrder));

        VersionComparisonResponse response = new VersionComparisonResponse();
        response.setProcedureId(procedureId);
        response.setProcedureName(procedureName);
        response.setFromVersion(fromRevision.getVersionNumber());
        response.setToVersion(toRevision.getVersionNumber());
        response.setAddedStepCount(added);
        response.setRemovedStepCount(removed);
        response.setChangedStepCount(changed);
        response.setUnchangedStepCount(unchanged);
        response.setSteps(stepComparisons);
        return response;
    }

    // ---------- internals ----------

    private ProcedureHistory requireRevision(Long procedureId, Integer versionNumber) {
        return procedureHistoryRepository.findByTestProcedureIdAndVersionNumber(procedureId, versionNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Revision " + versionNumber + " not found for procedure " + procedureId));
    }

    /**
     * TestStepResponse is a read-only response DTO (getters only, no
     * setters), so it cannot be deserialized via standard Jackson bean
     * binding. The snapshot is parsed as a generic JSON tree instead, and
     * each element is rebuilt through {@link TestStepResponse#fromSnapshot}.
     */
    private List<TestStepResponse> deserializeSteps(ProcedureHistory revision) {
        try {
            JsonNode root = objectMapper.readTree(revision.getStepsSnapshot());
            List<TestStepResponse> steps = new ArrayList<>();
            for (JsonNode stepNode : root) {
                steps.add(TestStepResponse.fromSnapshot(stepNode));
            }
            return steps;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize TestStep snapshot for procedure " + revision.getTestProcedureId()
                            + " revision " + revision.getVersionNumber(), e);
        }
    }

    /**
     * Steps are matched by persisted TestStep id, the one identifier that
     * remains stable across revisions for an edited-in-place step
     * regardless of reordering. A snapshot entry with a null id (should not
     * occur for a persisted step, but guarded defensively) falls back to a
     * synthetic per-position key so it still participates as its own
     * add/remove rather than being silently dropped or colliding with
     * another entry.
     */
    private Map<Long, TestStepResponse> indexById(List<TestStepResponse> steps) {
        Map<Long, TestStepResponse> byId = new LinkedHashMap<>();
        long syntheticId = -1;
        for (TestStepResponse step : steps) {
            Long key = step.getId() != null ? step.getId() : syntheticId--;
            byId.put(key, step);
        }
        return byId;
    }

    private List<FieldChangeResponse> diffFields(TestStepResponse fromStep, TestStepResponse toStep) {
        List<FieldChangeResponse> changes = new ArrayList<>();
        for (FieldSpec spec : FIELD_SPECS) {
            Object oldRaw = spec.extractor().apply(fromStep);
            Object newRaw = spec.extractor().apply(toStep);
            String oldNormalized = normalize(oldRaw);
            String newNormalized = normalize(newRaw);

            if (Objects.equals(oldNormalized, newNormalized)) {
                continue;
            }

            ChangeType fieldChangeType;
            if (oldNormalized == null) {
                fieldChangeType = ChangeType.ADDED;
            } else if (newNormalized == null) {
                fieldChangeType = ChangeType.REMOVED;
            } else {
                fieldChangeType = ChangeType.CHANGED;
            }

            changes.add(new FieldChangeResponse(spec.name(), oldNormalized, newNormalized, fieldChangeType));
        }
        return changes;
    }

    /**
     * Blank strings are treated the same as null (an unset optional field
     * re-saved as "" is not a meaningful change), and every value is
     * rendered to its display string so the frontend can treat every
     * compared field uniformly.
     */
    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String asString = String.valueOf(value);
        return asString.isBlank() ? null : asString;
    }

    private static int effectiveExecutionOrder(StepComparisonResponse comparison) {
        TestStepResponse reference = comparison.getToStep() != null ? comparison.getToStep() : comparison.getFromStep();
        Integer order = reference != null ? reference.getExecutionOrder() : null;
        return order != null ? order : Integer.MAX_VALUE;
    }

    private record FieldSpec(String name, Function<TestStepResponse, Object> extractor) {
    }
}
