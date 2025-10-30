package com.liyaqa.backend.internal.employee.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String? = null,
    
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String? = null,
    
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Invalid phone number format"
    )
    val phoneNumber: String? = null
)