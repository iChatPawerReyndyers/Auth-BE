package com.ichat.authbe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DevModeWarningRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevModeWarningRunner.class);

    @Value("${app.security.skip-password-check:false}")
    private boolean skipPasswordCheck;

    @Override
    public void run(String... args) {
        if (skipPasswordCheck) {
            log.warn("=================================================================");
            log.warn(" SECURITY WARNING: app.security.skip-password-check=true");
            log.warn(" /api/auth/login will accept ANY password for an existing user.");
            log.warn(" This must NEVER be enabled outside local development/testing.");
            log.warn("=================================================================");
        }
    }
}
