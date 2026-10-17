package com.example.memoryservice.memory;
/** 只能由认证层构造并传给服务；不能直接从请求 JSON 反序列化为可信身份。 */
public record AuthenticatedUser(String tenantId,String userId) {}
