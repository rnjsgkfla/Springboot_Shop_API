package com.skala.shop.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.skala.shop.ai.agent.ShoppingAssistantAgent;
import com.skala.shop.ai.controller.AiShopController;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "app.ai.enabled=true",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding.text=none",
        "spring.ai.vectorstore.type=none"
})
class AiFeatureContextTests {

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private AiShopController aiShopController;

    @Autowired
    private ShoppingAssistantAgent shoppingAssistantAgent;

    @Test
    void aiComponentsAreWiredWhenFeatureIsEnabled() {
        assertThat(aiShopController).isNotNull();
        assertThat(shoppingAssistantAgent).isNotNull();
    }
}
