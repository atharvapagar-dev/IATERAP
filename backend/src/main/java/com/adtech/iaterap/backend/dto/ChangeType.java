package com.adtech.iaterap.backend.dto;

/**
 * AI-182 / T7-B1: the state of a single comparison unit (a field or a
 * whole Test Step) between two procedure revisions. Shared by
 * {@link FieldChangeResponse} and {@link StepComparisonResponse} so the
 * frontend (AI-183 / T7-B2) can render both with the same enum/state
 * handling instead of two parallel string vocabularies.
 */
public enum ChangeType {
    ADDED,
    REMOVED,
    CHANGED,
    UNCHANGED
}
