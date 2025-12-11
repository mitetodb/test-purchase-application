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

        Shop saved = shopRepository.saveAndFlush(shop);

        assertThat(saved.getId()).isNotNull();

        var found = shopRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNumber()).isEqualTo("0001");
        assertThat(found.get().getName()).isEqualTo("Test Shop");
        assertThat(found.get().getCountry()).isEqualTo(Country.BULGARIA);
    }

    @Test
    void testFindLastSequence() {
        long base = shopRepository.findLastSequence();

        Shop shop1 = Shop.builder()
                .name("Shop 1")
                .country(Country.BULGARIA)
                .number(String.format("%04d", base + 1))
                .build();

        Shop shop2 = Shop.builder()
                .name("Shop 2")
                .country(Country.BULGARIA)
                .number(String.format("%04d", base + 2))
                .build();

        shopRepository.saveAndFlush(shop1);
        shopRepository.saveAndFlush(shop2);

        Long lastSeq = shopRepository.findLastSequence();
        assertThat(lastSeq).isEqualTo(base + 2);
    }
}

