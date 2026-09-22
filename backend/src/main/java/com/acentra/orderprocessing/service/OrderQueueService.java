package com.acentra.orderprocessing.service;

import com.acentra.orderprocessing.dto.ThreadPoolStatsDTO;
import com.acentra.orderprocessing.model.OrderPriority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@EnableScheduling
public class OrderQueueService {

    private static final Logger log = LoggerFactory.getLogger(OrderQueueService.class);

    private final WebSocketBroadcaster broadcaster;
    private final MeterRegistry meterRegistry;
    private final ThreadPoolExecutor executor;
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final Counter processedOrdersCounter;
    private final AtomicLong rollingWindowCompleted = new AtomicLong(0);
    private volatile double currentThroughput = 0.0;
    private volatile long lastThroughputTimestamp = System.currentTimeMillis();
    private volatile long lastThroughputCompletedCount = 0;

    public OrderQueueService(
            WebSocketBroadcaster broadcaster,
            MeterRegistry meterRegistry,
            @Value("${order-processing.thread-pool.core-size:4}") int coreSize,
            @Value("${order-processing.thread-pool.max-size:10}") int maxSize,
            @Value("${order-processing.thread-pool.queue-capacity:1000}") int queueCapacity,
            @Value("${order-processing.thread-pool.keep-alive-seconds:60}") int keepAliveSeconds
    ) {
        this.broadcaster = broadcaster;
        this.meterRegistry = meterRegistry;

        BlockingQueue<Runnable> priorityQueue = new PriorityBlockingQueue<>(
                queueCapacity,
                (r1, r2) -> {
                    if (r1 instanceof PrioritizedOrderTask p1 && r2 instanceof PrioritizedOrderTask p2) {
                        return p1.compareTo(p2);
                    }
                    return 0;
                }
        );

        this.executor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                priorityQueue,
                new ThreadFactory() {
                    private final AtomicLong count = new AtomicLong(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "order-worker-" + count.getAndIncrement());
                        t.setDaemon(false);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.processedOrdersCounter = Counter.builder("order_processing_orders_completed_total")
                .description("Total number of orders processed by thread pool")
                .register(meterRegistry);
    }

    public void submitOrderTask(String orderId, OrderPriority priority, Runnable task) {
        long seq = sequenceGenerator.incrementAndGet();
        PrioritizedOrderTask prioritizedTask = new PrioritizedOrderTask(orderId, priority, seq, () -> {
            try {
                task.run();
            } finally {
                processedOrdersCounter.increment();
                rollingWindowCompleted.incrementAndGet();
            }
        });

        executor.execute(prioritizedTask);
        log.debug("Submitted task for order {} with priority {}, queue depth: {}",
                orderId, priority, executor.getQueue().size());
    }

    public ThreadPoolStatsDTO getStats() {
        return ThreadPoolStatsDTO.builder()
                .activeThreads(executor.getActiveCount())
                .queueDepth(executor.getQueue().size())
                .completedTasks(executor.getCompletedTaskCount())
                .poolSize(executor.getPoolSize())
                .maxPoolSize(executor.getMaximumPoolSize())
                .ordersPerSecond(Math.round(currentThroughput * 10.0) / 10.0)
                .build();
    }

    @Scheduled(fixedRate = 1000)
    public void calculateThroughputAndBroadcast() {
        long now = System.currentTimeMillis();
        long completedNow = executor.getCompletedTaskCount();
        long elapsedMs = now - lastThroughputTimestamp;

        if (elapsedMs >= 900) {
            long delta = completedNow - lastThroughputCompletedCount;
            currentThroughput = (delta * 1000.0) / elapsedMs;
            lastThroughputTimestamp = now;
            lastThroughputCompletedCount = completedNow;
        }

        // Broadcast stats live to connected dashboards
        broadcaster.broadcastMetrics(getStats());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down OrderQueueService ThreadPoolExecutor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
