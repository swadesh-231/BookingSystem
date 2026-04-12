package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.Role;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRolesRequest {
    @NotEmpty(message = "Roles must not be empty")
    private Set<Role> roles;
}
