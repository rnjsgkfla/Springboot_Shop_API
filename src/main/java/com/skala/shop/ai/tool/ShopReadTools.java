package com.skala.shop.ai.tool;

import com.skala.shop.ai.dto.ProductSummary;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.dto.WishResponse;
import com.skala.shop.service.CustomerService;
import com.skala.shop.service.ProductService;
import com.skala.shop.service.WishService;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ShopReadTools {

    private final ProductService productService;
    private final CustomerService customerService;
    private final WishService wishService;
    private final VectorStore vectorStore;

    public ShopReadTools(
            ProductService productService,
            CustomerService customerService,
            WishService wishService,
            VectorStore vectorStore
    ) {
        this.productService = productService;
        this.customerService = customerService;
        this.wishService = wishService;
        this.vectorStore = vectorStore;
    }

    @Tool(description = "상품명, 설명, 브랜드, 태그, 가격, 카테고리 조건으로 현재 판매 상품과 실시간 재고를 검색합니다.")
    public List<ProductSummary> searchProducts(
            @ToolParam(description = "상품을 찾을 키워드", required = false) String query,
            @ToolParam(description = "최고 가격. 제한이 없으면 null", required = false) Double maxPrice,
            @ToolParam(description = "카테고리. 제한이 없으면 null", required = false) String category
    ) {
        return productService.search(query, maxPrice, category).stream()
                .map(ProductSummary::from)
                .toList();
    }

    @Tool(description = "현재 로그인한 고객의 포인트와 구매 상품을 조회합니다.")
    public OrderListDto getMyAccount(ToolContext toolContext) {
        return customerService.findById(customerId(toolContext));
    }

    @Tool(description = "현재 로그인한 고객의 찜 목록을 조회합니다.")
    public List<WishResponse> getMyWishes(ToolContext toolContext) {
        return wishService.findAll(customerId(toolContext));
    }

    @Tool(description = "배송, 교환, 환불 등 쇼핑몰 정책 문서에서 질문과 관련된 근거를 검색합니다.")
    public List<String> searchPolicies(
            @ToolParam(description = "정책 문서에서 찾을 질문") String query
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(4)
                .filterExpression("type == 'policy'")
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(Document::getText)
                .toList();
    }

    private String customerId(ToolContext toolContext) {
        return (String) toolContext.getContext().get(ShopToolContext.CUSTOMER_ID);
    }
}
