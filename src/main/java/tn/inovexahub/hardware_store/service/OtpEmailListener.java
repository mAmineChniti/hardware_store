package tn.inovexahub.hardware_store.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OtpEmailListener {

  private final EmailService emailService;

  public OtpEmailListener(EmailService emailService) {
    this.emailService = emailService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOtpEmailRequested(OtpEmailRequestedEvent event) {
    emailService.sendOtpEmail(event.getEmail(), event.getOtpCode(), event.getExpiryMinutes());
  }
}
