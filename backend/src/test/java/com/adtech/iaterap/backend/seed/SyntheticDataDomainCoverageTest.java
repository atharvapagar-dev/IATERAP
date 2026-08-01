package com.adtech.iaterap.backend.seed;

import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI-155 - verifies the synthetic-data seeder now covers the full domain
 * model (not just the LRU/interface/signal subtree from AI-154): Project,
 * Aircraft, Subsystem, FmeaBranchMapping and TestBlock all get realistic,
 * clearly-labelled synthetic rows, and every FmeaBranchMapping row is
 * FK-consistent and satisfies the risk_priority_number check constraint.
 * Runs against the real configured datasource, same approach as
 * SyntheticDataConstraintTest and BackendApplicationTests.
 */
@SpringBootTest
@ActiveProfiles("synthetic-data")
class SyntheticDataDomainCoverageTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private SubsystemRepository subsystemRepository;

    @Autowired
    private TestBlockRepository testBlockRepository;

    @Autowired
    private FmeaBranchMappingRepository fmeaBranchMappingRepository;

    @Autowired
    private LruRepository lruRepository;

    @Autowired
    private FaultTemplateRepository faultTemplateRepository;

    @Test
    void shouldSeedProjectsLabelledAsSynthetic() {
        List<com.adtech.iaterap.backend.entity.Project> projects = projectRepository.findAll();
        assertFalse(projects.isEmpty(), "Expected at least one Project to be seeded");
        projects.forEach(project ->
                assertTrue(project.getName().contains("(Synthetic)"),
                        "Project '" + project.getName() + "' is not labelled as synthetic"));
    }

    @Test
    void shouldSeedAircraftLabelledAsSynthetic() {
        List<com.adtech.iaterap.backend.entity.Aircraft> aircraft = aircraftRepository.findAll();
        assertFalse(aircraft.isEmpty(), "Expected at least one Aircraft to be seeded");
        aircraft.forEach(a ->
                assertTrue(a.getName().contains("(Synthetic)"),
                        "Aircraft '" + a.getName() + "' is not labelled as synthetic"));
    }

    @Test
    void shouldSeedSubsystemsLabelledAsSynthetic() {
        List<com.adtech.iaterap.backend.entity.Subsystem> subsystems = subsystemRepository.findAll();
        assertFalse(subsystems.isEmpty(), "Expected at least one Subsystem to be seeded");
        subsystems.forEach(subsystem ->
                assertTrue(subsystem.getName().toLowerCase().contains("synthetic"),
                        "Subsystem '" + subsystem.getName() + "' is not labelled as synthetic"));
    }

    @Test
    void shouldSeedTestBlocksWithUniqueNames() {
        List<com.adtech.iaterap.backend.entity.TestBlock> testBlocks = testBlockRepository.findAll();
        assertFalse(testBlocks.isEmpty(), "Expected at least one TestBlock to be seeded");

        long distinctNames = testBlocks.stream()
                .map(com.adtech.iaterap.backend.entity.TestBlock::getName)
                .distinct()
                .count();
        assertEquals(testBlocks.size(), distinctNames, "TestBlock names must be unique");
    }

    @Test
    void everyFmeaBranchMappingShouldBeFkConsistentAndWithinRpnBounds() {
        List<FmeaBranchMapping> mappings = fmeaBranchMappingRepository.findAll();
        assertFalse(mappings.isEmpty(), "Expected at least one FmeaBranchMapping to be seeded");

        for (FmeaBranchMapping mapping : mappings) {
            assertNotNull(mapping.getLru());
            assertTrue(lruRepository.existsById(mapping.getLru().getId()),
                    "FmeaBranchMapping " + mapping.getId() + " references a non-existent Lru id");

            assertNotNull(mapping.getFaultTemplate());
            assertTrue(faultTemplateRepository.existsById(mapping.getFaultTemplate().getId()),
                    "FmeaBranchMapping " + mapping.getId() + " references a non-existent FaultTemplate id");

            assertNotNull(mapping.getRiskPriorityNumber());
            assertTrue(mapping.getRiskPriorityNumber() >= 1 && mapping.getRiskPriorityNumber() <= 1000,
                    "FmeaBranchMapping " + mapping.getId() + " has an out-of-range riskPriorityNumber");
        }
    }
}
