package com.skala.shop.repository;

import com.skala.shop.entity.Customer;
import com.skala.shop.entity.OrderItem;
import com.skala.shop.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerProductRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByCustomerCustomerId(String customerId);

    Optional<OrderItem> findByCustomerAndProduct(
            Customer customer,
            Product product
    );

    boolean existsByProduct(Product product);
}
