package app.service;

import app.client.PricingClient;
import app.model.dto.ItemDTO;
import app.model.dto.PriceCalculationRequestDTO;
import app.model.dto.PriceCalculationResponseDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.TestPurchase;
import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.impl.TestPurchaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestPurchaseServiceUnitTest {

    @Mock
    private TestPurchaseRepository testPurchaseRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private PricingClient pricingClient;

    @Mock
    private StatusHistoryService statusHistoryService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TestPurchaseServiceImpl testPurchaseService;

    private UUID customerId;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
    }

    @Test
    void testPreviewPrice() {
        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customerId);
        dto.setShopId(shopId);
        dto.setCountry(Country.BULGARIA);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);

        ItemDTO item = new ItemDTO();
        item.setProductName("Product");
        item.setQuantity(2);
        item.setUnitPrice(100.0);
        dto.setItems(List.of(item));

        PriceCalculationResponseDTO response = new PriceCalculationResponseDTO();
        response.setTestPurchaseFee(50.0);
        response.setPostageFee(10.0);

        when(pricingClient.calculatePrice(any(PriceCalculationRequestDTO.class))).thenReturn(response);

        PriceCalculationResponseDTO result = testPurchaseService.previewPrice(dto);

        assertThat(result.getTestPurchaseFee()).isEqualTo(50.0);
        assertThat(result.getPostageFee()).isEqualTo(10.0);
        verify(pricingClient).calculatePrice(any(PriceCalculationRequestDTO.class));
    }

    @Test
    void testPreviewPriceMissingRequiredFields() {
        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();

        assertThatThrownBy(() -> testPurchaseService.previewPrice(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void testDelete() {
        UUID purchaseId = UUID.randomUUID();
        doNothing().when(testPurchaseRepository).deleteById(purchaseId);

        testPurchaseService.delete(purchaseId);

        verify(testPurchaseRepository).deleteById(purchaseId);
    }

    @Test
    void testFindByIdNotFound() {
        UUID purchaseId = UUID.randomUUID();
        when(testPurchaseRepository.findByIdWithRelations(purchaseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testPurchaseService.findById(purchaseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase not found");
    }

    @Test
    void testChangeStatus() {
        UUID purchaseId = UUID.randomUUID();
        TestPurchase purchase = TestPurchase.builder()
                .id(purchaseId)
                .status(TestPurchaseStatus.INITIALISED)
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(testPurchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(testPurchaseRepository.save(any(TestPurchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        doNothing().when(statusHistoryService).recordStatusChange(any(), any(), any(), anyString(), anyString());

        testPurchaseService.changeStatus(purchaseId, TestPurchaseStatus.PRODUCT_ORDERED, "Comment");

        assertThat(purchase.getStatus()).isEqualTo(TestPurchaseStatus.PRODUCT_ORDERED);
        verify(statusHistoryService).recordStatusChange(any(), any(), any(), anyString(), anyString());
        
        SecurityContextHolder.clearContext();
    }
}

