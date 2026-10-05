package com.example.aicustomerservice.entity;

/**
 * 客户进入客服系统的渠道。
 *
 * 渠道是会话的业务属性，前端可以据此展示不同图标，运营看板也可以按渠道统计。
 */
public enum ConversationChannel {
    WEB,
    WECHAT,
    APP,
    EMAIL,
    HOTLINE
}
