package com.skala.shop.ai.tool;

import com.skala.shop.ai.action.AiActionType;
import com.skala.shop.ai.action.PendingAiAction;
import com.skala.shop.ai.action.PendingAiActionService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ShopActionTools {

    private final PendingAiActionService actionService;

    public ShopActionTools(PendingAiActionService actionService) {
        this.actionService = actionService;
    }

    @Tool(description = "상품을 찜 목록에 추가하기 위한 확인 요청을 만듭니다. 이 도구 자체는 찜을 변경하지 않습니다.")
    public PendingAiAction proposeAddWish(
            @ToolParam(description = "상품 ID") Long productId,
            ToolContext toolContext
    ) {
        return propose(AiActionType.ADD_WISH, productId, 1, toolContext);
    }

    @Tool(description = "상품을 찜 목록에서 삭제하기 위한 확인 요청을 만듭니다. 이 도구 자체는 찜을 변경하지 않습니다.")
    public PendingAiAction proposeRemoveWish(
            @ToolParam(description = "상품 ID") Long productId,
            ToolContext toolContext
    ) {
        return propose(AiActionType.REMOVE_WISH, productId, 1, toolContext);
    }

    @Tool(description = "상품 주문을 위한 확인 요청을 만듭니다. 이 도구 자체는 주문하지 않습니다.")
    public PendingAiAction proposeOrder(
            @ToolParam(description = "상품 ID") Long productId,
            @ToolParam(description = "주문 수량") int quantity,
            ToolContext toolContext
    ) {
        return propose(AiActionType.PLACE_ORDER, productId, quantity, toolContext);
    }

    @Tool(description = "주문 취소를 위한 확인 요청을 만듭니다. 이 도구 자체는 주문을 취소하지 않습니다.")
    public PendingAiAction proposeCancelOrder(
            @ToolParam(description = "상품 ID") Long productId,
            @ToolParam(description = "취소 수량") int quantity,
            ToolContext toolContext
    ) {
        return propose(AiActionType.CANCEL_ORDER, productId, quantity, toolContext);
    }

    private PendingAiAction propose(
            AiActionType type,
            Long productId,
            int quantity,
            ToolContext toolContext
    ) {
        String customerId = (String) toolContext.getContext().get(ShopToolContext.CUSTOMER_ID);
        return actionService.propose(customerId, type, productId, quantity);
    }
}
