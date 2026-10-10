package com.adtech.iaterap.backend.ai;

/** Raised when the independent AI advisory service cannot honour a request. */
public class AiAdvisoryUnavailableException extends RuntimeException {
    public AiAdvisoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
