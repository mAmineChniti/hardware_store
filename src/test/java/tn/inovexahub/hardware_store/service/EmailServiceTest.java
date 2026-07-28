package tn.inovexahub.hardware_store.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock private JavaMailSender mailSender;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService(mailSender);
  }

  @Test
  void sendOtpEmail_SendsCorrectMessage() {
    emailService.sendOtpEmail("user@example.com", "482916", 15);

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage sent = captor.getValue();
    org.junit.jupiter.api.Assertions.assertArrayEquals(
        new String[] {"user@example.com"}, sent.getTo());
    org.junit.jupiter.api.Assertions.assertEquals("Password Reset Code", sent.getSubject());
    org.junit.jupiter.api.Assertions.assertTrue(sent.getText().contains("482916"));
    org.junit.jupiter.api.Assertions.assertTrue(sent.getText().contains("expires in 15 minutes"));
  }
}
