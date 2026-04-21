package com.geojit.contractnote.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long   expiresIn;
    private String email;
    private String name;
    private String role;
    private String userId;
}
