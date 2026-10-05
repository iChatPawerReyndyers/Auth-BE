package com.ichat.authbe.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String accessToken;
    // True unless birthYear/phoneNumber are still missing (e.g. a
    // Google/Facebook sign-up that hasn't completed its profile yet).
    // The frontend shows a blocking "complete your profile" modal when false.
    private boolean profileComplete = true;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }

    public AuthResponse(boolean success, String message, String username, boolean profileComplete) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.profileComplete = profileComplete;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
}
