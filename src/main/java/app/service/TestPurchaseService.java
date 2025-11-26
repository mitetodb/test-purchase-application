package app.service;

import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.entity.TestPurchase;

import java.util.List;
import java.util.UUID;

public interface TestPurchaseService {

    TestPurchase create(TestPurchaseCreateDTO dto);

    TestPurchase edit(UUID id, TestPurchaseEditDTO dto);

    void delete(UUID id);

    TestPurchase findById(UUID id);

    List<TestPurchase> findAll();
}
