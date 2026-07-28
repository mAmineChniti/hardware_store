package tn.inovexahub.hardware_store.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpEmailListenerTest {

  @Mock private EmailService emailService;

  @InjectMocks private OtpEmailListener otpEmailListener;

  @Test
  void handleOtpEmailRequested_callsEmailServiceWithCorrectParams() {
    OtpEmailRequestedEvent event = new OtpEmailRequestedEvent("test@example.com", "123456", 10);

    otpEmailListener.handleOtpEmailRequested(event);

    verify(emailService).sendOtpEmail("test@example.com", "123456", 10);
  }
}
