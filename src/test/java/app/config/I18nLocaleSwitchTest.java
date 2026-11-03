package app.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class I18nLocaleSwitchTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    app.client.PricingClient pricingClient;

    @ParameterizedTest
    @CsvSource({
            "bg,Потребителско име",
            "de,Benutzername",
            "en,Username"
    })
    void switchingLanguageViaQueryParamUpdatesPage(String lang, String expectedUsernameLabel) throws Exception {
        mvc.perform(get("/login").param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedUsernameLabel)))
                .andExpect(header().string("Set-Cookie", containsString("lang=" + lang)));
    }

    @Test
    void localeChoicePersistsInLangCookieWithoutQueryParam() throws Exception {
        MvcResult switched = mvc.perform(get("/login").param("lang", "de"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie langCookie = switched.getResponse().getCookie("lang");
        assertThat(langCookie).isNotNull();
        assertThat(langCookie.getValue()).isEqualTo("de");

        mvc.perform(get("/login").cookie(langCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Benutzername")))
                .andExpect(content().string(containsString(">DE<")));
    }

    @Test
    void loginPageIncludesAccessibleLanguageSwitcher() throws Exception {
        mvc.perform(get("/login").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"langs\"")))
                .andExpect(content().string(containsString("aria-label=\"Language\"")))
                .andExpect(content().string(containsString("hreflang=\"de\"")))
                .andExpect(content().string(containsString("lang=bg")))
                .andExpect(content().string(containsString("lang=de")));
    }

    @Test
    void htmlLangAttributeReflectsActiveLocale() throws Exception {
        mvc.perform(get("/login").param("lang", "bg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"bg\"")));
    }

    @Test
    void invalidLocaleParamIsIgnoredAndDefaultsToEnglish() throws Exception {
        mvc.perform(get("/login").param("lang", "xx"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Username")));
    }
}
