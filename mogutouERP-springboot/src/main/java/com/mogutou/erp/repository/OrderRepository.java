package com.mogutou.erp.repository;

import com.mogutou.erp.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderType(String orderType);
    
    Page<Order> findByOrderType(String orderType, Pageable pageable);
}