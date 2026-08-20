package tn.inovexahub.hardware_store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

  @Mock private SupplierRepository supplierRepository;

  @InjectMocks private SupplierService supplierService;

  private Supplier testSupplier;

  @BeforeEach
  void setUp() {
    testSupplier = new Supplier();
    testSupplier.setId(1L);
    testSupplier.setName("Test Supplier");
    testSupplier.setPhone("+216 20 123 456");
    testSupplier.setEmail("supplier@test.com");
    testSupplier.setAddress("123 Supplier St");
    testSupplier.setTaxIdentificationNumber("123456789");
    testSupplier.setContactPerson("John Doe");
    testSupplier.setPaymentTerms("Net 30 days");
    testSupplier.setNotes("Preferred supplier");
    testSupplier.setDeleted(false);
  }

  @Test
  void getAllSuppliers_ReturnsActiveSuppliers() {
    when(supplierRepository.findByDeletedFalse()).thenReturn(Arrays.asList(testSupplier));

    List<Supplier> suppliers = supplierService.getAllSuppliers();

    assertNotNull(suppliers);
    assertEquals(1, suppliers.size());
    assertEquals("Test Supplier", suppliers.get(0).getName());
    verify(supplierRepository).findByDeletedFalse();
  }

  @Test
  void getSupplierById_ExistingSupplier_ReturnsSupplier() {
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

    Optional<Supplier> result = supplierService.getSupplierById(1L);

    assertTrue(result.isPresent());
    assertEquals("Test Supplier", result.get().getName());
  }

  @Test
  void getSupplierById_NonExistingSupplier_ReturnsEmpty() {
    when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Supplier> result = supplierService.getSupplierById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  void getSupplierById_DeletedSupplier_ReturnsEmpty() {
    testSupplier.setDeleted(true);
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

    Optional<Supplier> result = supplierService.getSupplierById(1L);

    assertFalse(result.isPresent());
  }

  @Test
  void getSupplierByTaxId_ExistingSupplier_ReturnsSupplier() {
    when(supplierRepository.findByTaxIdentificationNumber("123456789"))
        .thenReturn(Optional.of(testSupplier));

    Optional<Supplier> result = supplierService.getSupplierByTaxId("123456789");

    assertTrue(result.isPresent());
    assertEquals("123456789", result.get().getTaxIdentificationNumber());
  }

  @Test
  void getSupplierByTaxId_NonExistingSupplier_ReturnsEmpty() {
    when(supplierRepository.findByTaxIdentificationNumber("999999999"))
        .thenReturn(Optional.empty());

    Optional<Supplier> result = supplierService.getSupplierByTaxId("999999999");

    assertFalse(result.isPresent());
  }

  @Test
  void getSupplierByTaxId_DeletedSupplier_ReturnsEmpty() {
    testSupplier.setDeleted(true);
    when(supplierRepository.findByTaxIdentificationNumber("123456789"))
        .thenReturn(Optional.of(testSupplier));

    Optional<Supplier> result = supplierService.getSupplierByTaxId("123456789");

    assertFalse(result.isPresent());
  }

  @Test
  void searchSuppliers_ReturnsMatchingSuppliers() {
    when(supplierRepository.findByNameContainingIgnoreCaseAndDeletedFalse("Test"))
        .thenReturn(Arrays.asList(testSupplier));

    List<Supplier> results = supplierService.searchSuppliers("Test");

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("Test Supplier", results.get(0).getName());
    verify(supplierRepository).findByNameContainingIgnoreCaseAndDeletedFalse("Test");
  }

  @Test
  void createSupplier_SetsDefaultsAndSaves() {
    Supplier newSupplier = new Supplier();
    newSupplier.setName("New Supplier");
    newSupplier.setEmail("new@supplier.com");
    newSupplier.setId(5L);

    when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

    Supplier savedSupplier = supplierService.createSupplier(newSupplier);

    assertNotNull(savedSupplier);
    assertNull(newSupplier.getId());
    assertFalse(newSupplier.getDeleted());
    verify(supplierRepository).save(newSupplier);
  }

  @Test
  void updateSupplier_ExistingSupplier_UpdatesFields() {
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
    when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

    Supplier updatedDetails = new Supplier();
    updatedDetails.setName("Updated Supplier");
    updatedDetails.setPhone("+216 99 888 777");
    updatedDetails.setEmail("updated@supplier.com");
    updatedDetails.setAddress("456 New St");
    updatedDetails.setTaxIdentificationNumber("987654321");
    updatedDetails.setContactPerson("Jane Doe");
    updatedDetails.setPaymentTerms("Net 60 days");
    updatedDetails.setNotes("Updated notes");

    Supplier result = supplierService.updateSupplier(1L, updatedDetails);

    assertNotNull(result);
    assertEquals("Updated Supplier", testSupplier.getName());
    assertEquals("+216 99 888 777", testSupplier.getPhone());
    assertEquals("updated@supplier.com", testSupplier.getEmail());
    assertEquals("456 New St", testSupplier.getAddress());
    assertEquals("987654321", testSupplier.getTaxIdentificationNumber());
    assertEquals("Jane Doe", testSupplier.getContactPerson());
    assertEquals("Net 60 days", testSupplier.getPaymentTerms());
    assertEquals("Updated notes", testSupplier.getNotes());
    verify(supplierRepository).save(testSupplier);
  }

  @Test
  void updateSupplier_NonExistingSupplier_ThrowsException() {
    when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

    Supplier updatedDetails = new Supplier();
    updatedDetails.setName("Updated Name");

    assertThrows(
        SupplierNotFoundException.class,
        () -> supplierService.updateSupplier(999L, updatedDetails));
  }

  @Test
  void deleteSupplier_SetsDeletedFlag() {
    when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
    when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

    supplierService.deleteSupplier(1L);

    assertTrue(testSupplier.getDeleted());
    verify(supplierRepository).save(testSupplier);
  }

  @Test
  void deleteSupplier_NonExistingSupplier_ThrowsException() {
    when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(SupplierNotFoundException.class, () -> supplierService.deleteSupplier(999L));
  }
}
