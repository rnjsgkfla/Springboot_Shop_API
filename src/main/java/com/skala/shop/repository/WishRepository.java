package com.skala.shop.repository;

import com.skala.shop.entity.Customer;
import com.skala.shop.entity.Product;
import com.skala.shop.entity.Wish;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishRepository extends JpaRepository<Wish, Long> {
    List<Wish> findByCustomer(Customer customer);
    Optional<Wish> findByCustomerAndProduct(Customer customer, Product product);
    boolean existsByProduct(Product product);
    void deleteByCustomer(Customer customer);
}
