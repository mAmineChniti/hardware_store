package tn.inovexahub.hardware_store.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tn.inovexahub.hardware_store.dto.SupplierRequest;
import tn.inovexahub.hardware_store.dto.SupplierResponse;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.service.SupplierService;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

  @Mock private SupplierService supplierService;

  private SupplierController supplierController;

  @BeforeEach
  void setUp() {
    supplierController = new SupplierController(supplierService);
  }

  // --- getAllSuppliers ---

  @Test
  void getAllSuppliers_ReturnsList() {
    Supplier supplier = buildSupplier(1L, "ABC Hardware");
    when(supplierService.getAllSuppliers()).thenReturn(List.of(supplier));

    ResponseEntity<List<SupplierResponse>> response = supplierController.getAllSuppliers();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("ABC Hardware", response.getBody().getFirst().getName());
  }

  @Test
  void getAllSuppliers_EmptyList_ReturnsEmpty() {
    when(supplierService.getAllSuppliers()).thenReturn(List.of());

    ResponseEntity<List<SupplierResponse>> response = supplierController.getAllSuppliers();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
  }

  // --- getSupplierById ---

  @Test
  void getSupplierById_Found_ReturnsOk() {
    Supplier supplier = buildSupplier(1L, "ABC Hardware");
    when(supplierService.getSupplierById(1L)).thenReturn(Optional.of(supplier));

    ResponseEntity<SupplierResponse> response = supplierController.getSupplierById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().getId());
    assertEquals("ABC Hardware", response.getBody().getName());
  }

  @Test
  void getSupplierById_NotFound_ReturnsNotFound() {
    when(supplierService.getSupplierById(999L)).thenReturn(Optional.empty());

    ResponseEntity<SupplierResponse> response = supplierController.getSupplierById(999L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  // --- getSupplierByTaxId ---

  @Test
  void getSupplierByTaxId_Found_ReturnsOk() {
    Supplier supplier = buildSupplier(1L, "ABC Hardware");
    supplier.setTaxIdentificationNumber("123456789");
    when(supplierService.getSupplierByTaxId("123456789")).thenReturn(Optional.of(supplier));

    ResponseEntity<SupplierResponse> response = supplierController.getSupplierByTaxId("123456789");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("123456789", response.getBody().getTaxIdentificationNumber());
  }

  @Test
  void getSupplierByTaxId_NotFound_ReturnsNotFound() {
    when(supplierService.getSupplierByTaxId("999999999")).thenReturn(Optional.empty());

    ResponseEntity<SupplierResponse> response = supplierController.getSupplierByTaxId("999999999");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
  }

  // --- searchSuppliers ---

  @Test
  void searchSuppliers_ReturnsList() {
    Supplier supplier = buildSupplier(1L, "ABC Hardware");
    when(supplierService.searchSuppliers("ABC")).thenReturn(List.of(supplier));

    ResponseEntity<List<SupplierResponse>> response = supplierController.searchSuppliers("ABC");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("ABC Hardware", response.getBody().getFirst().getName());
  }

  @Test
  void searchSuppliers_NoResults_ReturnsEmpty() {
    when(supplierService.searchSuppliers("NonExistent")).thenReturn(List.of());

    ResponseEntity<List<SupplierResponse>> response =
        supplierController.searchSuppliers("NonExistent");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
  }

  // --- createSupplier ---

  @Test
  void createSupplier_ReturnsCreated() {
    SupplierRequest request = buildSupplierRequest();
    Supplier createdSupplier = buildSupplier(1L, "ABC Hardware");
    when(supplierService.createSupplier(any(Supplier.class))).thenReturn(createdSupplier);

    ResponseEntity<SupplierResponse> response = supplierController.createSupplier(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().getId());
    assertEquals("ABC Hardware", response.getBody().getName());
    verify(supplierService).createSupplier(any(Supplier.class));
  }

  // --- updateSupplier ---

  @Test
  void updateSupplier_Found_ReturnsOk() {
    SupplierRequest request = buildSupplierRequest();
    Supplier updatedSupplier = buildSupplier(1L, "Updated Hardware");
    when(supplierService.updateSupplier(eq(1L), any(Supplier.class))).thenReturn(updatedSupplier);

    ResponseEntity<SupplierResponse> response = supplierController.updateSupplier(1L, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Updated Hardware", response.getBody().getName());
    verify(supplierService).updateSupplier(eq(1L), any(Supplier.class));
  }

  @Test
  void updateSupplier_NotFound_ThrowsNotFoundException() {
    SupplierRequest request = buildSupplierRequest();
    when(supplierService.updateSupplier(eq(999L), any(Supplier.class)))
        .thenThrow(new SupplierNotFoundException(999L));

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> supplierController.updateSupplier(999L, request));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- deleteSupplier ---

  @Test
  void deleteSupplier_Found_ReturnsNoContent() {
    doNothing().when(supplierService).deleteSupplier(1L);

    ResponseEntity<Void> response = supplierController.deleteSupplier(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNull(response.getBody());
    verify(supplierService).deleteSupplier(1L);
  }

  @Test
  void deleteSupplier_NotFound_ThrowsNotFoundException() {
    doThrow(new SupplierNotFoundException(999L)).when(supplierService).deleteSupplier(999L);

    ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            ResponseStatusException.class, () -> supplierController.deleteSupplier(999L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  // --- helpers ---

  private Supplier buildSupplier(Long id, String name) {
    Supplier supplier = new Supplier();
    supplier.setId(id);
    supplier.setName(name);
    supplier.setPhone("+216 20 123 456");
    supplier.setEmail("contact@example.tn");
    supplier.setAddress("123 Main St, Tunis");
    supplier.setTaxIdentificationNumber("123456789");
    supplier.setContactPerson("John Smith");
    supplier.setPaymentTerms("Net 30 days");
    supplier.setNotes("Preferred supplier");
    supplier.setDeleted(false);
    return supplier;
  }

  private SupplierRequest buildSupplierRequest() {
    SupplierRequest request = new SupplierRequest();
    request.setName("ABC Hardware");
    request.setPhone("+216 20 123 456");
    request.setEmail("contact@example.tn");
    request.setAddress("123 Main St, Tunis");
    request.setTaxIdentificationNumber("123456789");
    request.setContactPerson("John Smith");
    request.setPaymentTerms("Net 30 days");
    request.setNotes("Preferred supplier");
    return request;
  }
}
