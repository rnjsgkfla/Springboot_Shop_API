package com.skala.shop.ai.action;

import com.skala.shop.dto.OrderRequest;
import com.skala.shop.entity.Product;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.service.CustomerService;
import com.skala.shop.service.ProductService;
import com.skala.shop.service.WishService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class PendingAiActionService {

    private static final Duration VALIDITY = Duration.ofMinutes(10);

    private final ProductService productService;
    private final CustomerService customerService;
    private final WishService wishService;
    private final Map<String, StoredAction> actions = new ConcurrentHashMap<>();

    public PendingAiActionService(
            ProductService productService,
            CustomerService customerService,
            WishService wishService
    ) {
        this.productService = productService;
        this.customerService = customerService;
        this.wishService = wishService;
    }

    public PendingAiAction propose(String customerId, AiActionType type, Long productId, int quantity) {
        Product product = productService.findById(productId);
        if ((type == AiActionType.PLACE_ORDER || type == AiActionType.CANCEL_ORDER) && quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(VALIDITY);
        int effectiveQuantity = type == AiActionType.ADD_WISH || type == AiActionType.REMOVE_WISH
                ? 1 : quantity;
        String message = switch (type) {
            case ADD_WISH -> "'%s' 상품을 찜 목록에 추가할까요?".formatted(product.getProductName());
            case REMOVE_WISH -> "'%s' 상품을 찜 목록에서 삭제할까요?".formatted(product.getProductName());
            case PLACE_ORDER -> "'%s' %d개를 총 %.0f포인트에 주문할까요?"
                    .formatted(product.getProductName(), effectiveQuantity,
                            product.getProductPrice() * effectiveQuantity);
            case CANCEL_ORDER -> "'%s' 주문 %d개를 취소할까요?"
                    .formatted(product.getProductName(), effectiveQuantity);
        };
        actions.put(token, new StoredAction(
                customerId, type, productId, effectiveQuantity, expiresAt, message));
        return new PendingAiAction(token, type, productId, effectiveQuantity, expiresAt, message);
    }

    public List<PendingAiAction> findPending(String customerId) {
        Instant now = Instant.now();
        actions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        return actions.entrySet().stream()
                .filter(entry -> entry.getValue().customerId().equals(customerId))
                .map(entry -> {
                    StoredAction action = entry.getValue();
                    return new PendingAiAction(
                            entry.getKey(), action.type(), action.productId(), action.quantity(),
                            action.expiresAt(), action.confirmationMessage());
                })
                .toList();
    }

    public AiActionResult confirm(String customerId, String token) {
        StoredAction action = actions.get(token);
        if (action == null || !action.customerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (action.expiresAt().isBefore(Instant.now())) {
            actions.remove(token, action);
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!actions.remove(token, action)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Object result = switch (action.type()) {
            case ADD_WISH -> wishService.create(customerId, action.productId());
            case REMOVE_WISH -> {
                wishService.delete(customerId, action.productId());
                yield null;
            }
            case PLACE_ORDER -> customerService.placeOrder(
                    customerId, new OrderRequest(action.productId(), action.quantity()));
            case CANCEL_ORDER -> customerService.cancelOrder(
                    customerId, new OrderRequest(action.productId(), action.quantity()));
        };
        return new AiActionResult(action.type(), "요청이 실행되었습니다.", result);
    }

    private record StoredAction(
            String customerId,
            AiActionType type,
            Long productId,
            int quantity,
            Instant expiresAt,
            String confirmationMessage
    ) {
    }
}
