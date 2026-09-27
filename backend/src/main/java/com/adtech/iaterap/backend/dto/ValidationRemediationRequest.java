package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.validation.RemediationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ValidationRemediationRequest {
    @NotNull private RemediationStatus status;
    @NotBlank @Size(max = 2000) private String note;
    public RemediationStatus getStatus() { return status; }
    public void setStatus(RemediationStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
