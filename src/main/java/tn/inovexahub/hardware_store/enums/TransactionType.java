package tn.inovexahub.hardware_store.enums;

/**
 * Enum representing credit transaction types. Section 9: CreditHistory entity - transactionType
 * field SALE: Positive amount (debt increase) PAYMENT: Negative amount (debt decrease) ADJUSTMENT:
 * Manual corrections
 */
public enum TransactionType {
  SALE,
  PAYMENT,
  ADJUSTMENT
}
