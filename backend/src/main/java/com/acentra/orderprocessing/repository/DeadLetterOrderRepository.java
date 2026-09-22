package com.acentra.orderprocessing.repository;

import com.acentra.orderprocessing.model.DeadLetterOrder;
import com.acentra.orderprocessing.model.DlqStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeadLetterOrderRepository extends JpaRepository<DeadLetterOrder, Long> {
    List<DeadLetterOrder> findAllByOrderByLastAttemptAtDesc(Pageable pageable);
    List<DeadLetterOrder> findByStatusOrderByLastAttemptAtDesc(DlqStatus status);
    Optional<DeadLetterOrder> findByOrderId(String orderId);
}
