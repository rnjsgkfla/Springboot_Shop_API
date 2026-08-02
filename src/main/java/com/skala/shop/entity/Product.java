package com.skala.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
//lombok으로 빈 생성자, setter & getter 자동생성.
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String productName;

    @Column(name = "price", nullable = false)
    private Double productPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    public Product(String productName, Double productPrice) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.stockQuantity = 0;
    }

    public void decreaseStock(int quantity) {
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
