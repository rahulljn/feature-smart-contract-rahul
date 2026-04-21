package com.geojit.contractnote.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuppressionRequest {
    @NotBlank @Email
    private String email;
    private String reason;
}
