package com.skala.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 쇼핑몰 고객 정보를 저장하는 엔티티입니다.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Column(name = "password", nullable = false, length = 100)
    private String customerPassword;

    @Column(name = "point", nullable = false)
    private Double customerPoint;

    public Customer(String customerId, Double customerPoint) {
        this.customerId = customerId;
        this.customerPoint = customerPoint;
    }
}
