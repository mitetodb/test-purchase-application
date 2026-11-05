package app.service;

import app.TestPurchaseApplication;
import app.model.entity.Attachment;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.AttachmentRepository;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.support.TestPurchaseTestDataCleaner;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TestPurchaseReportExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    private User admin;
    private User accountManager;
    private User customerUser;
    private User mysteryShopper;
    private Customer managedCustomer;
    private Customer otherCustomer;
    private Shop shop;
    private TestPurchase managedPurchase;
    private TestPurchase otherPurchase;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        admin = saveUser("report_admin", Role.ADMIN);
        accountManager = saveUser("report_am", Role.ACCOUNT_MANAGER);
        mysteryShopper = saveUser("report_ms", Role.MYSTERY_SHOPPER);
        User salesManager = saveUser("report_sm", Role.SALES_MANAGER);

        managedCustomer = saveCustomer("Report Managed Co", salesManager);
        otherCustomer = saveCustomer("Report Other Co", salesManager);
        accountManager.getManagedCustomers().add(managedCustomer);
        userRepository.save(accountManager);

        customerUser = saveUser("report_customer", Role.CUSTOMER);
        customerUser.setCustomer(managedCustomer);
        userRepository.save(customerUser);

        shop = saveShop("ReportShop");

        managedPurchase = savePurchase("TP-REPORT-OK", managedCustomer, TestPurchaseStatus.IN_PROGRESS,
                mysteryShopper);
        otherPurchase = savePurchase("TP-REPORT-OTHER", otherCustomer, TestPurchaseStatus.INITIALISED, null);

        attachmentRepository.save(Attachment.builder()
                .testPurchase(managedPurchase)
                .name("receipt-demo.pdf")
                .category(AttachmentCategory.INVOICE)
                .fileType(AttachmentFileType.PDF)
                .filePath("/tmp/receipt-demo.pdf")
                .build());
    }

    @Test
    void pdfReportForbiddenForUnrelatedAccountManager() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + otherPurchase.getId() + "/report.pdf")
                        .with(user(userDetailsService.loadUserByUsername(accountManager.getUsername()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdfReportContainsPurchaseDataForAdmin() throws Exception {
        byte[] body = mockMvc.perform(get("/testpurchases/view/" + managedPurchase.getId() + "/report.pdf")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("test-purchase-TP-REPORT-OK.pdf")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        String pdfText = extractPdfText(body);
        assertThat(pdfText).contains("TP-REPORT-OK");
        assertThat(pdfText).contains("Report Managed Co");
        assertThat(pdfText).contains("receipt-demo.pdf");
        assertThat(pdfText).contains(mysteryShopper.getUsername());
    }

    @Test
    void pdfReportHidesMysteryShopperNameForCustomer() throws Exception {
        byte[] body = mockMvc.perform(get("/testpurchases/view/" + managedPurchase.getId() + "/report.pdf")
                        .with(user(userDetailsService.loadUserByUsername(customerUser.getUsername()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String pdfText = extractPdfText(body);
        assertThat(pdfText).contains("TP-REPORT-OK");
        assertThat(pdfText).contains("Assigned (name withheld)");
        assertThat(pdfText).doesNotContain(mysteryShopper.getUsername());
    }

    private static String extractPdfText(byte[] pdfBytes) throws IOException {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    @Test
    void excelExportRespectsStatusFilter() throws Exception {
        savePurchase("TP-FILTER-A", managedCustomer, TestPurchaseStatus.INITIALISED, null);
        savePurchase("TP-FILTER-B", managedCustomer, TestPurchaseStatus.CLOSED, null);

        byte[] body = mockMvc.perform(get("/testpurchases/export.xlsx")
                        .param("status", TestPurchaseStatus.INITIALISED.name())
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("test-purchases.xlsx")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(body))) {
            var sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            assertThat(lastRow).isGreaterThanOrEqualTo(1);
            var numbers = new java.util.ArrayList<String>();
            for (int row = 1; row <= lastRow; row++) {
                var cell = sheet.getRow(row).getCell(0);
                if (cell != null) {
                    numbers.add(cell.getStringCellValue());
                }
            }
            assertThat(numbers).contains("TP-FILTER-A", "TP-REPORT-OTHER");
            assertThat(numbers).doesNotContain("TP-FILTER-B", "TP-REPORT-OK");
        }
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@report-export.test")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .country(Country.GERMANY)
                .active(true)
                .managedCustomers(new HashSet<>())
                .build());
    }

    private Customer saveCustomer(String name, User salesManager) {
        return customerRepository.save(Customer.builder()
                .number(String.format("%05d", Math.abs(UUID.randomUUID().hashCode() % 100000)))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build());
    }

    private Shop saveShop(String name) {
        return shopRepository.save(Shop.builder()
                .number(String.format("%05d", Math.abs(UUID.randomUUID().hashCode() % 100000)))
                .name(name)
                .country(Country.GERMANY)
                .build());
    }

    private TestPurchase savePurchase(String number, Customer customer, TestPurchaseStatus status, User ms) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop)
                .country(Country.GERMANY)
                .category(TestPurchaseCategory.M)
                .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                .productPrice(10.0)
                .serviceFee(1.0)
                .postageFee(1.0)
                .active(true)
                .mysteryShopper(ms)
                .build();
        tp = testPurchaseRepository.saveAndFlush(tp);
        tp.setStatus(status);
        tp.setCreatedOn(LocalDate.of(2026, 1, 15).atStartOfDay());
        return testPurchaseRepository.saveAndFlush(tp);
    }
}
