package com.skala.shop.ai.controller;

import com.skala.shop.ai.action.AiActionResult;
import com.skala.shop.ai.action.PendingAiAction;
import com.skala.shop.ai.action.PendingAiActionService;
import com.skala.shop.ai.agent.ShoppingAssistantAgent;
import com.skala.shop.ai.dto.AiChatRequest;
import com.skala.shop.ai.dto.AiChatResponse;
import com.skala.shop.ai.dto.AiRecommendationRequest;
import com.skala.shop.ai.dto.AiRecommendationResponse;
import com.skala.shop.ai.knowledge.ShopKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 쇼핑", description = "상품 추천, 상담, 찜과 주문을 지원하는 AI API")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiShopController {

    private final ShoppingAssistantAgent shoppingAssistantAgent;
    private final PendingAiActionService actionService;
    private final ShopKnowledgeService knowledgeService;

    public AiShopController(
            ShoppingAssistantAgent shoppingAssistantAgent,
            PendingAiActionService actionService,
            ShopKnowledgeService knowledgeService
    ) {
        this.shoppingAssistantAgent = shoppingAssistantAgent;
        this.actionService = actionService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/recommendations")
    @Operation(summary = "구조화 상품 추천", description = "자연어 조건을 분석하고 실제 DB 상품만 구조화해 반환합니다.")
    public ResponseEntity<AiRecommendationResponse> recommendations(
            Authentication authentication,
            @Valid @RequestBody AiRecommendationRequest request
    ) {
        return ResponseEntity.ok(shoppingAssistantAgent.recommend(
                request.query(), authentication.getName()));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 쇼핑 대화", description = "개인화 상품 추천, 주문·찜 지원 및 정책 상담을 제공합니다.")
    public ResponseEntity<AiChatResponse> chat(
            Authentication authentication,
            @Valid @RequestBody AiChatRequest request
    ) {
        String clientConversationId = request.conversationId() == null
                || request.conversationId().isBlank() ? "default" : request.conversationId().trim();
        String conversationId = authentication.getName() + ":" + clientConversationId;
        String answer = shoppingAssistantAgent.ask(
                request.message(), conversationId, authentication.getName());
        return ResponseEntity.ok(new AiChatResponse(clientConversationId, answer));
    }

    @PostMapping("/actions/{confirmationToken}/confirm")
    @Operation(summary = "AI 작업 확인", description = "AI가 제안한 찜·주문·취소 작업을 최종 실행합니다.")
    public ResponseEntity<AiActionResult> confirm(
            Authentication authentication,
            @PathVariable String confirmationToken
    ) {
        return ResponseEntity.ok(actionService.confirm(
                authentication.getName(), confirmationToken));
    }

    @GetMapping("/actions")
    @Operation(summary = "AI 대기 작업 조회", description = "현재 사용자가 확인할 수 있는 AI 제안 작업을 조회합니다.")
    public ResponseEntity<List<PendingAiAction>> pendingActions(Authentication authentication) {
        return ResponseEntity.ok(actionService.findPending(authentication.getName()));
    }

    @PostMapping("/knowledge/reindex")
    @Operation(summary = "AI 지식 색인", description = "상품과 쇼핑몰 정책을 pgvector에 임베딩합니다.")
    public ResponseEntity<Map<String, Integer>> reindex() {
        return ResponseEntity.ok(Map.of("indexedDocuments", knowledgeService.index()));
    }
}
