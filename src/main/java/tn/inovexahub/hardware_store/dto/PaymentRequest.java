package tn.inovexahub.hardware_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.inovexahub.hardware_store.enums.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for processing a client payment")
public class PaymentRequest {

  @Schema(
      description = "Amount paid by the client",
      example = "200.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Amount paid is required")
  @Positive(message = "Amount paid must be positive")
  @jakarta.validation.constraints.Digits(
      integer = 13,
      fraction = 3,
      message = "Amount paid must not exceed 13 integer digits and 3 decimal places")
  private BigDecimal amountPaid;

  @Schema(
      description = "Payment method",
      example = "CASH",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Payment method is required")
  private PaymentMethod paymentMethod;
}
