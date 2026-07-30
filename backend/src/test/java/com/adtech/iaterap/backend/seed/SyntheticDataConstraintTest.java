package com.adtech.iaterap.backend.seed;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.entity.ProtocolTag;
import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.entity.SignalRange;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-154 [T4-A3] - FK/check-constraint validation tests.
 *
 * Validates that the data produced by SyntheticDataSeeder (and any other
 * data present in the local dev database) actually satisfies every FK
 * and CHECK constraint defined in the schema for the tables the seeder
 * writes to. Runs against the real configured datasource (no H2/Testcontainers
 * in this project), same approach as BackendApplicationTests.
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class SyntheticDataConstraintTest {

    private static final Set<String> VALID_FAULT_TEMPLATE_SEVERITIES =
            Set.of("NORMAL", "WARNING", "CRITICAL", "CATASTROPHIC");

    @Autowired
    private LruRepository lruRepository;

    @Autowired
    private InterfaceRepository interfaceRepository;

    @Autowired
    private ProtocolTagRepository protocolTagRepository;

    @Autowired
    private FaultTemplateRepository faultTemplateRepository;

    @Autowired
    private SignalLibraryRepository signalLibraryRepository;

    @Autowired
    private SignalRangeRepository signalRangeRepository;

    @Test
    void shouldHaveSeededAtLeastOneLru() {
        assertTrue(lruRepository.count() > 0, "Expected at least one LRU to exist (seeder should have run)");
    }

    @Test
    void everyProtocolTagShouldReferenceAnExistingLru() {
        List<ProtocolTag> tags = protocolTagRepository.findAll();
        assertFalse(tags.isEmpty(), "Expected at least one ProtocolTag to exist");

        for (ProtocolTag tag : tags) {
            assertNotNull(tag.getLru(), "ProtocolTag " + tag.getId() + " has a null Lru reference");
            assertTrue(lruRepository.existsById(tag.getLru().getId()),
                    "ProtocolTag " + tag.getId() + " references a non-existent Lru id " + tag.getLru().getId());
        }
    }

    @Test
    void everyFaultTemplateShouldReferenceAnExistingLru() {
        List<FaultTemplate> templates = faultTemplateRepository.findAll();
        assertFalse(templates.isEmpty(), "Expected at least one FaultTemplate to exist");

        for (FaultTemplate template : templates) {
            assertNotNull(template.getLru(), "FaultTemplate " + template.getId() + " has a null Lru reference");
            assertTrue(lruRepository.existsById(template.getLru().getId()),
                    "FaultTemplate " + template.getId() + " references a non-existent Lru id " + template.getLru().getId());
        }
    }

    @Test
    void everyFaultTemplateSeverityShouldSatisfyCheckConstraint() {
        List<FaultTemplate> templates = faultTemplateRepository.findAll();
        assertFalse(templates.isEmpty(), "Expected at least one FaultTemplate to exist");

        for (FaultTemplate template : templates) {
            assertTrue(VALID_FAULT_TEMPLATE_SEVERITIES.contains(template.getSeverity()),
                    "FaultTemplate " + template.getId() + " has severity '" + template.getSeverity()
                            + "' which violates chk_fault_template_severity");
        }
    }

    @Test
    void everySignalLibraryShouldReferenceAnExistingInterfaceAndLru() {
        List<SignalLibrary> libraries = signalLibraryRepository.findAll();
        assertFalse(libraries.isEmpty(), "Expected at least one SignalLibrary to exist");

        for (SignalLibrary library : libraries) {
            assertNotNull(library.getInterfaceEntity(), "SignalLibrary " + library.getId() + " has a null Interface reference");
            assertTrue(interfaceRepository.existsById(library.getInterfaceEntity().getId()),
                    "SignalLibrary " + library.getId() + " references a non-existent Interface id");

            assertNotNull(library.getLru(), "SignalLibrary " + library.getId() + " has a null Lru reference");
            assertTrue(lruRepository.existsById(library.getLru().getId()),
                    "SignalLibrary " + library.getId() + " references a non-existent Lru id");
        }
    }

    @Test
    void everySignalRangeShouldReferenceAnExistingSignalLibrary() {
        List<SignalRange> ranges = signalRangeRepository.findAll();
        assertFalse(ranges.isEmpty(), "Expected at least one SignalRange to exist");

        for (SignalRange range : ranges) {
            assertNotNull(range.getSignalLibrary(), "SignalRange " + range.getId() + " has a null SignalLibrary reference");
            assertTrue(signalLibraryRepository.existsById(range.getSignalLibrary().getId()),
                    "SignalRange " + range.getId() + " references a non-existent SignalLibrary id");
        }
    }
}