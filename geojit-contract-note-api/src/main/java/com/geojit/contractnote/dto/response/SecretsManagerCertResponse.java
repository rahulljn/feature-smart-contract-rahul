package com.geojit.contractnote.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SecretsManagerCertResponse {
    private String  secretName;
    private String  description;
    private String  createdDate;
    private String  lastChangedDate;
    @JsonProperty("isActive")
    private boolean isActive;
}
