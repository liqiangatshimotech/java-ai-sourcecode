package com.example.texttosqlagent.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AnalysisController 的 Web 层测试：完整 Spring 上下文 + MockMvc。
 *
 * text2sql.ai.offline=true 钉死离线规则模型——
 * 就算 CI 机器上恰好配了 DEEPSEEK_API_KEY，测试也不会真的调外部 API。
 */
@SpringBootTest(properties = "text2sql.ai.offline=true")
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void askReturnsAnswerForGoldenQuestion() throws Exception {
        mockMvc.perform(post("/api/analysis/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"2026年6月销售额最高的五个商品分类\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ANSWERED"))
            .andExpect(jsonPath("$.rowCount").value(greaterThan(0)))
            .andExpect(jsonPath("$.sql").isNotEmpty())
            .andExpect(jsonPath("$.summary").isNotEmpty())
            .andExpect(jsonPath("$.linkedTables").isArray())
            .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    void blankQuestionGets400() throws Exception {
        // 空问题在参数校验层就该被 400 拦下，不会进入 Agent 链路
        mockMvc.perform(post("/api/analysis/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void missingQuestionGets400() throws Exception {
        mockMvc.perform(post("/api/analysis/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void schemaEndpointExposesTablesWithComments() throws Exception {
        // schema 接口是给调试 Schema Linking 用的：必须能看到 5 张表和中文注释
        mockMvc.perform(get("/api/analysis/schema"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[0].name").isNotEmpty())
            .andExpect(jsonPath("$[0].comment").isNotEmpty())
            .andExpect(jsonPath("$[0].columns[0].comment").isNotEmpty());
    }
}
