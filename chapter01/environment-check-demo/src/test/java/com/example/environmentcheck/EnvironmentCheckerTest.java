package com.example.environmentcheck;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** 配置 Map 与可替换 Maven/探测入口保证这些测试不依赖个人凭据或外部网络。 */
class EnvironmentCheckerTest {
    @Test void defaultProviderIsDeepSeekAndNoImplicitProbe() {
        AtomicInteger calls = new AtomicInteger();
        var checker = new EnvironmentChecker(() -> true, c -> { calls.incrementAndGet(); return CheckResult.pass("MODEL_PROBE", "fixture"); });
        var results = checker.check(Map.of("DEEPSEEK_API_KEY", "test-only-key"),21,false);
        assertEquals(0,calls.get());
        assertEquals(CheckResult.Status.NOT_TESTED,results.getLast().status());
        assertTrue(results.stream().anyMatch(r->r.detail().contains("provider=deepseek")));
        assertFalse(results.toString().contains("test-only-key"));
    }
    @Test void missingKeyBlocksProbeAndReportsFailure() {
        var checker=new EnvironmentChecker(()->true,c->{throw new AssertionError("must not call");});
        var results=checker.check(Map.of(),21,true);
        assertTrue(results.stream().anyMatch(r->r.item().equals("CREDENTIAL")&&r.status()==CheckResult.Status.FAIL));
        assertEquals(CheckResult.Status.NOT_TESTED,results.getLast().status());
    }
    @Test void missingJdkOrMavenIsNotOverallSuccess() {
        var checker=new EnvironmentChecker(()->false,c->{throw new AssertionError("must not call");});
        var results=checker.check(Map.of("AI_PROVIDER","ollama"),17,true);
        assertEquals(2,results.stream().filter(r->r.status()==CheckResult.Status.FAIL).count());
        assertEquals(CheckResult.Status.NOT_TESTED,results.getLast().status());
    }
    @Test void explicitProbeRunsExactlyOnce() {
        AtomicInteger calls=new AtomicInteger();
        var checker=new EnvironmentChecker(()->true,c->{calls.incrementAndGet();return CheckResult.pass("MODEL_PROBE","fixture");});
        checker.check(Map.of("AI_PROVIDER","ollama"),21,true);assertEquals(1,calls.get());
    }
    @Test void chatgptIsOpenAiAliasAndConfigDoesNotExposeKey() {
        var config=ModelConfig.from(Map.of("AI_PROVIDER","chatgpt","OPENAI_API_KEY","secret-for-test"));
        assertEquals("openai",config.provider());assertFalse(config.toString().contains("secret-for-test"));
        assertEquals("https://api.openai.com/v1/chat/completions",config.endpoint().toString());
    }
    @ParameterizedTest @ValueSource(strings={"https://user:password@example.com/v1","https://example.com/v1?key=secret","https://example.com/v1#secret","file:///tmp/model","not a URL","https://example.com:99999/v1"})
    void rejectsInvalidOrCredentialBearingBaseUrl(String base) {
        var error=assertThrows(IllegalArgumentException.class,()->ModelConfig.from(Map.of("AI_BASE_URL",base)));
        assertFalse(error.getMessage().contains(base));
    }
    @Test void rejectsUnknownProviderAsFailedCheck() {
        var checker=new EnvironmentChecker(()->true,c->{throw new AssertionError("must not call");});
        assertTrue(checker.check(Map.of("AI_PROVIDER","unknown"),21,false).stream().anyMatch(r->r.item().equals("CONFIG")&&r.status()==CheckResult.Status.FAIL));
    }
}
