package com.adtech.iaterap.backend.seed;

import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Synthetic data seeder — LRU / Interface / Connector / Signal Library /
 * Signal Range / Protocol Tag / Fault Template.
 *
 * SYNTHETIC DATA NOTICE: every entity created here is fictitious and
 * generated for engineering demonstration purposes only, per Sec 3.2 of
 * the IATERAP proposal. None of it represents any real Adtech product,
 * customer programme, or classified system.
 *
 * Runs once via CommandLineRunner and is idempotent: it only seeds when
 * the LRU table is empty, so it is safe to leave enabled across restarts.
 */
@Component
public class SyntheticDataSeeder implements CommandLineRunner {

    private final LruRepository lruRepository;
    private final InterfaceRepository interfaceRepository;
    private final ConnectorRepository connectorRepository;
    private final SignalLibraryRepository signalLibraryRepository;
    private final SignalRangeRepository signalRangeRepository;
    private final ProtocolTagRepository protocolTagRepository;
    private final FaultTemplateRepository faultTemplateRepository;

    public SyntheticDataSeeder(
            LruRepository lruRepository,
            InterfaceRepository interfaceRepository,
            ConnectorRepository connectorRepository,
            SignalLibraryRepository signalLibraryRepository,
            SignalRangeRepository signalRangeRepository,
            ProtocolTagRepository protocolTagRepository,
            FaultTemplateRepository faultTemplateRepository
    ) {
        this.lruRepository = lruRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectorRepository = connectorRepository;
        this.signalLibraryRepository = signalLibraryRepository;
        this.signalRangeRepository = signalRangeRepository;
        this.protocolTagRepository = protocolTagRepository;
        this.faultTemplateRepository = faultTemplateRepository;
    }

    @Override
    public void run(String... args) {
        if (lruRepository.count() > 0) {
            return; // already seeded — idempotent no-op
        }

        seedLru("LRU-ENG-REG-01 (Synthetic Engine Regulator)",
                "Discrete I/O Interface DIO-7 (Synthetic)",
                "Analog Interface ANL-3 (Synthetic)",
                "CONN-DIO-7-A (Synthetic)",
                "ARINC-429-SIM (Synthetic)",
                "Open Circuit - DIO-7 (Synthetic)");

        seedLru("LRU-PWR-MON-02 (Synthetic Power Monitor)",
                "Discrete I/O Interface DIO-12 (Synthetic)",
                "Analog Interface ANL-5 (Synthetic)",
                "CONN-ANL-5-B (Synthetic)",
                "MIL-STD-1553-SIM (Synthetic)",
                "Signal Drift - ANL-5 (Synthetic)");

        seedLru("LRU-GSE-CTRL-03 (Synthetic GSE Controller)",
                "Discrete I/O Interface DIO-19 (Synthetic)",
                "Analog Interface ANL-9 (Synthetic)",
                "CONN-DIO-19-C (Synthetic)",
                "RS-422-SIM (Synthetic)",
                "Intermittent Fault - DIO-19 (Synthetic)");
    }

    private void seedLru(
            String lruName,
            String discreteInterfaceName,
            String analogInterfaceName,
            String connectorName,
            String protocolTagName,
            String faultTemplateName
    ) {
        Lru lru = lruRepository.save(new Lru(lruName));

        Interface discreteInterface = interfaceRepository.save(new Interface(discreteInterfaceName));
        Interface analogInterface = interfaceRepository.save(new Interface(analogInterfaceName));

        connectorRepository.saveAll(List.of(
                new Connector(connectorName)
        ));

        protocolTagRepository.save(new ProtocolTag(
                protocolTagName,
                "SIM-" + lru.getId(),
                "Synthetic protocol identifier seeded for demonstration only.",
                lru
        ));

        faultTemplateRepository.save(new FaultTemplate(
                faultTemplateName,
                "FLT-" + lru.getId(),
                "Synthetic FMEA-derived fault scenario seeded for demonstration only.",
                "CRITICAL",
                lru
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
}