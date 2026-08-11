package com.adtech.iaterap.backend.approval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI-166 / T5-B2: transition unit tests.
 * AI-168 / T5-QA: complete illegal-transition coverage (all 25 combinations
 * in the 5x5 state matrix are explicitly classified and asserted below).
 */
class ApprovalStateMachineTest {

    private final ApprovalStateMachine machine = new ApprovalStateMachine();

    /**
     * The exhaustive set of legal transitions per the AI-164 specification.
     * Everything in ApprovalState x ApprovalState NOT in this set is illegal.
     */
    private static final Set<Transition> LEGAL = Set.of(
            new Transition(ApprovalState.DRAFT, ApprovalState.IN_REVIEW),
            new Transition(ApprovalState.IN_REVIEW, ApprovalState.APPROVED),
            new Transition(ApprovalState.IN_REVIEW, ApprovalState.REJECTED),
            new Transition(ApprovalState.APPROVED, ApprovalState.SUPERSEDED)
    );

    private record Transition(ApprovalState from, ApprovalState to) {
        @Override
        public String toString() {
            return from + " -> " + to;
        }
    }

    /** All 25 source/target combinations (5 states x 5 states). */
    private static Stream<Transition> allCombinations() {
        return EnumSet.allOf(ApprovalState.class).stream()
                .flatMap(from -> EnumSet.allOf(ApprovalState.class).stream()
                        .map(to -> new Transition(from, to)));
    }

    @Nested
    @DisplayName("Legal transitions (the 4 required paths)")
    class LegalTransitions {

        @Test
        @DisplayName("DRAFT -> IN_REVIEW succeeds")
        void draftToInReview() {
            assertThat(machine.transition(ApprovalState.DRAFT, ApprovalState.IN_REVIEW))
                    .isEqualTo(ApprovalState.IN_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW -> APPROVED succeeds")
        void inReviewToApproved() {
            assertThat(machine.transition(ApprovalState.IN_REVIEW, ApprovalState.APPROVED))
                    .isEqualTo(ApprovalState.APPROVED);
        }

        @Test
        @DisplayName("IN_REVIEW -> REJECTED succeeds")
        void inReviewToRejected() {
            assertThat(machine.transition(ApprovalState.IN_REVIEW, ApprovalState.REJECTED))
                    .isEqualTo(ApprovalState.REJECTED);
        }

        @Test
        @DisplayName("APPROVED -> SUPERSEDED succeeds")
        void approvedToSuperseded() {
            assertThat(machine.transition(ApprovalState.APPROVED, ApprovalState.SUPERSEDED))
                    .isEqualTo(ApprovalState.SUPERSEDED);
        }

        @ParameterizedTest(name = "{0} is legal and isLegal() agrees")
        @MethodSource("com.adtech.iaterap.backend.approval.ApprovalStateMachineTest#legalTransitionsProvider")
        void isLegalReportsTrueForEachLegalTransition(Transition t) {
            assertThat(machine.isLegal(t.from(), t.to())).isTrue();
        }
    }

    static Stream<Transition> legalTransitionsProvider() {
        return LEGAL.stream();
    }

    @Nested
    @DisplayName("Explicit self-transitions must be rejected")
    class SelfTransitions {

        @ParameterizedTest(name = "{0} -> {0} is illegal")
        @EnumSource(ApprovalState.class)
        void selfTransitionsAreIllegal(ApprovalState state) {
            assertThatThrownBy(() -> machine.transition(state, state))
                    .isInstanceOf(InvalidApprovalTransitionException.class);
            assertThat(machine.isLegal(state, state)).isFalse();
        }
    }

    @Nested
    @DisplayName("Specifically forbidden transition: APPROVED -> DRAFT")
    class ApprovedToDraft {

        @Test
        void isRejected() {
            assertThatThrownBy(() -> machine.transition(ApprovalState.APPROVED, ApprovalState.DRAFT))
                    .isInstanceOf(InvalidApprovalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Terminal states have no outgoing transitions")
    class TerminalStates {

        @Test
        void rejectedIsTerminal() {
            assertThat(machine.isTerminal(ApprovalState.REJECTED)).isTrue();
            assertThat(machine.legalTargets(ApprovalState.REJECTED)).isEmpty();
        }

        @Test
        void supersededIsTerminal() {
            assertThat(machine.isTerminal(ApprovalState.SUPERSEDED)).isTrue();
            assertThat(machine.legalTargets(ApprovalState.SUPERSEDED)).isEmpty();
        }

        @ParameterizedTest(name = "REJECTED -> {0} is illegal")
        @EnumSource(ApprovalState.class)
        void allTransitionsOutOfRejectedAreIllegal(ApprovalState target) {
            assertThatThrownBy(() -> machine.transition(ApprovalState.REJECTED, target))
                    .isInstanceOf(InvalidApprovalTransitionException.class);
        }

        @ParameterizedTest(name = "SUPERSEDED -> {0} is illegal")
        @EnumSource(ApprovalState.class)
        void allTransitionsOutOfSupersededAreIllegal(ApprovalState target) {
            assertThatThrownBy(() -> machine.transition(ApprovalState.SUPERSEDED, target))
                    .isInstanceOf(InvalidApprovalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Complete 5x5 = 25 combination matrix")
    class FullMatrix {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.adtech.iaterap.backend.approval.ApprovalStateMachineTest#allCombinations")
        void everyCombinationIsClassifiedCorrectly(Transition t) {
            boolean expectedLegal = LEGAL.contains(t);

            if (expectedLegal) {
                assertThat(machine.transition(t.from(), t.to())).isEqualTo(t.to());
                assertThat(machine.isLegal(t.from(), t.to())).isTrue();
            } else {
                assertThat(machine.isLegal(t.from(), t.to())).isFalse();
                assertThatThrownBy(() -> machine.transition(t.from(), t.to()))
                        .isInstanceOf(InvalidApprovalTransitionException.class);
            }
        }

        @Test
        @DisplayName("Exactly 25 combinations exist and exactly 4 are legal")
        void matrixShape() {
            long total = allCombinations().count();
            long legalCount = allCombinations().filter(LEGAL::contains).count();

            assertThat(total).isEqualTo(25);
            assertThat(legalCount).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Exception payload")
    class ExceptionPayload {

        @Test
        void carriesCurrentAndRequestedState() {
            InvalidApprovalTransitionException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    InvalidApprovalTransitionException.class,
                    () -> machine.transition(ApprovalState.APPROVED, ApprovalState.DRAFT)
            );

            assertThat(ex.getCurrentState()).isEqualTo(ApprovalState.APPROVED);
            assertThat(ex.getRequestedState()).isEqualTo(ApprovalState.DRAFT);
            assertThat(ex.getMessage()).contains("APPROVED").contains("DRAFT");
        }
    }

    @Nested
    @DisplayName("Null handling")
    class NullHandling {

        @Test
        void nullCurrentStateRejected() {
            assertThatThrownBy(() -> machine.transition(null, ApprovalState.DRAFT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullRequestedStateRejected() {
            assertThatThrownBy(() -> machine.transition(ApprovalState.DRAFT, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
