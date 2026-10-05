package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.dto.ConversationAssignRequest;
import com.example.aicustomerservice.dto.ConversationClaimRequest;
import com.example.aicustomerservice.dto.ConversationDashboardResponse;
import com.example.aicustomerservice.dto.ConversationDetailResponse;
import com.example.aicustomerservice.dto.ConversationNoteRequest;
import com.example.aicustomerservice.dto.ConversationReplyRequest;
import com.example.aicustomerservice.dto.ConversationResolveRequest;
import com.example.aicustomerservice.dto.ConversationSummaryResponse;
import com.example.aicustomerservice.dto.ConversationWaitCustomerRequest;
import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.service.ConversationService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服会话收件箱 API。
 *
 * 所有写接口都返回最新的完整会话详情，前端操作完成后可以直接替换当前页面状态，无需再发一次 GET。
 */
@RestController
@RequestMapping("/api/support/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final Clock clock;

    public ConversationController(ConversationService conversationService, Clock clock) {
        this.conversationService = conversationService;
        this.clock = clock;
    }

    /**
     * 查询收件箱。所有条件可选，keyword 会匹配会话号、客户、主题、订单号、最近消息和标签。
     */
    @GetMapping
    public List<ConversationSummaryResponse> search(
        @RequestParam(required = false) ConversationStatus status,
        @RequestParam(required = false) ConversationChannel channel,
        @RequestParam(required = false) String assignee,
        @RequestParam(name = "queue", required = false) String queue,
        @RequestParam(name = "queueName", required = false) String queueName,
        @RequestParam(required = false) String keyword
    ) {
        Instant now = clock.instant();
        String selectedQueue = queue == null || queue.isBlank() ? queueName : queue;
        return conversationService.search(status, channel, assignee, selectedQueue, keyword).stream()
            .map(item -> ConversationSummaryResponse.from(item, now))
            .toList();
    }

    @GetMapping("/dashboard")
    public ConversationDashboardResponse dashboard() {
        return ConversationDashboardResponse.from(conversationService.dashboard());
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse get(@PathVariable String conversationId) {
        return ConversationDetailResponse.from(conversationService.get(conversationId));
    }

    @PostMapping("/{conversationId}/claim")
    public ConversationDetailResponse claim(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationClaimRequest request
    ) {
        return ConversationDetailResponse.from(conversationService.claim(conversationId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator())));
    }

    @PostMapping("/{conversationId}/assign")
    public ConversationDetailResponse assign(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationAssignRequest request
    ) {
        return ConversationDetailResponse.from(
            conversationService.assign(conversationId, request.owner(), com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()))
        );
    }

    @PostMapping("/{conversationId}/reply")
    public ConversationDetailResponse reply(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationReplyRequest request
    ) {
        return ConversationDetailResponse.from(
            conversationService.reply(conversationId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.content())
        );
    }

    @PostMapping("/{conversationId}/notes")
    public ConversationDetailResponse addNote(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationNoteRequest request
    ) {
        return ConversationDetailResponse.from(
            conversationService.addNote(conversationId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.content())
        );
    }

    @PostMapping("/{conversationId}/wait-customer")
    public ConversationDetailResponse waitForCustomer(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationWaitCustomerRequest request
    ) {
        return ConversationDetailResponse.from(
            conversationService.waitForCustomer(conversationId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.reason())
        );
    }

    @PostMapping("/{conversationId}/resolve")
    public ConversationDetailResponse resolve(
        @PathVariable String conversationId,
        @Valid @RequestBody ConversationResolveRequest request
    ) {
        return ConversationDetailResponse.from(
            conversationService.resolve(conversationId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.resolution())
        );
    }
}
