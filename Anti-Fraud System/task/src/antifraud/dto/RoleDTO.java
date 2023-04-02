package antifraud.dto;

import javax.validation.constraints.NotNull;

public record RoleDTO (@NotNull String username, @NotNull String role) {
}
