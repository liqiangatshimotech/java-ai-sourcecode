package com.example.enterpriseopsagent.incident;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(String incidentId) {
        super("排障事件不存在：" + incidentId);
    }
}
