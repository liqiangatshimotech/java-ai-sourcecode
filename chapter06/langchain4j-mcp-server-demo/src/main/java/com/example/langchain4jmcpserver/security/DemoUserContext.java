package com.example.langchain4jmcpserver.security;

import java.util.Set;

public final class DemoUserContext {

    private final CurrentUser currentUser;

    public DemoUserContext(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public static DemoUserContext supportAgent() {
        return new DemoUserContext(new CurrentUser("u-1001", "acme", Set.of("support_agent")));
    }

    public CurrentUser currentUser() {
        return currentUser;
    }
}
