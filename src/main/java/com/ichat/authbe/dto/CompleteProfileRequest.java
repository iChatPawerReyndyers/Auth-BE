package com.ichat.authbe.dto;

import jakarta.validation.constraints.*;

public class CompleteProfileRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Birth year is required")
    @Min(value = 1900, message = "Birth year is invalid")
    @Max(value = 2026, message = "Birth year is invalid")
    private Integer birthYear;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be 7-15 digits, optionally starting with +")
    private String phoneNumber;

    // Optional for backwards compatibility; the SDK supplies it so a token
    // can be issued for the app whose login initiated profile completion.
    private String appId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
}
