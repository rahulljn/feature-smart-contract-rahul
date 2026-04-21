package com.geojit.contractnote.dto.request;

import com.geojit.contractnote.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 2, max = 200)
    private String name;
    @Size(min = 8)
    private String password;
    @NotNull
    private User.Role role;
}
