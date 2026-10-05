package com.ichat.authbe.dto;

import jakarta.validation.constraints.NotBlank;

public class FacebookLoginRequest {

    @NotBlank(message = "accessToken is required")
    private String accessToken;

    // Identifies which app is calling (e.g. "stickies", "galleries"). Decides
    // whether the subscription gate applies — see app.clients.free-ids.
    @NotBlank(message = "appId is required")
    private String appId;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
}
