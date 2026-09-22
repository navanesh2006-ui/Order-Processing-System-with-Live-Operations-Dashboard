import React, { useEffect, useState, useRef } from 'react';
import { 
  fetchDlq, 
  fetchInventory, 
  fetchOrders, 
  fetchThreadPoolStats, 
  restockProduct, 
  replayDlq, 
  discardDlq, 
  runChaosTest, 
  resetSystemData 
} from './services/api';
import { wsService, ConnectionState } from './services/websocket';
import { 
  ChaosTestRequest, 
  DeadLetterOrder, 
  Inventory, 
  Order, 
  ThreadPoolStats, 
  ThroughputPoint 
} from './types';
import { Navbar } from './components/Navbar';
import { ThreadPoolPanel } from './components/ThreadPoolPanel';
import { InventoryGrid } from './components/InventoryGrid';
import { OrderFeed } from './components/OrderFeed';
import { DeadLetterQueuePanel } from './components/DeadLetterQueuePanel';
import { AnalyticsPanel } from './components/AnalyticsPanel';
import { ChaosModal } from './components/ChaosModal';
import { IdempotencyModal } from './components/IdempotencyModal';
import { ToastContainer, ToastItem } from './components/ToastContainer';

export const App: React.FC = () => {
  const [inventories, setInventories] = useState<Inventory[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [dlqOrders, setDlqOrders] = useState<DeadLetterOrder[]>([]);
  const [threadPoolStats, setThreadPoolStats] = useState<ThreadPoolStats>({
    activeThreads: 0,
    queueDepth: 0,
    completedTasks: 0,
    poolSize: 4,
    maxPoolSize: 10,
    ordersPerSecond: 0,
  });
  const [throughputHistory, setThroughputHistory] = useState<ThroughputPoint[]>([]);
  const [connectionState, setConnectionState] = useState<ConnectionState>('CONNECTING');
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const [isChaosOpen, setIsChaosOpen] = useState(false);
  const [isIdempotencyOpen, setIsIdempotencyOpen] = useState(false);
  const [isResetting, setIsResetting] = useState(false);

  // Keep track of counts for live analytics chart
  const confirmedDeltaRef = useRef<number>(0);
  const failedDeltaRef = useRef<number>(0);

  const addToast = (type: ToastItem['type'], title: string, message: string) => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev.slice(-3), { id, type, title, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  };

  const dismissToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // Initial load
  const loadInitialData = async () => {
    try {
      const [inv, ord, dlq, stats] = await Promise.all([
        fetchInventory().catch(() => []),
        fetchOrders(50).catch(() => []),
        fetchDlq(50).catch(() => []),
        fetchThreadPoolStats().catch(() => ({
          activeThreads: 0,
          queueDepth: 0,
          completedTasks: 0,
          poolSize: 4,
          maxPoolSize: 10,
          ordersPerSecond: 0,
        })),
      ]);
      setInventories(inv);
      setOrders(ord);
      setDlqOrders(dlq);
      setThreadPoolStats(stats);
    } catch (err) {
      console.error('Failed to load initial data:', err);
    }
  };

  useEffect(() => {
    loadInitialData();

    // Connect WebSocket
    wsService.connect({
      onConnectionChange: (state) => setConnectionState(state),
      onInventoryUpdate: (updated) => {
        setInventories((prev) => {
          const index = prev.findIndex((i) => i.productId === updated.productId);
          if (index !== -1) {
            const next = [...prev];
            next[index] = updated;
            return next;
          }
          return [...prev, updated];
        });
      },
      onOrderUpdate: (updatedOrder) => {
        if (updatedOrder.status === 'CONFIRMED') {
          confirmedDeltaRef.current += 1;
        } else if (updatedOrder.status === 'DEAD_LETTERED' || updatedOrder.status === 'FAILED') {
          failedDeltaRef.current += 1;
        }

        setOrders((prev) => {
          const index = prev.findIndex((o) => o.id === updatedOrder.id);
          if (index !== -1) {
            const next = [...prev];
            next[index] = updatedOrder;
            return next;
          }
          return [updatedOrder, ...prev.slice(0, 99)];
        });
      },
      onMetricsUpdate: (stats) => {
        setThreadPoolStats(stats);

        const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        setThroughputHistory((prev) => {
          const point: ThroughputPoint = {
            time: now,
            tps: stats.ordersPerSecond,
            confirmed: confirmedDeltaRef.current,
            failed: failedDeltaRef.current,
          };
          confirmedDeltaRef.current = 0;
          failedDeltaRef.current = 0;
          return [...prev.slice(-19), point];
        });
      },
      onDlqUpdate: (dlqItem) => {
        setDlqOrders((prev) => {
          const index = prev.findIndex((d) => d.id === dlqItem.id);
          if (index !== -1) {
            const next = [...prev];
            next[index] = dlqItem;
            return next;
          }
          return [dlqItem, ...prev];
        });

        if (dlqItem.status === 'ACTIVE') {
          addToast(
            'DLQ',
            'Order Routed to DLQ',
            `Order ${dlqItem.orderId.substring(0, 8)}... failed: ${dlqItem.failureReason}`
          );
        }
      },
      onAlert: (alert) => {
        addToast('LOW_STOCK', 'System Telemetry Alert', alert.message);
      },
    });

    return () => {
      wsService.disconnect();
    };
  }, []);

  const handleRestock = async (productId: number, amount: number) => {
    const updated = await restockProduct(productId, amount);
    setInventories((prev) =>
      prev.map((item) => (item.productId === productId ? updated : item))
    );
    addToast('INFO', 'Restocked SKU', `Added ${amount} units to inventory.`);
  };

  const handleReplayDlq = async (id: number) => {
    await replayDlq(id);
    setDlqOrders((prev) =>
      prev.map((item) => (item.id === id ? { ...item, status: 'REPLAYED' } : item))
    );
    addToast('INFO', 'DLQ Replayed', 'Replay transaction submitted into VIP worker lane.');
  };

  const handleDiscardDlq = async (id: number) => {
    await discardDlq(id);
    setDlqOrders((prev) =>
      prev.map((item) => (item.id === id ? { ...item, status: 'DISCARDED' } : item))
    );
    addToast('INFO', 'DLQ Discarded', 'Dead-lettered order removed by operator.');
  };

  const handleRunChaos = async (req: ChaosTestRequest) => {
    const res = await runChaosTest(req);
    return res;
  };

  const handleReset = async () => {
    try {
      setIsResetting(true);
      await resetSystemData();
      await loadInitialData();
      addToast('INFO', 'System Reset Complete', 'All orders, events, and DLQ cleared. Stock restored.');
    } finally {
      setIsResetting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0a0d14] text-slate-100 flex flex-col font-sans">
      {/* Top Navbar */}
      <Navbar
        connectionState={connectionState}
        stats={threadPoolStats}
        onOpenChaosModal={() => setIsChaosOpen(true)}
        onOpenIdempotencyModal={() => setIsIdempotencyOpen(true)}
        onReset={handleReset}
        isResetting={isResetting}
      />

      {/* Main Operations Canvas */}
      <main className="flex-1 p-4 lg:p-6 max-w-7xl mx-auto w-full space-y-5">
        {/* Row 1: Concurrency Telemetry Gauges */}
        <ThreadPoolPanel stats={threadPoolStats} />

        {/* Row 2: Live Inventory Grid */}
        <InventoryGrid inventories={inventories} onRestock={handleRestock} />

        {/* Row 3: Split Stream - Ingestion Feed & DLQ */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
          <div className="lg:col-span-2">
            <OrderFeed orders={orders} />
          </div>
          <div className="lg:col-span-1">
            <DeadLetterQueuePanel
              dlqOrders={dlqOrders}
              onReplay={handleReplayDlq}
              onDiscard={handleDiscardDlq}
            />
          </div>
        </div>

        {/* Row 4: Real-Time Throughput & Performance Charts */}
        <AnalyticsPanel data={throughputHistory} />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-[#07090e] py-3 text-center text-xs text-slate-500 font-mono">
        Acentra Zero-Oversell High-Concurrency Order Processing System • Hackathon Build 2026
      </footer>

      {/* Modals & Alerts */}
      <ChaosModal
        isOpen={isChaosOpen}
        onClose={() => setIsChaosOpen(false)}
        onRunTest={handleRunChaos}
      />

      <IdempotencyModal
        isOpen={isIdempotencyOpen}
        onClose={() => setIsIdempotencyOpen(false)}
      />

      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
};

export default App;
