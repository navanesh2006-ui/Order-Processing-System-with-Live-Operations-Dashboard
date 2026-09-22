import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { DeadLetterOrder, Inventory, Order, ThreadPoolStats, WsMessage } from '../types';

export type ConnectionState = 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED';

export interface WebSocketCallbacks {
  onOrderUpdate?: (order: Order) => void;
  onInventoryUpdate?: (inventory: Inventory) => void;
  onMetricsUpdate?: (metrics: ThreadPoolStats) => void;
  onDlqUpdate?: (dlq: DeadLetterOrder) => void;
  onAlert?: (alert: { message: string; details: any }) => void;
  onConnectionChange?: (state: ConnectionState) => void;
}

class WebSocketService {
  private client: Client | null = null;
  private callbacks: WebSocketCallbacks = {};

  public connect(callbacks: WebSocketCallbacks) {
    this.callbacks = callbacks;
    this.callbacks.onConnectionChange?.('CONNECTING');

    const socketUrl = `${window.location.protocol}//${window.location.host}/ws`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg) => {
        // quiet debug log
      },
      onConnect: () => {
        this.callbacks.onConnectionChange?.('CONNECTED');
        this.subscribeTopics();
      },
      onDisconnect: () => {
        this.callbacks.onConnectionChange?.('DISCONNECTED');
      },
      onStompError: (frame) => {
        console.error('STOMP protocol error:', frame.headers['message']);
        this.callbacks.onConnectionChange?.('DISCONNECTED');
      },
    });

    this.client.activate();
  }

  private subscribeTopics() {
    if (!this.client || !this.client.connected) return;

    // 1. Live Orders Topic
    this.client.subscribe('/topic/orders', (message: IMessage) => {
      try {
        const payload: WsMessage<Order> = JSON.parse(message.body);
        if (payload && payload.payload) {
          this.callbacks.onOrderUpdate?.(payload.payload);
        }
      } catch (err) {
        console.error('Failed to parse order update', err);
      }
    });

    // 2. Live Inventory Topic
    this.client.subscribe('/topic/inventory', (message: IMessage) => {
      try {
        const payload: WsMessage<Inventory> = JSON.parse(message.body);
        if (payload && payload.payload) {
          this.callbacks.onInventoryUpdate?.(payload.payload);
        }
      } catch (err) {
        console.error('Failed to parse inventory update', err);
      }
    });

    // 3. Live Metrics / Thread Pool Topic
    this.client.subscribe('/topic/metrics', (message: IMessage) => {
      try {
        const payload: WsMessage<ThreadPoolStats> = JSON.parse(message.body);
        if (payload && payload.payload) {
          this.callbacks.onMetricsUpdate?.(payload.payload);
        }
      } catch (err) {
        console.error('Failed to parse metrics update', err);
      }
    });

    // 4. Dead Letter Queue Topic
    this.client.subscribe('/topic/dlq', (message: IMessage) => {
      try {
        const payload: WsMessage<DeadLetterOrder> = JSON.parse(message.body);
        if (payload && payload.payload) {
          this.callbacks.onDlqUpdate?.(payload.payload);
        }
      } catch (err) {
        console.error('Failed to parse DLQ update', err);
      }
    });

    // 5. System Alerts & Chaos Events
    this.client.subscribe('/topic/alerts', (message: IMessage) => {
      try {
        const payload: WsMessage<{ message: string; details: any }> = JSON.parse(message.body);
        if (payload && payload.payload) {
          this.callbacks.onAlert?.(payload.payload);
        }
      } catch (err) {
        console.error('Failed to parse alert update', err);
      }
    });
  }

  public disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.callbacks.onConnectionChange?.('DISCONNECTED');
    }
  }
}

export const wsService = new WebSocketService();
