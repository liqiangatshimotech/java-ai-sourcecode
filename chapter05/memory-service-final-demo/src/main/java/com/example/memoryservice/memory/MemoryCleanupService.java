package com.example.memoryservice.memory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
/** 定时任务做物理清理，召回处的 TTL 过滤不会替代这个步骤。 */
@Service
public class MemoryCleanupService {
    private final CustomerSupportMemoryService service;
    public MemoryCleanupService(CustomerSupportMemoryService service) { this.service=service; }
    @Scheduled(fixedDelayString="${memory.cleanup-ms:60000}")
    public void cleanExpired() { service.purgeExpired(); }
}
