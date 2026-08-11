package com.adtech.iaterap.backend.approval;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic, reusable approval state machine.
 *
 * <p>This service centralizes every approval transition rule for the
 * platform. It is deliberately artifact-agnostic: it knows nothing about
 * TestStep, TestProcedure, or persistence. Callers pass in a current state
 * and a requested state; the machine either returns the (legal) target
 * state or throws {@link InvalidApprovalTransitionException}.</p>
 *
 * <p>Usage pattern for any versioned artifact service:</p>
 * <pre>
 *   ApprovalState next = approvalStateMachine.transition(entity.getApprovalStatus(), requested);
 *   entity.setApprovalStatus(next);
 *   repository.save(entity);
 * </pre>
 *
 * <p>Legal transitions:</p>
 * <pre>
 *   DRAFT      -&gt; IN_REVIEW
 *   IN_REVIEW  -&gt; APPROVED
 *   IN_REVIEW  -&gt; REJECTED
 *   APPROVED   -&gt; SUPERSEDED
 * </pre>
 *
 * <p>Everything else -- including every self-transition and
 * {@code APPROVED -> DRAFT} -- is illegal. REJECTED and SUPERSEDED are
 * terminal states with no outgoing legal transitions.</p>
 */
@Service
public class ApprovalStateMachine {

    private static final Map<ApprovalState, Set<ApprovalState>> LEGAL_TRANSITIONS = buildTransitionTable();

    private static Map<ApprovalState, Set<ApprovalState>> buildTransitionTable() {
        Map<ApprovalState, Set<ApprovalState>> table = new EnumMap<>(ApprovalState.class);

        table.put(ApprovalState.DRAFT, EnumSet.of(ApprovalState.IN_REVIEW));
        table.put(ApprovalState.IN_REVIEW, EnumSet.of(ApprovalState.APPROVED, ApprovalState.REJECTED));
        table.put(ApprovalState.APPROVED, EnumSet.of(ApprovalState.SUPERSEDED));
        table.put(ApprovalState.REJECTED, EnumSet.noneOf(ApprovalState.class));
        table.put(ApprovalState.SUPERSEDED, EnumSet.noneOf(ApprovalState.class));

        return Map.copyOf(table);
    }

    /**
     * Returns the set of states legally reachable from {@code currentState}.
     * Never null; empty for terminal states.
     */
    public Set<ApprovalState> legalTargets(ApprovalState currentState) {
        requireNonNull(currentState, "currentState");
        return LEGAL_TRANSITIONS.get(currentState);
    }

    /**
     * Returns true if moving from {@code currentState} to {@code requestedState}
     * is a legal transition. Self-transitions are always false: this state
     * machine does not model "no-op" transitions as legal.
     */
    public boolean isLegal(ApprovalState currentState, ApprovalState requestedState) {
        requireNonNull(currentState, "currentState");
        requireNonNull(requestedState, "requestedState");
        return LEGAL_TRANSITIONS.get(currentState).contains(requestedState);
    }

    /**
     * Validates and performs a transition, purely in terms of state values.
     * This method does NOT persist anything -- it is the caller's
     * responsibility to apply the returned state to an entity and save it.
     *
     * @return {@code requestedState}, if the transition is legal
     * @throws InvalidApprovalTransitionException if the transition is illegal
     */
    public ApprovalState transition(ApprovalState currentState, ApprovalState requestedState) {
        requireNonNull(currentState, "currentState");
        requireNonNull(requestedState, "requestedState");

        if (currentState == requestedState) {
            throw new InvalidApprovalTransitionException(
                    currentState,
                    requestedState,
                    "self-transitions are not permitted"
            );
        }

        if (!isLegal(currentState, requestedState)) {
            String reason = isTerminal(currentState)
                    ? currentState + " is a terminal state with no outgoing transitions"
                    : "no legal path from " + currentState + " to " + requestedState;
            throw new InvalidApprovalTransitionException(currentState, requestedState, reason);
        }

        return requestedState;
    }

    /**
     * True if {@code state} has no legal outgoing transitions.
     */
    public boolean isTerminal(ApprovalState state) {
        requireNonNull(state, "state");
        return LEGAL_TRANSITIONS.get(state).isEmpty();
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
