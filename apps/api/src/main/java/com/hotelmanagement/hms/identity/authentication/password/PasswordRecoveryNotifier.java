package com.hotelmanagement.hms.identity.authentication.password;

import com.hotelmanagement.hms.identity.authentication.config.PasswordRecoveryProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.net.URI;

@Component
public class PasswordRecoveryNotifier {
    private final ObjectProvider<JavaMailSender> mail;
    private final PasswordRecoveryProperties properties;
    public PasswordRecoveryNotifier(ObjectProvider<JavaMailSender> mail, PasswordRecoveryProperties properties) {
        this.mail = mail; this.properties = properties;
        if (properties.enabled()) {
            URI uri = URI.create(properties.frontendUrl());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || properties.from() == null
                    || properties.from().isBlank() || mail.getIfAvailable() == null)
                throw new IllegalStateException("Password recovery requires HTTPS frontend URL, sender and SMTP configuration.");
        }
    }
    public record Delivery(String email, String token) {
        @Override public String toString() { return "PasswordRecoveryDelivery[REDACTED]"; }
    }
    @Async
    @TransactionalEventListener
    public void deliver(Delivery delivery) {
        if (!properties.enabled()) return;
        var message = new SimpleMailMessage();
        message.setFrom(properties.from()); message.setTo(delivery.email());
        message.setSubject(delivery.token() == null ? "Your hotel account password changed" : "Reset your hotel account password");
        // A fragment keeps the credential out of HTTP request/access logs and referrers.
        message.setText(delivery.token() == null
                ? "Your password was changed and existing refresh sessions were revoked. If this was not you, contact your administrator."
                : "Choose your new password using this link (valid for 20 minutes):\n"
                    + properties.frontendUrl().replaceAll("/+$", "") + "/reset-password#token=" + delivery.token()
                    + "\nIf you did not request this, ignore this message.");
        try { mail.getObject().send(message); }
        catch (RuntimeException failure) {
            // Do not log provider exceptions: they may contain the message body or recipient.
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Password recovery email delivery failed.");
        }
    }
}
