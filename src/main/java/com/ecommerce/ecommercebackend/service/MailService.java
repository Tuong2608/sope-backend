package com.ecommerce.ecommercebackend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Gửi email giao dịch (quên mật khẩu, xác nhận đăng ký) qua SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromAddress;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String body = buildHtmlEmail(
                "Đặt lại mật khẩu SOPE",
                "Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu cho tài khoản SOPE này. "
                        + "Bấm nút bên dưới để đặt mật khẩu mới. Nếu không phải bạn, hãy bỏ qua email này.",
                "Đặt lại mật khẩu",
                resetLink
        );
        send(toEmail, "Đặt lại mật khẩu SOPE", body);
    }

    public void sendVerificationEmail(String toEmail, String verifyLink) {
        String body = buildHtmlEmail(
                "Xác nhận địa chỉ email",
                "Cảm ơn bạn đã đăng ký tài khoản SOPE. Bấm nút bên dưới để xác nhận địa chỉ email này.",
                "Xác nhận email",
                verifyLink
        );
        send(toEmail, "Xác nhận email đăng ký SOPE", body);
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            mailSender.send(message);
            log.info("[MAIL] Đã gửi '{}' tới {}", subject, toEmail);
        } catch (Exception ex) {
            log.error("[MAIL] Gửi mail thất bại tới {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildHtmlEmail(String title, String message, String buttonText, String link) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;\">"
                + "<h2 style=\"color:#EE4D2D;\">" + title + "</h2>"
                + "<p style=\"color:#333;line-height:1.6;\">" + message + "</p>"
                + "<p style=\"text-align:center;margin:32px 0;\">"
                + "<a href=\"" + link + "\" style=\"background:#EE4D2D;color:#fff;padding:12px 24px;"
                + "border-radius:8px;text-decoration:none;font-weight:bold;\">" + buttonText + "</a>"
                + "</p>"
                + "<p style=\"color:#999;font-size:12px;\">Nếu nút không hoạt động, mở link sau: " + link + "</p>"
                + "</div>";
    }
}
