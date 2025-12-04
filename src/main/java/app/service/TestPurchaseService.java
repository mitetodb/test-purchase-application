package app.service;

import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;

import java.util.List;
import java.util.UUID;

public interface TestPurchaseService {

    TestPurchase create(TestPurchaseCreateDTO dto);

    TestPurchase edit(UUID id, TestPurchaseEditDTO dto);

    void delete(UUID id);

    TestPurchase findById(UUID id);

    List<TestPurchase> findAll();

    void changeStatus(UUID id, TestPurchaseStatus newStatus, String comment);

    List<TestPurchase> findAllForCurrentUser();

    TestPurchase findByIdForCurrentUser(UUID id);
}
