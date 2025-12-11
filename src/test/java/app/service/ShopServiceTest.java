package app.service;

import app.config.SecurityUtils;
import app.exception.ResourceNotFoundException;
import app.model.dto.ShopDTO;
import app.model.entity.Shop;
import app.model.enums.Country;
import app.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopService shopService;

    private Shop shop;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        shop = Shop.builder()
                .id(shopId)
                .name("Test Shop")
                .country(Country.BULGARIA)
                .number("0001")
                .build();
    }

    @Test
    void testFindAll() {
        when(shopRepository.findAll()).thenReturn(List.of(shop));

        List<Shop> result = shopService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(shop);
        verify(shopRepository).findAll();
    }

    @Test
    void testFindById() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        Shop result = shopService.findById(shopId);

        assertThat(result).isEqualTo(shop);
        verify(shopRepository).findById(shopId);
    }

    @Test
    void testFindByIdNotFound() {
        when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.findById(shopId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Shop not found");
    }

    @Test
    void testCreate() {
        ShopDTO dto = new ShopDTO();
        dto.setName("New Shop");
        dto.setCountry(Country.BULGARIA);

        when(shopRepository.findLastSequence()).thenReturn(0L);
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            Shop result = shopService.create(dto);

            assertThat(result.getName()).isEqualTo("New Shop");
            assertThat(result.getNumber()).isEqualTo("0001");
            verify(shopRepository).save(any(Shop.class));
        }
    }

    @Test
    void testUpdate() {
        ShopDTO dto = new ShopDTO();
        dto.setName("Updated Shop");
        dto.setCountry(Country.BULGARIA);

        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            Shop result = shopService.update(shopId, dto);

            assertThat(result.getName()).isEqualTo("Updated Shop");
            verify(shopRepository).save(shop);
        }
    }

    @Test
    void testDelete() {
        shopService.delete(shopId);

        verify(shopRepository).deleteById(shopId);
    }
}

