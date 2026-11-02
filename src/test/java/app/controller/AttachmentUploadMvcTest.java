package app.controller;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.support.UploadFixtureBytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.servlet.multipart.max-file-size=2KB",
        "spring.servlet.multipart.max-request-size=2KB"
})
@Transactional
class AttachmentUploadMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestPurchase testPurchase;
    private User admin;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        admin = userRepository.save(User.builder()
                .username("upload_admin_" + suffix)
                .password(passwordEncoder.encode("secret"))
                .email("upload_" + suffix + "@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .name("Upload Customer " + suffix)
                .number("UC" + suffix)
                .country(Country.GERMANY)
                .active(true)
                .build());

        Shop shop = shopRepository.save(Shop.builder()
                .name("Upload Shop " + suffix)
                .number("US" + suffix)
                .country(Country.GERMANY)
                .build());

        testPurchase = testPurchaseRepository.save(TestPurchase.builder()
                .number("TP" + suffix)
                .customer(customer)
                .shop(shop)
                .country(Country.GERMANY)
                .category(TestPurchaseCategory.S)
                .type(TestPurchaseType.FORWARDING_TO_CLIENT)
                .status(TestPurchaseStatus.INITIALISED)
                .active(true)
                .productPrice(100.0)
                .serviceFee(10.0)
                .postageFee(5.0)
                .build());
    }

    @Test
    void oversizeUploadShowsMessageOnPurchaseView() throws Exception {
        byte[] payload = new byte[4 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", payload);

        MvcResult redirect = mockMvc.perform(multipart("/attachments/upload/" + testPurchase.getId())
                        .file(file)
                        .with(csrf())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testpurchases/view/" + testPurchase.getId()))
                .andExpect(flash().attributeExists("uploadError"))
                .andExpect(flash().attribute("uploadError", containsString("too large")))
                .andReturn();

        MockHttpSession session = (MockHttpSession) redirect.getRequest().getSession();
        mockMvc.perform(get("/testpurchases/view/" + testPurchase.getId())
                        .session(session)
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("uploadError"))
                .andExpect(model().attribute("uploadError", containsString("DOCX")));
    }

    @Test
    void invalidTypeShowsMessageOnPurchaseView() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.doc", "application/msword", UploadFixtureBytes.LEGACY_OLE);

        mockMvc.perform(multipart("/attachments/upload/" + testPurchase.getId())
                        .file(file)
                        .with(csrf())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("uploadError"))
                .andExpect(flash().attribute("uploadError", containsString("Could not upload")));
    }
}
