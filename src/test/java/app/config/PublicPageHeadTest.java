package app.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPageHeadTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private app.client.PricingClient pricingClient;

    @ParameterizedTest
    @CsvSource({
            "/,bg,bg",
            "/,de,de",
            "/,en,en",
            "/login,bg,bg",
            "/login,de,de",
            "/login,en,en",
            "/register,bg,bg",
            "/forgot-password,de,de"})
    void htmlLangMatchesActiveLocale(String path, String lang, String expectedLang) throws Exception {
        mvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"" + expectedLang + "\"")));
    }

    @ParameterizedTest
    @CsvSource({
            "/,bg,testkauf",
            "/,de,testkauf",
            "/,en,manage test purchases",
            "/login,bg,Вход – testkauf",
            "/login,de,Anmelden – testkauf",
            "/login,en,Log in – testkauf",
            "/forgot-password,de,property=\"og:title\""})
    void publicPagesExposeMetaTags(String path, String lang, String expectedSnippet) throws Exception {
        var result = mvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk());

        if (path.equals("/") || path.startsWith("/login")) {
            result.andExpect(content().string(containsString("<title")));
        } else {
            result.andExpect(content().string(containsString("<meta name=\"description\" content=\"")))
                    .andExpect(content().string(containsString("property=\"og:type\" content=\"website\"")))
                    .andExpect(content().string(containsString("property=\"og:locale\"")))
                    .andExpect(content().string(containsString("property=\"og:url\" content=\"http://localhost:8080" + path + "\"")));
        }
        result.andExpect(content().string(containsString(expectedSnippet)));
    }
}
