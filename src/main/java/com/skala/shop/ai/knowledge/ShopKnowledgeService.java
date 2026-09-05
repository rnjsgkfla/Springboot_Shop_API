package com.skala.shop.ai.knowledge;

import com.skala.shop.entity.Product;
import com.skala.shop.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class ShopKnowledgeService {

    private static final String POLICY_RESOURCE = "classpath:documents/shop-policy.txt";

    private final ResourceLoader resourceLoader;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;

    public ShopKnowledgeService(
            ResourceLoader resourceLoader,
            VectorStore vectorStore,
            ProductRepository productRepository
    ) {
        this.resourceLoader = resourceLoader;
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
    }

    public int index() {
        List<Document> documents = new ArrayList<>();
        List<Document> policyDocuments = new TextReader(
                resourceLoader.getResource(POLICY_RESOURCE)).get();
        List<Document> policyChunks = TokenTextSplitter.builder()
                .withChunkSize(200)
                .withMinChunkSizeChars(50)
                .build()
                .apply(policyDocuments);
        for (int index = 0; index < policyChunks.size(); index++) {
            Document chunk = policyChunks.get(index);
            documents.add(document(
                    "policy:" + index,
                    chunk.getText(),
                    Map.of("type", "policy", "source", "shop-policy.txt")
            ));
        }
        documents.addAll(productRepository.findAll().stream()
                .map(this::toDocument)
                .toList());
        vectorStore.add(documents);
        return documents.size();
    }

    private Document toDocument(Product product) {
        String text = """
                상품 ID: %d
                상품명: %s
                설명: %s
                카테고리: %s
                브랜드: %s
                태그: %s
                """.formatted(
                product.getId(), product.getProductName(), product.getDescription(),
                product.getCategory(), product.getBrand(), product.getTags());
        return document("product:" + product.getId(), text, Map.of(
                "type", "product",
                "productId", product.getId()
        ));
    }

    private Document document(String sourceId, String text, Map<String, Object> metadata) {
        String id = UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8)).toString();
        return new Document(id, text, metadata);
    }
}
