package com.example.groupproject.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    public void sendEmail(String to,
            String subject,
            String text) {
        SimpleMailMessage message =
                new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
    public void sendVerificationEmail(
            String to,
            String fullName,
            String verifyLink) {
        sendEmail(
                to,
                "Verify your account",
                "Hello " + fullName
                        + "\n\n"
                        + "Click link below to verify account:"
                        + "\n\n"
                        + verifyLink
                        + "\n\n"
                        + "Link expires in 30 minutes."
        );
    }
    public void sendResetPasswordEmail(String to, String otp) {
        sendEmail(to, "Reset password OTP", "Your OTP is: "
                        + otp
                        + "\n\n"
                        + "OTP expires in 10 minutes."
        );
    }
}