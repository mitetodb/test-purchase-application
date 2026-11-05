package app.controller;

import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.service.TestPurchaseService;
import app.service.AttachmentService;
import app.service.CustomerService;
import app.service.ShopService;
import app.service.AccessControlService;
import app.service.QuestionnaireService;
import app.service.StatusHistoryService;
import app.service.AccessControlService;
import app.service.VisitAppointmentService;
import app.service.UploadErrorMessages;
import app.security.CustomUserDetailsService;
import app.security.LoginAuthenticationFailureHandler;
import app.security.LoginAuthenticationSuccessHandler;
import app.security.LoginRateLimitFilter;
import app.service.LoginSecurityService;
import app.support.ListPagingSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
        controllers = TestPurchaseController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TestPurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private app.client.PricingClient pricingClient;

    @MockBean
    private TestPurchaseService testPurchaseService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private ShopService shopService;

    @MockBean
    private AttachmentService attachmentService;

    @MockBean
    private StatusHistoryService statusHistoryService;

    @MockBean
    private QuestionnaireService questionnaireService;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private app.service.UserService userService;

    @MockBean
    private ListPagingSupport listPagingSupport;

    @MockBean
    private app.config.AppListExportProperties exportProperties;

    @MockBean
    private app.support.ListExportResponseFactory exportResponses;

    @MockBean
    private VisitAppointmentService visitAppointmentService;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private app.service.TestPurchaseReportService testPurchaseReportService;

    @MockBean
    private LoginSecurityService loginSecurityService;

    @MockBean
    private LoginRateLimitFilter loginRateLimitFilter;

    @MockBean
    private LoginAuthenticationFailureHandler loginAuthenticationFailureHandler;

    @MockBean
    private LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UploadErrorMessages uploadErrorMessages;

    @MockBean
    private app.security.TwoFactorGateFilter twoFactorGateFilter;

    @Test
    void testViewPageReturns200() throws Exception {
        UUID testPurchaseId = UUID.randomUUID();
        TestPurchaseViewDTO dto = new TestPurchaseViewDTO();
        dto.setId(testPurchaseId);
        dto.setNumber("TP-1001");
        dto.setStatus(TestPurchaseStatus.INITIALISED);

        given(testPurchaseService.findByIdForCurrentUser(any(UUID.class)))
                .willReturn(dto);
        TestPurchase entity = new TestPurchase();
        entity.setId(testPurchaseId);
        given(testPurchaseService.findById(any(UUID.class))).willReturn(entity);
        given(accessControlService.canManageVisitAppointment(any(TestPurchase.class))).willReturn(false);
        given(attachmentService.getByTestPurchase(any(UUID.class)))
                .willReturn(List.of());
        given(statusHistoryService.getHistory(any(UUID.class)))
                .willReturn(List.of());
        TestPurchase entity = new TestPurchase();
        entity.setId(testPurchaseId);
        entity.setStatus(TestPurchaseStatus.INITIALISED);
        given(testPurchaseService.findEntityByIdForCurrentUser(any(UUID.class))).willReturn(entity);
        given(accessControlService.canFillQuestionnaire(any(TestPurchase.class))).willReturn(false);
        given(questionnaireService.hasResponse(any(UUID.class))).willReturn(false);
        given(customerService.findAllForCurrentUser()).willReturn(List.of());
        given(shopService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/testpurchases/view/" + testPurchaseId))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("purchase"))
                .andExpect(model().attributeExists("attachments"))
                .andExpect(model().attributeExists("history"))
                .andExpect(model().attributeExists("statuses"));
    }
}
