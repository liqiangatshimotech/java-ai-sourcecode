package com.example.aicustomerservice.config;

import com.example.aicustomerservice.ai.ChatModelClient;
import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.ai.OpenAiCompatibleChatModelClient;
import com.example.aicustomerservice.dao.AiRunDao;
import com.example.aicustomerservice.dao.ConversationDao;
import com.example.aicustomerservice.dao.CustomerDao;
import com.example.aicustomerservice.dao.InMemoryConversationDao;
import com.example.aicustomerservice.dao.InMemoryCustomerDao;
import com.example.aicustomerservice.dao.InMemoryAiRunDao;
import com.example.aicustomerservice.dao.InMemoryKnowledgeDao;
import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.dao.KnowledgeDao;
import com.example.aicustomerservice.dao.KnowledgeVectorDao;
import com.example.aicustomerservice.dao.NoOpKnowledgeVectorDao;
import com.example.aicustomerservice.dao.OrderDao;
import com.example.aicustomerservice.dao.PgVectorKnowledgeVectorDao;
import com.example.aicustomerservice.dao.PostgreSqlConversationDao;
import com.example.aicustomerservice.dao.PostgreSqlAiRunDao;
import com.example.aicustomerservice.dao.PostgreSqlKnowledgeDao;
import com.example.aicustomerservice.dao.PostgreSqlOrderDao;
import com.example.aicustomerservice.dao.PostgreSqlTicketDao;
import com.example.aicustomerservice.dao.TicketDao;
import com.example.aicustomerservice.service.ConversationMemoryService;
import com.example.aicustomerservice.service.ConversationService;
import com.example.aicustomerservice.service.AiRunService;
import com.example.aicustomerservice.service.CustomerSupportService;
import com.example.aicustomerservice.service.CustomerToolService;
import com.example.aicustomerservice.service.EmbeddingService;
import com.example.aicustomerservice.service.EnterpriseProblemCatalog;
import com.example.aicustomerservice.service.HashingEmbeddingService;
import com.example.aicustomerservice.service.HotQuestionCache;
import com.example.aicustomerservice.service.InMemoryCustomerToolService;
import com.example.aicustomerservice.service.InMemoryHotQuestionCache;
import com.example.aicustomerservice.service.IntentRecognitionService;
import com.example.aicustomerservice.service.KnowledgeLifecycleService;
import com.example.aicustomerservice.service.OrderService;
import com.example.aicustomerservice.service.ProductionGuardrailService;
import com.example.aicustomerservice.service.QueuedModelExecutor;
import com.example.aicustomerservice.service.RetrievalService;
import com.example.aicustomerservice.service.SemanticChunker;
import com.example.aicustomerservice.service.TicketIdGenerator;
import com.example.aicustomerservice.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class CustomerServiceConfiguration {

    /**
     * PostgreSQL 模式才创建连接池。memory 模式没有 DataSource Bean，因此不会尝试连接数据库，
     * Actuator 也不会错误地注册数据库健康检查。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public HikariDataSource supportDataSource(
        @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName,
        @Value("${spring.datasource.url}") String jdbcUrl,
        @Value("${spring.datasource.username}") String username,
        @Value("${spring.datasource.password}") String password,
        @Value("${spring.datasource.hikari.pool-name:ai-customer-service-pool}") String poolName,
        @Value("${spring.datasource.hikari.maximum-pool-size:5}") int maximumPoolSize,
        @Value("${spring.datasource.hikari.connection-timeout:3000}") long connectionTimeout
    ) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ModelSettings modelSettings(@Value("${support.ai.provider:deepseek}") String providerName) {
        return ModelSettings.fromProviderName(providerName);
    }

    @Bean
    public ChatModelClient chatModelClient(ModelSettings modelSettings) {
        return new OpenAiCompatibleChatModelClient(modelSettings);
    }

    @Bean
    public QueuedModelExecutor queuedModelExecutor(ChatModelClient chatModelClient) {
        return new QueuedModelExecutor(chatModelClient, 16);
    }

    @Bean
    public HotQuestionCache hotQuestionCache() {
        return new InMemoryHotQuestionCache();
    }

    @Bean
    public ProductionGuardrailService productionGuardrailService() {
        return new ProductionGuardrailService();
    }

    @Bean
    public EnterpriseProblemCatalog enterpriseProblemCatalog() {
        return new EnterpriseProblemCatalog();
    }

    @Bean
    public CustomerDao customerDao() {
        return new InMemoryCustomerDao();
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public OrderDao postgreSqlOrderDao(JdbcTemplate jdbcTemplate) {
        return new PostgreSqlOrderDao(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public OrderDao inMemoryOrderDao(Clock clock) {
        return new InMemoryOrderDao(clock);
    }

    @Bean
    public OrderService orderService(OrderDao orderDao, Clock clock) {
        return new OrderService(orderDao, clock);
    }

    @Bean
    public CustomerToolService customerToolService(OrderDao orderDao) {
        return new InMemoryCustomerToolService(orderDao);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public KnowledgeDao postgreSqlKnowledgeDao(JdbcTemplate jdbcTemplate, Clock clock) {
        return new PostgreSqlKnowledgeDao(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public KnowledgeDao inMemoryKnowledgeDao(Clock clock) {
        return new InMemoryKnowledgeDao(clock);
    }

    @Bean
    public SemanticChunker semanticChunker() {
        return new SemanticChunker();
    }

    @Bean
    public EmbeddingService embeddingService(
        @Value("${support.rag.embedding.dimensions:64}") int dimensions,
        @Value("${support.rag.embedding.mode:hashing}") String mode,
        @Value("${support.rag.embedding.base-url:}") String baseUrl,
        @Value("${support.rag.embedding.api-key:}") String apiKey,
        @Value("${support.rag.embedding.model:}") String model) {
        if ("hashing".equals(mode)) return new HashingEmbeddingService(dimensions);
        if (!"remote".equals(mode)) throw new IllegalArgumentException("Embedding mode 只能是 hashing 或 remote");
        return new com.example.aicustomerservice.service.OpenAiCompatibleEmbeddingService(baseUrl, apiKey, model, dimensions);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public KnowledgeVectorDao pgVectorKnowledgeVectorDao(
        JdbcTemplate jdbcTemplate,
        EmbeddingService embeddingService
    ) {
        return new PgVectorKnowledgeVectorDao(jdbcTemplate, embeddingService);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public KnowledgeVectorDao noOpKnowledgeVectorDao() {
        return new NoOpKnowledgeVectorDao();
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public RetrievalService pgVectorRetrievalService(KnowledgeVectorDao vectorDao) {
        return new RetrievalService(vectorDao);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public RetrievalService inMemoryRetrievalService(KnowledgeDao knowledgeDao, SemanticChunker semanticChunker) {
        return new RetrievalService(knowledgeDao, semanticChunker);
    }

    @Bean
    public KnowledgeLifecycleService knowledgeLifecycleService(
        KnowledgeDao knowledgeDao,
        SemanticChunker semanticChunker,
        Clock clock,
        KnowledgeVectorDao vectorDao
    ) {
        return new KnowledgeLifecycleService(knowledgeDao, semanticChunker, clock, vectorDao);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public ApplicationRunner syncPublishedKnowledgeVectors(
        KnowledgeDao knowledgeDao,
        SemanticChunker semanticChunker,
        KnowledgeVectorDao vectorDao
    ) {
        return args -> {
            for (var document : knowledgeDao.findPublishedDocuments()) {
                var chunks = semanticChunker.split(document);
                knowledgeDao.replaceChunks(document, chunks);
                vectorDao.syncDocument(document, chunks);
                knowledgeDao.markChunksSynced(
                    document.documentId(),
                    vectorDao.embeddingModelName(),
                    vectorDao.embeddingDimensions()
                );
            }
        };
    }

    @Bean
    public IntentRecognitionService intentRecognitionService() {
        return new IntentRecognitionService();
    }

    @Bean
    public ConversationMemoryService conversationMemoryService() {
        return new ConversationMemoryService();
    }

    @Bean
    public TicketIdGenerator ticketIdGenerator(Clock clock) {
        return new TicketIdGenerator(clock);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public TicketDao postgreSqlTicketDao(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        return new PostgreSqlTicketDao(jdbcTemplate, transactionTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public TicketDao inMemoryTicketDao(Clock clock) {
        return new InMemoryTicketDao(clock);
    }

    @Bean
    public TicketService ticketService(TicketDao ticketDao, TicketIdGenerator ticketIdGenerator, Clock clock) {
        return new TicketService(ticketDao, ticketIdGenerator, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public ConversationDao postgreSqlConversationDao(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate
    ) {
        return new PostgreSqlConversationDao(jdbcTemplate, transactionTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public ConversationDao inMemoryConversationDao(Clock clock) {
        return new InMemoryConversationDao(clock);
    }

    @Bean
    public ConversationService conversationService(
        ConversationDao conversationDao,
        CustomerDao customerDao,
        Clock clock
    ) {
        return new ConversationService(conversationDao, customerDao, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "postgres", matchIfMissing = true)
    public AiRunDao postgreSqlAiRunDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new PostgreSqlAiRunDao(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "support.storage.type", havingValue = "memory")
    public AiRunDao inMemoryAiRunDao(Clock clock) {
        return new InMemoryAiRunDao(clock);
    }

    @Bean
    public AiRunService aiRunService(AiRunDao aiRunDao) {
        return new AiRunService(aiRunDao);
    }

    @Bean
    public CustomerSupportService customerSupportService(
        CustomerDao customerDao,
        IntentRecognitionService intentRecognitionService,
        RetrievalService retrievalService,
        CustomerToolService toolService,
        QueuedModelExecutor queuedModelExecutor,
        TicketService ticketService,
        ConversationMemoryService memoryService,
        ProductionGuardrailService guardrailService,
        HotQuestionCache hotQuestionCache
    ) {
        return new CustomerSupportService(
            customerDao,
            intentRecognitionService,
            retrievalService,
            toolService,
            queuedModelExecutor,
            ticketService,
            memoryService,
            guardrailService,
            hotQuestionCache
        );
    }
}
