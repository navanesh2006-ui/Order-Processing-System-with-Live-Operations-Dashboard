export type OrderPriority = 'VIP' | 'STANDARD';

export type OrderStatus = 'RECEIVED' | 'PROCESSING' | 'CONFIRMED' | 'FAILED' | 'DEAD_LETTERED';

export type DlqStatus = 'ACTIVE' | 'REPLAYED' | 'DISCARDED';

export interface Inventory {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  price: number;
  quantity: number;
  version: number;
  lowStock: boolean;
}

export interface OrderEvent {
  id: number;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  reason: string;
  createdAt: string;
}

export interface Order {
  id: string;
  idempotencyKey: string;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  priority: OrderPriority;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
  events: OrderEvent[];
  duplicateSuppressed?: boolean;
}

export interface DeadLetterOrder {
  id: number;
  orderId: string;
  failureReason: string;
  retryCount: number;
  lastAttemptAt: string;
  payloadJson: string;
  status: DlqStatus;
  createdAt: string;
}

export interface ThreadPoolStats {
  activeThreads: number;
  queueDepth: number;
  completedTasks: number;
  poolSize: number;
  maxPoolSize: number;
  ordersPerSecond: number;
}

export interface ChaosTestRequest {
  totalOrders: number;
  concurrencyLevel: number;
  includeScarceStock: boolean;
}

export interface ChaosTestResponse {
  testId: string;
  totalOrdersSubmitted: number;
  status: string;
  durationMs: number;
}

export interface WsMessage<T = any> {
  type: string;
  timestamp: string;
  payload: T;
}

export interface ThroughputPoint {
  time: string;
  tps: number;
  confirmed: number;
  failed: number;
}
