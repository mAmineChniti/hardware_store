package tn.inovexahub.hardware_store.service;

import java.math.BigDecimal;

/** Shared VAT rate helpers: percentage normalization and the default rate. */
public final class VatRates {

  public static final BigDecimal DEFAULT_RATE = new BigDecimal("19.00");

  private VatRates() {}

  /**
   * Normalize a persisted VAT rate to percentage units. Legacy documents may store fractional rates
   * (e.g. 0.19 for 19%); values below 1 are treated as fractions and converted to percentages. A
   * null rate falls back to the 19% default.
   */
  public static BigDecimal normalize(BigDecimal vatRate) {
    if (vatRate == null) {
      return DEFAULT_RATE;
    }
    return vatRate.compareTo(BigDecimal.ONE) < 0
        ? vatRate.multiply(BigDecimal.valueOf(100))
        : vatRate;
  }
}
