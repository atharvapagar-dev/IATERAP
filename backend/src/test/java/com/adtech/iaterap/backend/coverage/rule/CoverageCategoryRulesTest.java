package com.adtech.iaterap.backend.coverage.rule;

import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Permanent hand-calculated fixtures for the distinct defined-element formula. */
class CoverageCategoryRulesTest {
    private final Lru lru = lru(1L);
    @Test void interfacePartialCoverageCountsDuplicateReferencesOnce() {
        InterfaceRepository repo = mock(InterfaceRepository.class); Interface a = iface(10L), b = iface(11L), c = iface(12L);
        when(repo.findByLruId(1L)).thenReturn(List.of(a,b,c));
        RuleResult result = new InterfaceCoverageRule(repo).evaluate(new TestProcedure(), lru, List.of(step(a),step(a),step(b)));
        assertMeasured(result, 2, 3, "66.67");
        assertEquals(List.of(12L), result.getDetails().get("gapIds"));
    }
    @Test void signalFullyCoveredAndFullyUncoveredFixturesUseDefinedElements() {
        SignalLibraryRepository repo = mock(SignalLibraryRepository.class); SignalLibrary a = signal(20L), b = signal(21L);
        when(repo.findByLruId(1L)).thenReturn(List.of(a,b)); SignalCoverageRule rule = new SignalCoverageRule(repo);
        assertMeasured(rule.evaluate(new TestProcedure(), lru, List.of(signalStep(a),signalStep(b))), 2, 2, "100.00");
        assertEquals(List.of(), rule.evaluate(new TestProcedure(), lru, List.of(signalStep(a),signalStep(b))).getDetails().get("gapIds"));
        assertMeasured(rule.evaluate(new TestProcedure(), lru, List.of(new TestStep())), 0, 2, "0.00");
    }
    @Test void faultPartialCoverageUsesFaultTemplateReferences() {
        FaultTemplateRepository repo = mock(FaultTemplateRepository.class); FaultTemplate a=fault(30L), b=fault(31L), c=fault(32L), foreign=fault(99L);
        when(repo.findByLruId(1L)).thenReturn(List.of(a,b,c));
        assertMeasured(new FaultCoverageRule(repo).evaluate(new TestProcedure(), lru, List.of(faultStep(a),faultStep(a),faultStep(foreign))), 1, 3, "33.33");
    }
    @Test void fmeaCoverageDerivesAllMappedBranchesFromReferencedFaultTemplate() {
        FmeaBranchMappingRepository repo = mock(FmeaBranchMappingRepository.class); FaultTemplate covered=fault(40L), uncovered=fault(41L);
        when(repo.findByLruId(1L)).thenReturn(List.of(mapping(50L,covered),mapping(51L,covered),mapping(52L,uncovered)));
        assertMeasured(new FmeaCoverageRule(repo).evaluate(new TestProcedure(), lru, List.of(faultStep(covered),faultStep(covered))), 2, 3, "66.67");
    }
    @Test void zeroElementAndRequirementFunctionalAreExplicitlyUnavailable() {
        InterfaceRepository repo = mock(InterfaceRepository.class); when(repo.findByLruId(1L)).thenReturn(List.of());
        RuleResult zero = new InterfaceCoverageRule(repo).evaluate(new TestProcedure(),lru,List.of());
        assertNull(zero.getDetails().get("percentage")); assertEquals(0,zero.getDetails().get("totalElements"));
        for (RuleResult result : List.of(new RequirementCoverageRule().evaluate(new TestProcedure(),lru,List.of()),new FunctionalCoverageRule().evaluate(new TestProcedure(),lru,List.of()))) {
            assertFalse((Boolean) result.getDetails().get("measurable")); assertNull(result.getDetails().get("percentage"));
        }
    }
    private static void assertMeasured(RuleResult r,int referenced,int total,String percentage) { assertTrue(r.isPassed()); assertEquals(referenced,r.getDetails().get("referencedElements")); assertEquals(total,r.getDetails().get("totalElements")); assertEquals(new BigDecimal(percentage),r.getDetails().get("percentage")); }
    private static TestStep step(Interface v) { TestStep s=new TestStep();s.setInterfaceEntity(v);return s; }
    private static TestStep signalStep(SignalLibrary v) { TestStep s=new TestStep();s.setSignalLibrary(v);return s; }
    private static TestStep faultStep(FaultTemplate v) { TestStep s=new TestStep();s.setFaultCategory(v);return s; }
    private static Lru lru(Long id) { Lru v=new Lru("LRU");id(v,id);return v; }
    private static Interface iface(Long id) { Interface v=new Interface();id(v,id);return v; }
    private static SignalLibrary signal(Long id) { SignalLibrary v=new SignalLibrary();id(v,id);return v; }
    private static FaultTemplate fault(Long id) { FaultTemplate v=new FaultTemplate();id(v,id);return v; }
    private static FmeaBranchMapping mapping(Long id,FaultTemplate fault) { FmeaBranchMapping v=new FmeaBranchMapping();id(v,id);v.setFaultTemplate(fault);return v; }
    private static void id(Object v,Long id) { try { Field f=v.getClass().getDeclaredField("id");f.setAccessible(true);f.set(v,id); } catch (ReflectiveOperationException e) { throw new AssertionError(e); } }
}
