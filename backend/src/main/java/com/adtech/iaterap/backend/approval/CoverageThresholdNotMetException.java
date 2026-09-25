package com.adtech.iaterap.backend.approval;

/**
 * Raised when the frozen coverage report for an in-review procedure does not
 * satisfy one or more configured coverage thresholds.
 */
public class CoverageThresholdNotMetException extends RuntimeException {

    public CoverageThresholdNotMetException(Long procedureId, Integer versionNumber, String failedRules) {
        super("Cannot approve procedure " + procedureId + " version " + versionNumber
                + ": frozen coverage thresholds are not met for " + failedRules + ".");
    }
}
