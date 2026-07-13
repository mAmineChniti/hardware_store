package tn.inovexahub.hardware_store.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

  private Long id;
  private String name;
  private String phone;
  private String email;
  private String address;
  private String taxIdentificationNumber;
  private String contactPerson;
  private String paymentTerms;
  private String notes;
  private Boolean deleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
