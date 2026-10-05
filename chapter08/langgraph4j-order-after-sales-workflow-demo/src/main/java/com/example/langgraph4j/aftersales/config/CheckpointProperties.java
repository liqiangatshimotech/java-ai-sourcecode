package com.example.langgraph4j.aftersales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Memory/MySQL checkpoint 的外部化配置。 */
@ConfigurationProperties(prefix = "aftersales.checkpoint")
public class CheckpointProperties {
    private String type = "memory";
    private String jdbcUrl = "jdbc:mysql://localhost:3306/langgraph4j_demo";
    private String username = "root";
    private String password = "";

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean mysqlEnabled() {
        return "mysql".equalsIgnoreCase(type);
    }
}
