package tn.inovexahub.hardware_store.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.inovexahub.hardware_store.entity.Supplier;
import tn.inovexahub.hardware_store.exception.SupplierNotFoundException;
import tn.inovexahub.hardware_store.repository.SupplierRepository;

@Service
@Transactional
public class SupplierService {

  private final SupplierRepository supplierRepository;

  public SupplierService(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  public List<Supplier> getAllSuppliers() {
    return supplierRepository.findByDeletedFalse();
  }

  public Optional<Supplier> getSupplierById(Long id) {
    return supplierRepository.findById(id).filter(s -> !s.getDeleted());
  }

  public Optional<Supplier> getSupplierByTaxId(String taxIdentificationNumber) {
    return supplierRepository
        .findByTaxIdentificationNumber(taxIdentificationNumber)
        .filter(s -> !s.getDeleted());
  }

  public List<Supplier> searchSuppliers(String name) {
    return supplierRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
  }

  public Supplier createSupplier(Supplier supplier) {
    supplier.setId(null);
    supplier.setDeleted(false);
    return supplierRepository.save(supplier);
  }

  public Supplier updateSupplier(Long id, Supplier supplierDetails) {
    Supplier supplier = getSupplierById(id).orElseThrow(() -> new SupplierNotFoundException(id));

    supplier.setName(supplierDetails.getName());
    supplier.setPhone(supplierDetails.getPhone());
    supplier.setEmail(supplierDetails.getEmail());
    supplier.setAddress(supplierDetails.getAddress());
    supplier.setTaxIdentificationNumber(supplierDetails.getTaxIdentificationNumber());
    supplier.setContactPerson(supplierDetails.getContactPerson());
    supplier.setPaymentTerms(supplierDetails.getPaymentTerms());
    supplier.setNotes(supplierDetails.getNotes());

    return supplierRepository.save(supplier);
  }

  public void deleteSupplier(Long id) {
    Supplier supplier = getSupplierById(id).orElseThrow(() -> new SupplierNotFoundException(id));

    supplier.setDeleted(true);
    supplierRepository.save(supplier);
  }
}
