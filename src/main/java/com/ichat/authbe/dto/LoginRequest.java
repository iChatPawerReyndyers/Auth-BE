package com.ichat.authbe.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // Identifies which app is calling (e.g. "stickies", "galleries"). Decides
    // whether the subscription gate applies — see app.clients.free-ids.
    @NotBlank(message = "appId is required")
    private String appId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
}
