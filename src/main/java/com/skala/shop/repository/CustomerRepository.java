package com.skala.shop.repository;

import com.skala.shop.entity.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, String> {
    List<Customer> findByReferrer(Customer referrer);
}
