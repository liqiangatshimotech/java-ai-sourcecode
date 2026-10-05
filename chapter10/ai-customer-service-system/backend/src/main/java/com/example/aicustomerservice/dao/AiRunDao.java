package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.AiRunAudit;
import java.util.List;
import java.util.Optional;

/** AI Run 审计存储边界。 */
public interface AiRunDao {

    AiRunAudit save(AiRunAudit run);

    Optional<AiRunAudit> findByRequestId(String requestId);

    List<AiRunAudit> search(String conversationId, Boolean handoff, int limit);
}
