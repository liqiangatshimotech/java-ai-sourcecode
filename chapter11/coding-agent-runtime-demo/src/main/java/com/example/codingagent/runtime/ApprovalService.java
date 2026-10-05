package com.example.codingagent.runtime;

import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;

public interface ApprovalService {

    ApprovalDecision requestApproval(CodingTask task, CodeChangeProposal proposal);
}
