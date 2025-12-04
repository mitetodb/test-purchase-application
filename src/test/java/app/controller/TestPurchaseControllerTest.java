package app.controller;

import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.service.AttachmentService;
import app.service.StatusHistoryService;
import app.service.TestPurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestPurchaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TestPurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestPurchaseService testPurchaseService;

    @MockBean
    private AttachmentService attachmentService;

    @MockBean
    private StatusHistoryService statusHistoryService;

    @Test
    void testViewPageReturns200() throws Exception {
        TestPurchase tp = new TestPurchase();
        tp.setId(UUID.randomUUID());
        tp.setNumber("TP-1000");
        tp.setStatus(TestPurchaseStatus.INITIALISED);

        given(testPurchaseService.findByIdForCurrentUser(any(UUID.class)))
                .willReturn(tp);
        given(attachmentService.getByTestPurchase(any(UUID.class)))
                .willReturn(List.of());
        given(statusHistoryService.getHistory(any(UUID.class)))
                .willReturn(List.of());

        mockMvc.perform(get("/testpurchases/view/" + tp.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchase-view"))
                .andExpect(model().attributeExists("purchase"))
                .andExpect(model().attributeExists("attachments"))
                .andExpect(model().attributeExists("history"))
                .andExpect(model().attributeExists("statuses"));
    }
}
