package com.peccio.space_colony_simulator.application.colony;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateColonyRequest(
        @NotBlank(message = "Colony name is required")
        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        String name
) {
}
