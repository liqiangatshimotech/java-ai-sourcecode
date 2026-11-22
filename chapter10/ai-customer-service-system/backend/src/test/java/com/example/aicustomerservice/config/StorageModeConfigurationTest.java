package com.example.aicustomerservice.config;

import com.example.aicustomerservice.dao.ConversationDao;
import com.example.aicustomerservice.dao.ConversationQuery;
import com.example.aicustomerservice.dao.AiRunDao;
import com.example.aicustomerservice.dao.InMemoryAiRunDao;
import com.example.aicustomerservice.dao.InMemoryConversationDao;
import com.example.aicustomerservice.dao.InMemoryKnowledgeDao;
import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.dao.KnowledgeDao;
import com.example.aicustomerservice.dao.OrderDao;
import com.example.aicustomerservice.dao.OrderQuery;
import com.example.aicustomerservice.dao.TicketDao;
import com.example.aicustomerservice.service.RetrievalService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止 README 中的内存启动方式再次与代码漂移。
 *
 * 该测试加载完整 Spring 容器但不启动 Web Server，也不要求本机 PostgreSQL 可用。
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "support.storage.type=memory",
        "support.ai.provider=ollama"
    }
)
class StorageModeConfigurationTest {

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private AiRunDao aiRunDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private TicketDao ticketDao;

    @Autowired
    private KnowledgeDao knowledgeDao;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldUseAllInMemoryRepositoriesAndKeepKnowledgeSearchAvailable() {
        // 会话、AI Run、订单、工单和知识必须同时切到内存实现，
        // 不能出现一半用内存、一半仍然连接 PostgreSQL 的混合状态。
        assertThat(conversationDao).isInstanceOf(InMemoryConversationDao.class);
        assertThat(aiRunDao).isInstanceOf(InMemoryAiRunDao.class);
        assertThat(orderDao).isInstanceOf(InMemoryOrderDao.class);
        assertThat(ticketDao).isInstanceOf(InMemoryTicketDao.class);
        assertThat(knowledgeDao).isInstanceOf(InMemoryKnowledgeDao.class);

        // memory 模式不应该创建 DataSource；没有 Docker 时健康检查也不应因为数据库而失败。
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();

        // 切换存储后，核心页面依赖的数据与检索能力仍然要可用。
        assertThat(conversationDao.search(new ConversationQuery(
            null,
            null,
            null,
            null,
            null
        ))).isNotEmpty();
        assertThat(aiRunDao.search(null, null, 20)).isNotEmpty();
        assertThat(orderDao.search(new OrderQuery(null, null, null))).isNotEmpty();
        assertThat(knowledgeDao.findPublishedDocuments()).isNotEmpty();
        assertThat(retrievalService.search("退款", 3)).isNotEmpty();
    }
}
