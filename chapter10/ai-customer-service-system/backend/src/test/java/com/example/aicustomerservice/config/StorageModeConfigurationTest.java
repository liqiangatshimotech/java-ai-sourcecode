package com.example.aicustomerservice.config;

import com.example.aicustomerservice.dao.ConversationDao;
import com.example.aicustomerservice.dao.AiRunDao;
import com.example.aicustomerservice.dao.InMemoryAiRunDao;
import com.example.aicustomerservice.dao.InMemoryConversationDao;
import com.example.aicustomerservice.dao.InMemoryKnowledgeDao;
import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.dao.KnowledgeDao;
import com.example.aicustomerservice.dao.OrderDao;
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
        assertThat(conversationDao).isInstanceOf(InMemoryConversationDao.class);
        assertThat(aiRunDao).isInstanceOf(InMemoryAiRunDao.class);
        assertThat(orderDao).isInstanceOf(InMemoryOrderDao.class);
        assertThat(ticketDao).isInstanceOf(InMemoryTicketDao.class);
        assertThat(knowledgeDao).isInstanceOf(InMemoryKnowledgeDao.class);
        assertThat(applicationContext.getBeansOfType(DataSource.class)).isEmpty();

        assertThat(conversationDao.search(new com.example.aicustomerservice.dao.ConversationQuery(
            null, null, null, null, null
        ))).isNotEmpty();
        assertThat(aiRunDao.search(null, null, 20)).isNotEmpty();
        assertThat(orderDao.search(new com.example.aicustomerservice.dao.OrderQuery(null, null, null))).isNotEmpty();
        assertThat(knowledgeDao.findPublishedDocuments()).isNotEmpty();
        assertThat(retrievalService.search("退款", 3)).isNotEmpty();
    }
}
