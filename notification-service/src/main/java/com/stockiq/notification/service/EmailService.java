package com.stockiq.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordReset(String toEmail, String resetToken, String baseUrl) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("StockIQ — Reset your password");
        msg.setText("""
                Hello,

                You requested a password reset for your StockIQ account.
                Click the link below to reset your password (valid for 1 hour):

                %s

                If you did not request this, ignore this email.

                — StockIQ Team
                """.formatted(resetLink));
        try {
            mailSender.send(msg);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendWelcome(String toEmail, String username) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Welcome to StockIQ");
        msg.setText("""
                Hi %s,

                Welcome to StockIQ — your real-time stock and ETF research platform.

                You can now:
                  • Track the Magnificent 7 and S&P 100 movers in real time
                  • Build and manage your investment portfolio
                  • Set price alerts on your watchlist

                Log in at: https://stockiq.yourdomain.com

                — StockIQ Team
                """.formatted(username));
        try {
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send welcome email: {}", e.getMessage());
        }
    }

    public void sendPriceAlert(String toEmail, String symbol, String alertType, double targetPrice, double currentPrice) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("StockIQ Alert — %s %s $%.2f".formatted(symbol, alertType, targetPrice));
        msg.setText("""
                Your price alert triggered for %s:

                Alert type:    %s $%.2f
                Current price: $%.2f

                Log in to StockIQ to review and take action.

                — StockIQ Team
                """.formatted(symbol, alertType, targetPrice, currentPrice));
        try {
            mailSender.send(msg);
            log.info("Price alert email sent to {} for {} {}", toEmail, symbol, alertType);
        } catch (Exception e) {
            log.warn("Failed to send price alert email: {}", e.getMessage());
        }
    }
}
