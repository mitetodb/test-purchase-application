package app.repository;

import app.model.entity.Shop;
import app.model.enums.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ShopRepositoryTest {

    @Autowired
    private ShopRepository shopRepository;

    @Test
    void testSaveAndFindShop() {
        Shop shop = Shop.builder()
                .name("Test Shop")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        Shop saved = shopRepository.save(shop);

        assertThat(saved.getId()).isNotNull();
        assertThat(shopRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindLastSequence() {
        Shop shop1 = Shop.builder()
                .name("Shop 1")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        Shop shop2 = Shop.builder()
                .name("Shop 2")
                .country(Country.BULGARIA)
                .number("0002")
                .build();

        shopRepository.save(shop1);
        shopRepository.save(shop2);

        Long lastSeq = shopRepository.findLastSequence();
        assertThat(lastSeq).isNotNull();
    }
}

