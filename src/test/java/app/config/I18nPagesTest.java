package app.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class I18nPagesTest {
    @Autowired MockMvc mvc;
    @MockBean app.client.PricingClient pricingClient;

    @ParameterizedTest
    @CsvSource({
            "/login,bg,Потребителско име", "/login,de,Benutzername", "/login,en,Username",
            "/register,bg,Регистрация", "/register,de,Registrieren",
            "/,bg,Защо да изберете нас?", "/,de,Warum unsere Plattform?", "/,en,Why Choose Our Platform?",
            "/forgot-password,bg,Забравена парола", "/forgot-password,de,Passwort vergessen"})
    void pageIsTranslated(String path, String lang, String expected) throws Exception {
        mvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expected)));
    }
}
