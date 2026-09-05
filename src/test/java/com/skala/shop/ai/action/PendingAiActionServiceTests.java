package com.skala.shop.ai.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shop.entity.Product;
import com.skala.shop.service.CustomerService;
import com.skala.shop.service.ProductService;
import com.skala.shop.service.WishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingAiActionServiceTests {

    @Mock
    private ProductService productService;

    @Mock
    private CustomerService customerService;

    @Mock
    private WishService wishService;

    private PendingAiActionService actionService;

    @BeforeEach
    void setUp() {
        actionService = new PendingAiActionService(
                productService, customerService, wishService);
    }

    @Test
    void orderIsExecutedOnlyAfterConfirmation() {
        Product product = new Product("무선 마우스", 15000.0);
        product.setId(1L);
        when(productService.findById(1L)).thenReturn(product);

        PendingAiAction pending = actionService.propose(
                "customer01", AiActionType.PLACE_ORDER, 1L, 2);

        assertThat(pending.confirmationMessage()).contains("30000포인트");
        assertThat(actionService.findPending("customer01")).containsExactly(pending);
        actionService.confirm("customer01", pending.confirmationToken());
        assertThat(actionService.findPending("customer01")).isEmpty();

        verify(customerService).placeOrder(
                "customer01", new com.skala.shop.dto.OrderRequest(1L, 2));
    }

    @Test
    void anotherCustomerCannotConsumeConfirmationToken() {
        Product product = new Product("무선 마우스", 15000.0);
        product.setId(1L);
        when(productService.findById(1L)).thenReturn(product);
        PendingAiAction pending = actionService.propose(
                "customer01", AiActionType.ADD_WISH, 1L, 1);

        assertThatThrownBy(() -> actionService.confirm(
                "customer02", pending.confirmationToken()))
                .isInstanceOf(com.skala.shop.exception.BusinessException.class);

        actionService.confirm("customer01", pending.confirmationToken());
        verify(wishService).create("customer01", 1L);
    }
}
