package app.controller;

import app.TestPurchaseApplication;
import app.model.enums.Country;
import app.repository.UserRepository;
import app.support.TestActiveUsers;
import app.support.UploadFixtureBytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileAvatarUploadMvcTest {

    @MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAdminUser() {
        TestActiveUsers.seedStandardUsers(userRepository, passwordEncoder);
    }

    @AfterEach
    void removeSeededUsers() {
        TestActiveUsers.deleteStandardUsers(userRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "en,Could not upload the profile picture",
            "de,Das Profilbild konnte nicht hochgeladen werden",
            "bg,Снимката на профила не можа да бъде качена"
    })
    void invalidAvatarShowsLocalizedFieldError(String lang, String expectedSnippet) throws Exception {
        MockMultipartFile pdfAvatar = new MockMultipartFile(
                "imageFile", "avatar.pdf", "application/pdf", UploadFixtureBytes.TINY_PDF);

        mockMvc.perform(multipart("/profile")
                        .file(pdfAvatar)
                        .param("lang", lang)
                        .param("username", "admin")
                        .param("email", "admin@mock-user.test")
                        .param("country", Country.GERMANY.name())
                        .param("role", "ADMIN")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedSnippet)))
                .andExpect(content().string(containsString("JPEG")));
    }
}
