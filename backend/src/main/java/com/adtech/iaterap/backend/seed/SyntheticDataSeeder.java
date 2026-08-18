package com.adtech.iaterap.backend.seed;

import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Synthetic data seeder - LRU / Interface / Connector / Signal Library /
 * Signal Range / Protocol Tag / Fault Template / FMEA Branch Mapping /
 * Project / Aircraft / Subsystem / Test Block.
 *
 * SYNTHETIC DATA NOTICE: every entity created here is fictitious and
 * generated for engineering demonstration purposes only, per Sec 3.2 of
 * the IATERAP proposal. None of it represents any real Adtech product,
 * customer programme, or classified system. Every seeded record's name
 * carries a "(Synthetic)" suffix (or, for join-style rows without their
 * own display name, a "SIM-"/"FLT-"/"FMEA-" prefixed code) so synthetic
 * data can never be mistaken for real production data in the UI, in
 * exports, or in the database itself. AI-155.
 *
 * Runs once via CommandLineRunner and is idempotent: run() always calls
 * reset() first, so repeated executions (container restarts, re-running
 * the seeder profile, CI, demo dry runs) reliably converge on the exact
 * same known-good synthetic dataset instead of accumulating duplicates.
 */
@Component
@Profile("synthetic-data")
public class SyntheticDataSeeder implements CommandLineRunner {

    private final LruRepository lruRepository;
    private final InterfaceRepository interfaceRepository;
    private final ConnectorRepository connectorRepository;
    private final SignalLibraryRepository signalLibraryRepository;
    private final SignalRangeRepository signalRangeRepository;
    private final ProtocolTagRepository protocolTagRepository;
    private final FaultTemplateRepository faultTemplateRepository;
    private final FmeaBranchMappingRepository fmeaBranchMappingRepository;
    private final ProjectRepository projectRepository;
    private final AircraftRepository aircraftRepository;
    private final SubsystemRepository subsystemRepository;
    private final TestBlockRepository testBlockRepository;
    private final TestStepRepository testStepRepository;
    private final TestProcedureRepository testProcedureRepository;
    private final ProcedureHistoryRepository procedureHistoryRepository;
    private final ReviewerCommentRepository reviewerCommentRepository;

    public SyntheticDataSeeder(
            LruRepository lruRepository,
            InterfaceRepository interfaceRepository,
            ConnectorRepository connectorRepository,
            SignalLibraryRepository signalLibraryRepository,
            SignalRangeRepository signalRangeRepository,
            ProtocolTagRepository protocolTagRepository,
            FaultTemplateRepository faultTemplateRepository,
            FmeaBranchMappingRepository fmeaBranchMappingRepository,
            ProjectRepository projectRepository,
            AircraftRepository aircraftRepository,
            SubsystemRepository subsystemRepository,
            TestBlockRepository testBlockRepository,
            TestStepRepository testStepRepository,
            TestProcedureRepository testProcedureRepository,
            ProcedureHistoryRepository procedureHistoryRepository,
            ReviewerCommentRepository reviewerCommentRepository
    ) {
        this.lruRepository = lruRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectorRepository = connectorRepository;
        this.signalLibraryRepository = signalLibraryRepository;
        this.signalRangeRepository = signalRangeRepository;
        this.protocolTagRepository = protocolTagRepository;
        this.faultTemplateRepository = faultTemplateRepository;
        this.fmeaBranchMappingRepository = fmeaBranchMappingRepository;
        this.projectRepository = projectRepository;
        this.aircraftRepository = aircraftRepository;
        this.subsystemRepository = subsystemRepository;
        this.testBlockRepository = testBlockRepository;
        this.testStepRepository = testStepRepository;
        this.testProcedureRepository = testProcedureRepository;
        this.procedureHistoryRepository = procedureHistoryRepository;
        this.reviewerCommentRepository = reviewerCommentRepository;
    }

    @Override
    public void run(String... args) {
        reset();

        seedLru("LRU-ENG-REG-01 (Synthetic Engine Regulator)",
                "Discrete I/O Interface DIO-7 (Synthetic)",
                "Analog Interface ANL-3 (Synthetic)",
                "CONN-DIO-7-A (Synthetic)",
                "ARINC-429-SIM (Synthetic)",
                "Open Circuit - DIO-7 (Synthetic)",
                "Loss of regulation signal at the engine regulator output stage.",
                "Discrete channel DIO-7 continuity check during BIT.",
                "Inspect DIO-7 harness and connector CONN-DIO-7-A for continuity.",
                310);

        seedLru("LRU-PWR-MON-02 (Synthetic Power Monitor)",
                "Discrete I/O Interface DIO-12 (Synthetic)",
                "Analog Interface ANL-5 (Synthetic)",
                "CONN-ANL-5-B (Synthetic)",
                "MIL-STD-1553-SIM (Synthetic)",
                "Signal Drift - ANL-5 (Synthetic)",
                "Gradual telemetry drift on the analog power-monitoring channel.",
                "Analog channel ANL-5 out-of-range comparison against nominal band.",
                "Recalibrate ANL-5 sensor chain and verify CONN-ANL-5-B seating.",
                180);

        seedLru("LRU-GSE-CTRL-03 (Synthetic GSE Controller)",
                "Discrete I/O Interface DIO-19 (Synthetic)",
                "Analog Interface ANL-9 (Synthetic)",
                "CONN-DIO-19-C (Synthetic)",
                "RS-422-SIM (Synthetic)",
                "Intermittent Fault - DIO-19 (Synthetic)",
                "Intermittent discrete signal loss on ground-support control line DIO-19.",
                "Repeated BIT failures correlated with vibration during ground handling.",
                "Torque-check CONN-DIO-19-C and inspect DIO-19 wiring for chafing.",
                420);

        seedProjectDomain();
        seedTestBlocks();
    }

    /**
     * Clears all previously seeded synthetic data (children first, to respect
     * FK constraints) so re-running this profile always produces a fresh,
     * known-good demo dataset - this is what makes the profile repeatable
     * and reset-on-demand, per AI-158, and keeps AI-155's seeder idempotent
     * across every table in the domain model it now covers.
     */
    private void reset() {
        // AI-179 fix: test_steps/test_procedures/procedure_history/
        // reviewer_comments were added in V10-V16, after this method was
        // originally written, and were never added here. Any real
        // TestStep/TestProcedure created through the running app (not
        // seeded) then permanently blocked every subsequent reset(),
        // since interface/connector/signal_libraries/protocol_tags/
        // fault_templates/test_procedures are all still FK-referenced by
        // it. Deleted first, in FK-dependency order, below.
        reviewerCommentRepository.deleteAllInBatch();
        procedureHistoryRepository.deleteAllInBatch();
        testStepRepository.deleteAllInBatch();
        testProcedureRepository.deleteAllInBatch();

        fmeaBranchMappingRepository.deleteAllInBatch();
        signalRangeRepository.deleteAllInBatch();
        signalLibraryRepository.deleteAllInBatch();
        protocolTagRepository.deleteAllInBatch();
        faultTemplateRepository.deleteAllInBatch();
        connectorRepository.deleteAllInBatch();
        interfaceRepository.deleteAllInBatch();
        lruRepository.deleteAllInBatch();

        subsystemRepository.deleteAllInBatch();
        aircraftRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();

        testBlockRepository.deleteAllInBatch();
    }

    private void seedLru(
            String lruName,
            String discreteInterfaceName,
            String analogInterfaceName,
            String connectorName,
            String protocolTagName,
            String faultTemplateName,
            String failureEffect,
            String detectionMethod,
            String recommendedAction,
            int riskPriorityNumber
    ) {
        Lru lru = lruRepository.save(new Lru(lruName));

        Interface discreteInterface = interfaceRepository.save(new Interface(discreteInterfaceName, lru));
        Interface analogInterface = interfaceRepository.save(new Interface(analogInterfaceName, lru));

        connectorRepository.saveAll(List.of(
                new Connector(connectorName, lru)
        ));

        protocolTagRepository.save(new ProtocolTag(
                protocolTagName,
                "SIM-" + lru.getId(),
                "Synthetic protocol identifier seeded for demonstration only.",
                lru
        ));

        FaultTemplate faultTemplate = faultTemplateRepository.save(new FaultTemplate(
                faultTemplateName,
                "FLT-" + lru.getId(),
                "Synthetic FMEA-derived fault scenario seeded for demonstration only.",
                "CRITICAL",
                lru
        ));

        // FMEA branch mapping (AI-155): links every synthetic fault template
        // back to its LRU with a fully FK-consistent, check-constraint-safe
        // (RPN 1-1000) failure-mode row, so the FMEA join table is no longer
        // left empty by the seeder.
        fmeaBranchMappingRepository.save(new FmeaBranchMapping(
                "FMEA-" + lru.getId() + "-" + faultTemplate.getId(),
                failureEffect,
                "Synthetic root-cause narrative seeded for demonstration only.",
                detectionMethod,
                recommendedAction,
                riskPriorityNumber,
                lru,
                faultTemplate
        ));

        SignalLibrary discreteSignal = signalLibraryRepository.save(new SignalLibrary(
                discreteInterfaceName.split(" \\(")[0] + " Signal",
                "Synthetic discrete signal bound to " + discreteInterfaceName,
                "V",
                discreteInterface,
                lru
        ));

        SignalLibrary analogSignal = signalLibraryRepository.save(new SignalLibrary(
                analogInterfaceName.split(" \\(")[0] + " Signal",
                "Synthetic analog signal bound to " + analogInterfaceName,
                "mA",
                analogInterface,
                lru
        ));

        signalRangeRepository.save(new SignalRange(
                "Nominal Range",
                BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(5.0),
                "NORMAL",
                discreteSignal
        ));

        signalRangeRepository.save(new SignalRange(
                "Nominal Range",
                BigDecimal.valueOf(4.0),
                BigDecimal.valueOf(20.0),
                "NORMAL",
                analogSignal
        ));
    }

    /**
     * Seeds the top-level Project / Aircraft / Subsystem tables.
     *
     * These three tables do not currently carry FK columns in the schema
     * (V1__initial_schema.sql defines them as independent lookup tables),
     * so "FK-consistent" here means each row is still clearly labelled
     * synthetic and safe to reset/reseed alongside every other table this
     * seeder owns, without colliding with the non-synthetic demo rows the
     * V1 baseline migration inserts (those use plain names with no
     * "(Synthetic)" suffix, so there is no name collision either way).
     */
    private void seedProjectDomain() {
        projectRepository.saveAll(List.of(
                new Project(
                        "IATERAP Synthetic Reliability Program (Synthetic)",
                        "Synthetic top-level program used to demo project/aircraft/subsystem CRUD."
                ),
                new Project(
                        "IATERAP Synthetic Flight Test Campaign (Synthetic)",
                        "Synthetic program grouping the flight-test aircraft and subsystems below."
                )
        ));

        aircraftRepository.saveAll(List.of(
                new Aircraft("SYN-AC-100 (Synthetic)", "Synthetic Aerospace Co."),
                new Aircraft("SYN-AC-200 (Synthetic)", "Synthetic Aerospace Co."),
                new Aircraft("SYN-AC-300 (Synthetic)", "Synthetic Dynamics Inc.")
        ));

        subsystemRepository.saveAll(List.of(
                new Subsystem("Synthetic Flight Control Subsystem"),
                new Subsystem("Synthetic Power Distribution Subsystem"),
                new Subsystem("Synthetic Ground Support Subsystem")
        ));
    }

    /**
     * Seeds the Test Block library (test_blocks has a unique constraint on
     * name, so every row uses a distinct synthetic name).
     */
    private void seedTestBlocks() {
        testBlockRepository.saveAll(List.of(
                new TestBlock(
                        "Synthetic Power-On BIT Block",
                        "Synthetic built-in-test block exercised at LRU power-on."
                ),
                new TestBlock(
                        "Synthetic Signal Range Verification Block",
                        "Synthetic test block that sweeps signal ranges against nominal bands."
                ),
                new TestBlock(
                        "Synthetic Fault Injection Block",
                        "Synthetic test block used to demonstrate fault-template detection flows."
                )
        ));
    }
}
