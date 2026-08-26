package com.adtech.iaterap.backend.dto;

/**
 * AI-182 / T7-B1: a single field-level difference between two revisions of
 * the same (matched) Test Step. {@code oldValue}/{@code newValue} are
 * plain string representations so the frontend Version Diff Row
 * (AI-183 / T7-B2) can render any compared field uniformly without knowing
 * its underlying Java type.
 */
public class FieldChangeResponse {

    private String fieldName;
    private String oldValue;
    private String newValue;
    private ChangeType changeType;

    public FieldChangeResponse() {
    }

    public FieldChangeResponse(String fieldName, String oldValue, String newValue, ChangeType changeType) {
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
}
