package com.adtech.iaterap.backend.approval;

/**
 * Thrown by {@link ApprovalStateMachine} when a requested state transition
 * is not legal for the current state. Mapped to HTTP 409 CONFLICT by
 * {@code GlobalExceptionHandler}.
 */
public class InvalidApprovalTransitionException extends RuntimeException {

    private final ApprovalState currentState;
    private final ApprovalState requestedState;

    public InvalidApprovalTransitionException(
            ApprovalState currentState,
            ApprovalState requestedState,
            String reason
    ) {
        super(buildMessage(currentState, requestedState, reason));
        this.currentState = currentState;
        this.requestedState = requestedState;
    }

    private static String buildMessage(
            ApprovalState currentState,
            ApprovalState requestedState,
            String reason
    ) {
        return "Illegal approval transition from " + currentState
                + " to " + requestedState + ": " + reason;
    }

    public ApprovalState getCurrentState() {
        return currentState;
    }

    public ApprovalState getRequestedState() {
        return requestedState;
    }
}
