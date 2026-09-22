import { 
  ChaosTestRequest, 
  ChaosTestResponse, 
  DeadLetterOrder, 
  Inventory, 
  Order, 
  OrderPriority, 
  ThreadPoolStats 
} from '../types';

const BASE_URL = '';

export async function fetchInventory(): Promise<Inventory[]> {
  const res = await fetch(`${BASE_URL}/api/inventory`);
  if (!res.ok) throw new Error('Failed to fetch inventory');
  return res.json();
}

export async function restockProduct(productId: number, amount: number): Promise<Inventory> {
  const res = await fetch(`${BASE_URL}/api/inventory/${productId}/restock`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount }),
  });
  if (!res.ok) throw new Error('Failed to restock product');
  return res.json();
}

export async function fetchOrders(limit = 50): Promise<Order[]> {
  const res = await fetch(`${BASE_URL}/api/orders?limit=${limit}`);
  if (!res.ok) throw new Error('Failed to fetch orders');
  return res.json();
}

export async function fetchOrderById(id: string): Promise<Order> {
  const res = await fetch(`${BASE_URL}/api/orders/${id}`);
  if (!res.ok) throw new Error('Failed to fetch order details');
  return res.json();
}

export async function submitOrder(data: {
  productId: number;
  quantity: number;
  idempotencyKey: string;
  priority?: OrderPriority;
}): Promise<Order> {
  const res = await fetch(`${BASE_URL}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to submit order');
  return res.json();
}

export async function fetchDlq(limit = 50): Promise<DeadLetterOrder[]> {
  const res = await fetch(`${BASE_URL}/api/dlq?limit=${limit}`);
  if (!res.ok) throw new Error('Failed to fetch DLQ orders');
  return res.json();
}

export async function replayDlq(id: number): Promise<Order> {
  const res = await fetch(`${BASE_URL}/api/dlq/${id}/replay`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Failed to replay DLQ order');
  return res.json();
}

export async function discardDlq(id: number): Promise<DeadLetterOrder> {
  const res = await fetch(`${BASE_URL}/api/dlq/${id}/discard`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Failed to discard DLQ order');
  return res.json();
}

export async function fetchThreadPoolStats(): Promise<ThreadPoolStats> {
  const res = await fetch(`${BASE_URL}/api/metrics/threadpool`);
  if (!res.ok) throw new Error('Failed to fetch thread pool stats');
  return res.json();
}

export async function runChaosTest(request: ChaosTestRequest): Promise<ChaosTestResponse> {
  const res = await fetch(`${BASE_URL}/api/chaos/load-test`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error('Failed to trigger chaos test');
  return res.json();
}

export async function runIdempotencyDemo(productId = 1): Promise<any> {
  const res = await fetch(`${BASE_URL}/api/chaos/idempotency-demo?productId=${productId}`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Failed to run idempotency demo');
  return res.json();
}

export async function resetSystemData(): Promise<{ message: string }> {
  const res = await fetch(`${BASE_URL}/api/chaos/reset`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Failed to reset system data');
  return res.json();
}
