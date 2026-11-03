package app.service;

import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.filter.CustomerListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<Customer> findAll();

    List<Customer> findAllForCurrentUser();

    List<Customer> findAllForCurrentUser(boolean includeDeactivated);

    Page<Customer> findPageForCurrentUser(CustomerListFilter filter, Pageable pageable);

    long countForList(CustomerListFilter filter);

    List<Customer> findAllForListExport(CustomerListFilter filter, Pageable sortPageable, int maxRows);

    Customer findById(UUID id);

    Customer findByIdForCurrentUser(UUID id);

    Customer create(CustomerDTO dto);

    Customer update(UUID id, CustomerDTO dto);

    void deactivateCustomer(UUID id);

    void reactivateCustomer(UUID id);
}
