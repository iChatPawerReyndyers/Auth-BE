package com.ichat.authbe.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default SmsSender: logs the message instead of sending a real text.
 * No SMS provider is configured in this project yet, so this is what runs
 * out of the box — read the OTP from the backend's console/log during
 * local testing. Replace with a real provider (e.g. Twilio) before this
 * needs to reach actual phones — see backend/docs/SMS_OTP_SETUP.md.
 */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.warn("=================================================================");
        log.warn(" [DEV SMS] No real SMS provider configured — logging instead.");
        log.warn(" To: {}", phoneNumber);
        log.warn(" Message: {}", message);
        log.warn(" See backend/docs/SMS_OTP_SETUP.md to send real texts.");
        log.warn("=================================================================");
    }
}
