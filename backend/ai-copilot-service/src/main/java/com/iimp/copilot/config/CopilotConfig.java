package com.iimp.copilot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CopilotConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an expert SRE copilot assistant for the Intelligent Incident Management Platform (IIMP).
                You help on-call engineers investigate incidents, diagnose root causes, interpret alerts,
                and suggest remediation steps. Be concise, precise, and action-oriented.
                """)
            .build();
    }
}
