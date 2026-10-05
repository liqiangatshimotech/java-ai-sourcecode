package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.config.CourseModelFactory;
import com.example.agentscopeframework.config.CourseModelSettings;

/**
 * 知识点 13.1：模型配置。
 *
 * <p>这个例子只演示如何把课程统一配置转换成 AgentScope 的 Model。没有 API Key 时不会创建远程模型，避免第一次运行就因为密钥缺失失败。
 */
public class ModelConfigurationKnowledgeDemo {

    public static void main(String[] args) {
        CourseModelSettings settings = CourseModelSettings.fromEnvironment();
        System.out.println("当前模型配置：" + settings.summary());

        if (!settings.hasUsableCredential()) {
            System.out.println("没有检测到模型凭证，只展示配置解析结果。");
            return;
        }

        var model = CourseModelFactory.create(settings);
        System.out.println("已创建 AgentScope Model：" + model.getClass().getSimpleName());
    }
}
