package org.example.eshop.repository;

import org.example.eshop.entity.OrderDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @EntityGraph(attributePaths = "product")
    List<OrderDetail> findByOrderIdIn(Collection<Long> orderIds);
}
