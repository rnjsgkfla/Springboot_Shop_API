package com.skala.shop.ai.agent;

import com.skala.shop.ai.dto.AiRecommendationResponse;
import com.skala.shop.ai.dto.AiRecommendationResponse.RecommendedProduct;
import com.skala.shop.ai.tool.ShopActionTools;
import com.skala.shop.ai.tool.ShopReadTools;
import com.skala.shop.ai.tool.ShopToolContext;
import com.skala.shop.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ShoppingAssistantAgent {

    private final ChatClient chatClient;
    private final ProductRepository productRepository;

    public ShoppingAssistantAgent(
            ChatModel chatModel,
            ChatMemory chatMemory,
            ShopReadTools readTools,
            ShopActionTools actionTools,
            ProductRepository productRepository
    ) {
        this.productRepository = productRepository;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 AI SHOPPING의 쇼핑 컨시어지입니다.
                        상품 추천, 고객의 포인트·구매·찜 조회, 배송·교환·환불 정책 안내를 담당합니다.
                        가격, 재고, 포인트, 구매 내역과 정책을 추측하지 말고 필요한 도구로 확인하세요.
                        상품 추천에는 실제 상품 ID와 현재 가격, 재고를 사용하고 이유를 짧고 구체적으로 설명하세요.
                        정책 질문에는 정책 검색 도구로 찾은 내용만 사용하고, 근거가 없으면 상담원 확인이 필요하다고 안내하세요.
                        찜·주문·취소 요청에는 상태를 직접 변경하지 않는 확인 요청 도구만 사용하세요.
                        확인 토큰, 만료 시각과 확인 문구를 전달하고 사용자가 승인해야 실행된다고 안내하세요.
                        """)
                .defaultTools(readTools, actionTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    public String ask(String request, String conversationId, String customerId) {
        return chatClient.prompt()
                .user(request)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(ShopToolContext.CUSTOMER_ID, customerId))
                .call()
                .content();
    }

    public AiRecommendationResponse recommend(String request, String customerId) {
        RecommendationDraft draft = chatClient.prompt()
                .user(request + "\n상품 검색 도구로 확인한 상품 중 조건에 가장 잘 맞는 상품을 최대 3개 추천하세요.")
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, customerId + ":recommendations"))
                .toolContext(Map.of(ShopToolContext.CUSTOMER_ID, customerId))
                .call()
                .entity(RecommendationDraft.class);
        if (draft == null || draft.products() == null) {
            return new AiRecommendationResponse(List.of());
        }

        List<RecommendedProduct> products = draft.products().stream()
                .flatMap(item -> productRepository.findById(item.productId()).stream()
                        .filter(product -> product.getStockQuantity() > 0)
                        .map(product -> new RecommendedProduct(
                                product.getId(),
                                product.getProductName(),
                                product.getProductPrice(),
                                product.getStockQuantity(),
                                item.reason()
                        )))
                .limit(3)
                .toList();
        return new AiRecommendationResponse(products);
    }

    private record RecommendationDraft(List<RecommendationItem> products) {
    }

    private record RecommendationItem(Long productId, String reason) {
    }
}
