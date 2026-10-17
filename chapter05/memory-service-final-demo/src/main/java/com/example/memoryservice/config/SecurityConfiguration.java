package com.example.memoryservice.config;
import java.time.*;
import com.example.memoryservice.memory.MemoryRetentionPolicy;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
/** 本机 HTTP Basic 演示，凭据来自运行环境；生产替换为现有登录体系和 HTTPS。 */
@Configuration
public class SecurityConfiguration {
    @Bean public SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.anyRequest().authenticated()).httpBasic(b->{}).build();
    }
    @Bean public UserDetailsService users(Environment env) {
        var encoder=new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername("user-a").password(encoder.encode(required(env,"MEMORY_USER_A_PASSWORD"))).roles("USER").build(),
                User.withUsername("user-b").password(encoder.encode(required(env,"MEMORY_USER_B_PASSWORD"))).roles("USER").build());
    }
    @Bean public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public MemoryRetentionPolicy retention(Clock clock) { return new MemoryRetentionPolicy(Duration.ofDays(30),clock); }
    private String required(Environment env,String name) {
        String value=env.getProperty(name);
        if(value==null||value.isBlank()) throw new IllegalArgumentException("请设置 "+name);
        return value;
    }
}
