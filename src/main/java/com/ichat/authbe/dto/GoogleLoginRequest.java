package com.ichat.authbe.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequest {

    @NotBlank(message = "idToken is required")
    private String idToken;

    // Identifies which app is calling (e.g. "stickies", "galleries"). Decides
    // whether the subscription gate applies — see app.clients.free-ids.
    @NotBlank(message = "appId is required")
    private String appId;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
}
