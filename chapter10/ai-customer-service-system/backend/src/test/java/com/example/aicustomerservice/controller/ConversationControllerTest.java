package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationDetail;
import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.ConversationPriority;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.entity.CustomerProfile;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.MessageSenderType;
import com.example.aicustomerservice.entity.MessageVisibility;
import com.example.aicustomerservice.entity.SupportConversation;
import com.example.aicustomerservice.service.ConversationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.test.context.TestPropertySource(properties="support.auth.enabled=false")
@WebMvcTest(ConversationController.class)
@Import(ConversationControllerTest.FixedClockConfiguration.class)
class ConversationControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");
    private static final String CONVERSATION_ID = "CONV-TEST-001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationService conversationService;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void shouldReturnInboxFieldsAndCalculateWaitingMinutesWithFixedClock() throws Exception {
        when(conversationService.search(
            eq(ConversationStatus.WAITING),
            eq(ConversationChannel.WECHAT),
            eq("王芳"),
            eq("售后二线"),
            eq("退款")
        )).thenReturn(List.of(conversation(ConversationStatus.WAITING, "")));

        mockMvc.perform(get("/api/support/conversations")
                .param("status", "WAITING")
                .param("channel", "WECHAT")
                .param("assignee", "王芳")
                .param("queue", "售后二线")
                .param("keyword", "退款"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].conversationId").value(CONVERSATION_ID))
            .andExpect(jsonPath("$[0].customerId").value("C10001"))
            .andExpect(jsonPath("$[0].customerName").value("北京朝阳门店"))
            .andExpect(jsonPath("$[0].customerTier").value("ENTERPRISE"))
            .andExpect(jsonPath("$[0].channel").value("WECHAT"))
            .andExpect(jsonPath("$[0].status").value("WAITING"))
            .andExpect(jsonPath("$[0].priority").value("URGENT"))
            .andExpect(jsonPath("$[0].subject").value("高价值订单取消"))
            .andExpect(jsonPath("$[0].intent").value("REFUND"))
            .andExpect(jsonPath("$[0].orderNo").value("SO202606210004"))
            .andExpect(jsonPath("$[0].queueName").value("售后二线"))
            .andExpect(jsonPath("$[0].tags", contains("企业客户", "退款")))
            .andExpect(jsonPath("$[0].unreadCount").value(2))
            .andExpect(jsonPath("$[0].lastMessagePreview").value("这个订单还能取消吗？"))
            .andExpect(jsonPath("$[0].waitingMinutes").value(20));

        verify(conversationService).search(
            ConversationStatus.WAITING,
            ConversationChannel.WECHAT,
            "王芳",
            "售后二线",
            "退款"
        );
    }

    @Test
    void shouldReturnDetailWithCustomerTagsAndMessages() throws Exception {
        when(conversationService.get(CONVERSATION_ID))
            .thenReturn(detail(ConversationStatus.IN_PROGRESS, "王芳"));

        mockMvc.perform(get("/api/support/conversations/{conversationId}", CONVERSATION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.assignee").value("王芳"))
            .andExpect(jsonPath("$.customer.customerId").value("C10001"))
            .andExpect(jsonPath("$.customer.displayName").value("北京朝阳门店"))
            .andExpect(jsonPath("$.customer.tier").value("ENTERPRISE"))
            .andExpect(jsonPath("$.customer.phoneMasked").value("138****0001"))
            .andExpect(jsonPath("$.tags", contains("企业客户", "退款")))
            .andExpect(jsonPath("$.messages", hasSize(1)))
            .andExpect(jsonPath("$.messages[0].messageId").value("MSG-CUSTOMER-001"))
            .andExpect(jsonPath("$.messages[0].senderType").value("CUSTOMER"))
            .andExpect(jsonPath("$.messages[0].visibility").value("CUSTOMER"))
            .andExpect(jsonPath("$.messages[0].content").value("这个订单还能取消吗？"));

        verify(conversationService).get(CONVERSATION_ID);
    }

    @Test
    void shouldClaimConversationWithOperatorFromRequestBody() throws Exception {
        when(conversationService.claim(CONVERSATION_ID, "王芳"))
            .thenReturn(detail(ConversationStatus.IN_PROGRESS, "王芳"));

        mockMvc.perform(post("/api/support/conversations/{conversationId}/claim", CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operator": "王芳"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.assignee").value("王芳"))
            .andExpect(jsonPath("$.customer.customerId").value("C10001"))
            .andExpect(jsonPath("$.messages", hasSize(1)));

        verify(conversationService).claim(CONVERSATION_ID, "王芳");
    }

    @Test
    void shouldRejectBlankClaimOperatorBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/support/conversations/{conversationId}/claim", CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operator": "  "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("请求参数不完整或格式错误"))
            .andExpect(jsonPath("$.details", hasSize(1)))
            .andExpect(jsonPath("$.details[0]", containsString("operator")));

        verifyNoInteractions(conversationService);
    }

    @Test
    void shouldRejectReplyWithoutContentBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/support/conversations/{conversationId}/reply", CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operator": "王芳",
                      "content": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details[0]", containsString("content")));

        verifyNoInteractions(conversationService);
    }

    private ConversationDetail detail(ConversationStatus status, String assignee) {
        return new ConversationDetail(
            conversation(status, assignee),
            new CustomerProfile("C10001", "北京朝阳门店", CustomerTier.ENTERPRISE, "138****0001"),
            List.of(new ConversationMessage(
                "MSG-CUSTOMER-001",
                CONVERSATION_ID,
                MessageSenderType.CUSTOMER,
                "北京朝阳门店",
                MessageVisibility.CUSTOMER,
                "这个订单还能取消吗？",
                NOW.minusSeconds(20 * 60)
            ))
        );
    }

    private SupportConversation conversation(ConversationStatus status, String assignee) {
        return new SupportConversation(
            CONVERSATION_ID,
            "C10001",
            "北京朝阳门店",
            CustomerTier.ENTERPRISE,
            ConversationChannel.WECHAT,
            status,
            ConversationPriority.URGENT,
            "高价值订单取消",
            "REFUND",
            "SO202606210004",
            "售后二线",
            assignee,
            List.of("企业客户", "退款"),
            status == ConversationStatus.WAITING ? 2 : 0,
            "这个订单还能取消吗？",
            NOW.minusSeconds(20 * 60),
            NOW.minusSeconds(40 * 60),
            NOW.minusSeconds(20 * 60),
            status == ConversationStatus.WAITING ? null : NOW.minusSeconds(15 * 60),
            null,
            ""
        );
    }
}
