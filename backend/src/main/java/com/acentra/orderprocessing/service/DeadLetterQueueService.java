package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.DeadLetterOrderDTO;
import com.acentra.orderprocessing.model.DeadLetterOrder;
import com.acentra.orderprocessing.model.DlqStatus;
import com.acentra.orderprocessing.repository.DeadLetterOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private final DeadLetterOrderRepository dlqRepository;
    private final WebSocketBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueService(
            DeadLetterOrderRepository dlqRepository,
            WebSocketBroadcaster broadcaster,
            ObjectMapper objectMapper
    ) {
        this.dlqRepository = dlqRepository;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeadLetterOrder recordFailure(String orderId, String failureReason, int retryCount, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = "{\"raw\":\"" + String.valueOf(payload) + "\"}";
        }

        DeadLetterOrder dlq = DeadLetterOrder.builder()
                .orderId(orderId)
                .failureReason(failureReason)
                .retryCount(retryCount)
                .lastAttemptAt(Instant.now())
                .payloadJson(payloadJson)
                .status(DlqStatus.ACTIVE)
                .build();

        DeadLetterOrder saved = dlqRepository.saveAndFlush(dlq);
        log.warn("Order {} routed to Dead Letter Queue. Reason: {}", orderId, failureReason);

        DeadLetterOrderDTO dto = toDTO(saved);
        broadcaster.broadcastDlq(dto);
        return saved;
    }

    @Transactional
    public DeadLetterOrderDTO markAsReplayed(Long dlqId) {
        DeadLetterOrder dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found with ID: " + dlqId));

        dlq.setStatus(DlqStatus.REPLAYED);
        DeadLetterOrder saved = dlqRepository.saveAndFlush(dlq);

        DeadLetterOrderDTO dto = toDTO(saved);
        broadcaster.broadcastDlq(dto);
        return dto;
    }

    @Transactional
    public DeadLetterOrderDTO discard(Long dlqId) {
        DeadLetterOrder dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found with ID: " + dlqId));

        dlq.setStatus(DlqStatus.DISCARDED);
        DeadLetterOrder saved = dlqRepository.saveAndFlush(dlq);
        log.info("Dead letter order {} was discarded by operator", dlq.getOrderId());

        DeadLetterOrderDTO dto = toDTO(saved);
        broadcaster.broadcastDlq(dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<DeadLetterOrderDTO> getRecentDlqOrders(int limit) {
        return dlqRepository.findAllByOrderByLastAttemptAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeadLetterOrder getEntity(Long dlqId) {
        return dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ entry not found: " + dlqId));
    }

    public DeadLetterOrderDTO toDTO(DeadLetterOrder dlq) {
        return DeadLetterOrderDTO.builder()
                .id(dlq.getId())
                .orderId(dlq.getOrderId())
                .failureReason(dlq.getFailureReason())
                .retryCount(dlq.getRetryCount())
                .lastAttemptAt(dlq.getLastAttemptAt())
                .payloadJson(dlq.getPayloadJson())
                .status(dlq.getStatus())
                .createdAt(dlq.getCreatedAt())
                .build();
    }
}
