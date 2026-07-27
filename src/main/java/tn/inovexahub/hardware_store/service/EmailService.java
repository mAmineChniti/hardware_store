package tn.inovexahub.hardware_store.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Async
  public void sendOtpEmail(String to, String otpCode, int expiryMinutes) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject("Password Reset Code");
    message.setText(
        "Your password reset code is: "
            + otpCode
            + "\n\n"
            + "This code expires in "
            + expiryMinutes
            + " minutes.\n"
            + "If you did not request a password reset, ignore this email.");
    mailSender.send(message);
  }
}
