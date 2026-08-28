package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.approval.InvalidApprovalTransitionException;
import com.adtech.iaterap.backend.dto.ProcedureHistoryResponse;
import com.adtech.iaterap.backend.dto.TestProcedureRequest;
import com.adtech.iaterap.backend.dto.TestProcedureResponse;
import com.adtech.iaterap.backend.dto.TestStepRequest;
import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.entity.Connector;
import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.Interface;
import com.adtech.iaterap.backend.entity.Lru;
import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.repository.ConnectorRepository;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.InterfaceRepository;
import com.adtech.iaterap.backend.repository.LruRepository;
import com.adtech.iaterap.backend.repository.ProcedureHistoryRepository;
import com.adtech.iaterap.backend.repository.ProtocolTagRepository;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.TestProcedureRepository;
import com.adtech.iaterap.backend.repository.TestStepRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI-195 / T8-QA: end-to-end author -> version -> submit regression.
 *
 * <p>Unlike the focused unit tests, this drives the public service flow with
 * real repositories: a DRAFT is authored, edited into a new revision, compared
 * against its predecessor, submitted for review, and then protected from a
 * repeat submission.</p>
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class AuthorVersionSubmitRegressionTest {

    @Autowired private TestProcedureService testProcedureService;
    @Autowired private VersionComparisonService versionComparisonService;
    @Autowired private TestProcedureRepository testProcedureRepository;
    @Autowired private TestStepRepository testStepRepository;
    @Autowired private ProcedureHistoryRepository procedureHistoryRepository;
    @Autowired private LruRepository lruRepository;
    @Autowired private InterfaceRepository interfaceRepository;
    @Autowired private ConnectorRepository connectorRepository;
    @Autowired private SignalLibraryRepository signalLibraryRepository;
    @Autowired private ProtocolTagRepository protocolTagRepository;
    @Autowired private FaultTemplateRepository faultTemplateRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long createdProcedureId;

    @AfterEach
    void cleanUp() {
        if (createdProcedureId != null) {
            jdbcTemplate.update("DELETE FROM procedure_history WHERE test_procedure_id = ?", createdProcedureId);
            testStepRepository.deleteAll(testStepRepository
                    .findByTestProcedureIdOrderByExecutionOrderAsc(createdProcedureId));
            testProcedureRepository.deleteById(createdProcedureId);
            createdProcedureId = null;
        }
    }

    @Test
    void authorVersionCompareAndSubmitRemainOneRevisionChain() {
        TestProcedureResponse created = testProcedureService.save(
                procedureRequest("AI-195 author version submit", "expected v1", null));
        createdProcedureId = created.getId();

        assertEquals("DRAFT", created.getApprovalStatus());
        assertEquals(1, created.getVersionNumber());
        Long stepId = created.getSteps().get(0).getId();

        TestProcedureResponse revised = testProcedureService.update(created.getId(),
                procedureRequest("AI-195 author version submit", "expected v2", stepId));

        assertEquals("DRAFT", revised.getApprovalStatus());
        assertEquals(2, revised.getVersionNumber());

        VersionComparisonResponse comparison = versionComparisonService.compare(
                created.getId(), 1, 2);
        assertEquals(1, comparison.getChangedStepCount());
        assertEquals("expected v1", comparison.getSteps().get(0)
                .getFieldChanges().get(0).getOldValue());
        assertEquals("expected v2", comparison.getSteps().get(0)
                .getFieldChanges().get(0).getNewValue());

        TestProcedureResponse submitted = testProcedureService.submitForReview(created.getId());
        assertEquals("IN_REVIEW", submitted.getApprovalStatus());
        assertEquals(3, submitted.getVersionNumber());

        List<ProcedureHistoryResponse> history = testProcedureService.getHistory(created.getId());
        assertEquals(3, history.size());
        assertEquals(List.of(1, 2, 3), history.stream()
                .map(ProcedureHistoryResponse::getVersionNumber).toList());
        assertEquals("IN_REVIEW", history.get(2).getApprovalStatus());

        assertThrows(InvalidApprovalTransitionException.class,
                () -> testProcedureService.submitForReview(created.getId()));
        assertTrue(testProcedureRepository.findById(created.getId()).isPresent());
    }

    private TestProcedureRequest procedureRequest(String name, String behaviour, Long stepId) {
        Lru lru = lruRepository.findAll().get(0);
        Interface iface = interfaceRepository.findAll().stream()
                .filter(candidate -> candidate.getLru().getId().equals(lru.getId()))
                .findFirst().orElseThrow();
        Connector connector = connectorRepository.findByLruId(lru.getId()).get(0);
        SignalLibrary signal = signalLibraryRepository.findByLruId(lru.getId()).get(0);
        ProtocolTag protocol = protocolTagRepository.findByLruId(lru.getId()).get(0);
        FaultTemplate fault = faultTemplateRepository.findByLruId(lru.getId()).get(0);

        TestStepRequest step = new TestStepRequest();
        step.setId(stepId);
        step.setInterfaceId(iface.getId());
        step.setConnectorId(connector.getId());
        step.setSignalLibraryId(signal.getId());
        step.setProtocolTagId(protocol.getId());
        step.setFaultCategoryId(fault.getId());
        step.setExpectedBehaviour(behaviour);
        step.setAcceptanceCriteria("Signal is within the expected range");
        step.setPassFailCriteria("Pass when the signal is nominal");
        step.setExecutionOrder(1);

        TestProcedureRequest request = new TestProcedureRequest();
        request.setLruId(lru.getId());
        request.setName(name);
        request.setDescription("AI-195 full author-to-submit regression fixture");
        request.setSteps(List.of(step));
        return request;
    }
}
