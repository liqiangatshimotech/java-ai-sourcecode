package com.example.texttosqlagent.agent;

import com.example.texttosqlagent.sql.QueryResult;
import java.util.List;

/**
 * 结果解说器：把查询结果集翻译成运营同学看得懂的中文结论。
 *
 * 独立成接口的原因和 SqlGenerationModel 一样：
 * 真实系统可以换成「让大模型基于结果写结论」的实现，
 * 本项目用确定性的模板实现，保证离线和测试环境下输出稳定。
 */
public interface AnswerNarrator {

    /**
     * @param question 用户原始问题
     * @param result   只读执行器返回的结果集
     * @return 中文解说文本
     */
    String narrate(String question, QueryResult result);
}
