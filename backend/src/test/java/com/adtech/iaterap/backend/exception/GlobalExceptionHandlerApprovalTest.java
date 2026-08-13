package com.adtech.iaterap.backend.exception;

import com.adtech.iaterap.backend.approval.ApprovalState;
import com.adtech.iaterap.backend.approval.InvalidApprovalTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI-164 / Part E: illegal approval transitions must surface as HTTP 409
 * CONFLICT via the existing ApiError/GlobalExceptionHandler architecture,
 * not a new exception-handling mechanism.
 */
class GlobalExceptionHandlerApprovalTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidApprovalTransitionMapsTo409WithApiErrorBody() {
        InvalidApprovalTransitionException exception = new InvalidApprovalTransitionException(
                ApprovalState.APPROVED,
                ApprovalState.DRAFT,
                "no legal path from APPROVED to DRAFT"
        );

        ResponseEntity<ApiError> response = handler.handleInvalidApprovalTransition(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage())
                .contains("APPROVED")
                .contains("DRAFT");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
