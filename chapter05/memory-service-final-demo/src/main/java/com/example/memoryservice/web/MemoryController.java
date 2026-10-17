package com.example.memoryservice.web;
import com.example.memoryservice.memory.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
/** userId 来自通过认证的 Principal，tenantId 来自服务配置；不接受客户端伪造身份字段。 */
@RestController @RequestMapping("/api/memory")
public class MemoryController {
    private final CustomerSupportMemoryService service;
    private final String tenant;
    public MemoryController(CustomerSupportMemoryService service,Environment env) {
        this.service=service;this.tenant=env.getProperty("MEMORY_TENANT_ID","tenant-1");
    }
    private AuthenticatedUser user(Principal principal) { return new AuthenticatedUser(tenant,principal.getName()); }
    private MemoryScope scope(Principal principal,String conversation) { return new MemoryScope(tenant,principal.getName(),conversation); }
    @PostMapping("/{conversation}/chat")
    public Map<String,String> chat(Principal principal,@PathVariable("conversation") String conversation,@Valid @RequestBody Request body) {
        return Map.of("answer",service.chat(user(principal),scope(principal,conversation),body.message()));
    }
    @GetMapping("/{conversation}")
    public CustomerSupportMemoryService.Snapshot get(Principal principal,@PathVariable("conversation") String conversation) {
        return service.snapshot(user(principal),scope(principal,conversation));
    }
    @DeleteMapping("/{conversation}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearConversation(Principal principal,@PathVariable("conversation") String conversation) {
        service.clearConversation(user(principal),scope(principal,conversation));
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearUser(Principal principal) { service.clearUser(user(principal),scope(principal,"all")); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid() { return Map.of("error","请求参数不合法"); }
    @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String,String> forbidden() { return Map.of("error","无权访问此记忆"); }
    public record Request(@NotBlank @Size(max=4000) String message) {}
}
