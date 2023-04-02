package antifraud.dto;

import javax.validation.constraints.NotNull;

public record OperationDTO(@NotNull String username, @NotNull String operation) {
}
