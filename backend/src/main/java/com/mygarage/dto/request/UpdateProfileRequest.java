package com.mygarage.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Enter a valid 10-digit phone number")
    private String phone;
}
