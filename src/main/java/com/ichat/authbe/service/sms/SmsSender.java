package com.ichat.authbe.service.sms;

/**
 * Abstraction over whatever sends an SMS. Swap the default
 * {@link ConsoleSmsSender} for a real provider (e.g. Twilio) by adding a
 * new implementation and marking it @Primary — see
 * backend/docs/SMS_OTP_SETUP.md.
 */
public interface SmsSender {
    void send(String phoneNumber, String message);
}
