# Acentra — High-Concurrency Order Processing System with Live Operations Dashboard

[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-4.x-38bdf8.svg)](https://tailwindcss.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ed.svg)](https://www.docker.com/)

> A production-quality, hackathon-ready order processing system built to demonstrate **guaranteed zero-overselling under heavy concurrency**, bounded resilience retries, dead-letter queue (DLQ) recovery, and real-time STOMP WebSocket observability.

---

## 🏛️ System Architecture

```
[ Incoming Requests (Load Test / Shoppers) ]
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. INGESTION & IDEMPOTENCY LAYER                            │
│    • Idempotency Check: Client-provided `idempotencyKey`    │
│    • Duplicate Suppression: Instant return of cached order  │
│    • Persists initial state: `RECEIVED`                     │
│    • Emits OrderEvent audit log                             │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. THREAD POOL & PRIORITY LANE (Layer 1 Concurrency)        │
│    • Dedicated Fixed-Size `ThreadPoolExecutor` (4-10 threads)│
│    • Backed by `PriorityBlockingQueue`                      │
│    • VIP Orders prioritized over STANDARD orders with FIFO   │
│    • Real-time queue depth & active worker telemetry        │
└───────────────────────────┬─────────────────────────────────┘
                            │ Workers dequeue
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. STOCK DECREMENT & LOCKING (Layer 2 Concurrency)          │
│    • State: `PROCESSING`                                    │
│    • Optimistic Locking: `@Version` column on `Inventory`   │
│    • Resilience4j Bounded Retry: Exponential backoff + jitter│
│    • Stock validation: quantity >= requested (never < 0)    │
│    • Success: `CONFIRMED`                                   │
└───────────────────────────┬─────────────────────────────────┘
                            │ Failure (Exhausted Retries OR Out of Stock)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. DEAD LETTER QUEUE (DLQ) & RECOVERY                       │
│    • State: `FAILED` ➔ `DEAD_LETTERED`                      │
│    • Persisted in `dead_letter_orders` table                │
│    • Audit log recorded with root-cause failure reason      │
│    • Operator Actions: "Replay" (VIP re-queue) or "Discard" │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. LIVE OBSERVABILITY DASHBOARD (STOMP WebSockets)          │
│    • Topics: `/topic/orders`, `/topic/inventory`,           │
│              `/topic/metrics`, `/topic/dlq`, `/topic/alerts`│
│    • React + TypeScript + Tailwind Operations Console       │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚡ Concurrency Model: Why We Deliberately Use BOTH Layers

A central question judges ask: *"How did you handle concurrency?"* 
We implemented **two distinct, decoupled layers of concurrency control**:

### Layer 1: Ingestion Concurrency — `ThreadPoolExecutor` & `PriorityBlockingQueue`
- **Purpose:** Controls system load, thread allocation, and queue backpressure.
- **Mechanism:** HTTP requests are rapidly accepted and dispatched into an in-memory priority queue consumed by a fixed-size worker pool (`core: 4`, `max: 10`). VIP orders jump ahead of standard orders while preserving strict FIFO ordering within each tier.
- **Why this alone is not enough:** In a distributed multi-node environment (or even across multiple worker threads), an in-memory queue does not protect database records from race conditions.

### Layer 2: Persistence Concurrency — Optimistic Locking (`@Version`) & Resilience4j Bounded Retries
- **Purpose:** Prevents race conditions and guarantees stock never oversells at the database level.
- **Mechanism:** The `Inventory` entity has a `@Version` field. When multiple worker threads attempt to decrement stock for the same SKU simultaneously:
  1. The first thread commits successfully and increments the version number.
  2. The conflicting threads fail with `ObjectOptimisticLockingFailureException`.
  3. Rather than dropping the transaction, **Resilience4j** automatically retries the stock decrement up to **5 times with exponential backoff and jitter** against a fresh database read (`Propagation.REQUIRES_NEW`).
  4. If stock is depleted, the transaction aborts with `InsufficientStockException`, and routes the order to the Dead Letter Queue.
  5. **Zero overselling guarantee:** Under a blast of 100 concurrent requests for 5 items, **exactly 5 orders are confirmed**, the inventory ends at **0 (never negative)**, and 95 orders safely route to the DLQ.

---

## 🎯 7 Innovative Additions Implemented

1. **Live "Chaos / Load Test" Button:** Fire 20–300 concurrent simulated orders at customizable concurrency (2–25 threads) directly from the dashboard. Judges *watch* the live feed flood, inventory drop, and DLQ catch failures in real time.
2. **Interactive DLQ Replay & Discard:** Operators can inspect the root-cause failure reason and click **"Replay"** (which re-queues the order with VIP priority once stock is available) or **"Discard"**.
3. **Idempotency Proof Toggle:** Live test button that submits identical orders with the same `idempotencyKey` back-to-back. The UI proves the first is accepted while the second is marked `DUPLICATE_SUPPRESSED` without double-decrementing stock.
4. **VIP Priority Lane:** Built using a custom `PrioritizedOrderTask` inside a `PriorityBlockingQueue`, proving VIP orders preempt standard orders.
5. **Visible Concurrency Telemetry Panel:** Real-time meters for active worker threads, buffer queue depth, completed tasks, and calculated transactions-per-second (TPS).
6. **Real-time STOMP over SockJS:** Zero polling. Orders, inventory counts, thread metrics, and DLQ records push over `/topic/*` channels.
7. **One-Command Docker Compose:** Complete orchestration of PostgreSQL 16 + Spring Boot backend + Nginx frontend.

---

## 🚀 Quick Start & Running Options

### Option A: One-Command Docker Run (Recommended for Judges)
Ensure Docker Desktop is running, then execute:
```bash
docker-compose up --build
```
- **Live Operations Dashboard:** [http://localhost](http://localhost) (or [http://localhost:3000](http://localhost:3000))
- **Spring Boot API & Actuator:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **PostgreSQL Database:** `localhost:5432` (`orderdb`)

---

### Option B: Local Run (Zero-Config, Without Docker)
The backend features an automated **in-memory H2 profile** with PostgreSQL compatibility mode, so you don't even need a local database installed!

#### 1. Start Backend:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*(Backend boots on `http://localhost:8080` and pre-seeds products automatically)*

#### 2. Start Frontend:
```powershell
cd frontend
npm install
npm run dev
```
*(Dashboard opens at `http://localhost:5173`)*

Or simply execute the automated startup script:
```powershell
.\start_dev.ps1
```

---

## 🧪 Running Automated Concurrency & Zero-Oversell Tests

We provide automated unit and integration tests verifying the concurrency guarantees:
```powershell
cd backend
.\mvnw.cmd test
```
**Test Scenarios Executed:**
1. `testHighConcurrencyZeroOverselling`: 50 concurrent threads simultaneously blast 5 available units. Verifies **exactly 5 confirmed**, stock is **0 (never negative)**, and remaining 45 orders are routed to the DLQ table.
2. `testIdempotencyProtection`: Verifies retransmitting the same idempotency key suppresses duplicates and prevents double-decrementing stock.

---

## 📋 Judge Demo Script (Step-by-Step Walkthrough)

Follow this 2-minute demo script to showcase the system to judges:

1. **Open the Operations Dashboard:**
   Navigate to [http://localhost:5173](http://localhost:5173) (or `http://localhost` if using Docker).
   - Point out the **Live WS connection pill** (top right).
   - Point out the **Worker Threads / Queue Depth / TPS ticker**.
   - Show the **Live Inventory Storage**: Notice `NVIDIA RTX 5090 Blackwell` is flagged **SCARCE (3 units)** with DB Version `v0`.

2. **Launch the Live Chaos Test:**
   - Click **"Launch Chaos Test"** in the top bar.
   - Set **Total Orders = 100**, **Concurrency = 10 threads**, and ensure **"Target Scarce Stock"** is checked.
   - Click **"Fire 100 Orders Now"**.
   - **What happens live before the judges:**
     - The **Queue Depth** and **Active Workers** spike immediately on the top ticker.
     - The **Live Ingestion Feed** streams incoming orders transitioning `RECEIVED ➔ PROCESSING ➔ CONFIRMED` or `DEAD-LETTERED`.
     - The `RTX 5090 Blackwell` stock visibly decrements from `3 ➔ 2 ➔ 1 ➔ 0` and stops at **0**.
     - Point out the DB Version has incremented (e.g. `v3`), proving Optimistic Locking.
     - Excess orders for the scarce GPU fail with `Insufficient stock` and populate the **Dead Letter Queue (DLQ)** panel on the right.

3. **Demonstrate State Machine Audit Trail:**
   - Click any card in the **Live Ingestion Feed**.
   - An accordion expands displaying the immutable `OrderEvent` timeline with timestamps and transition reasons.

4. **Demonstrate DLQ Recovery (Operator Replay):**
   - Look at the DLQ panel: find a failed RTX 5090 order.
   - Click **"+10"** on the RTX 5090 card in the Inventory Grid to restock.
   - In the DLQ panel, click **"Replay"** on the failed order.
   - Watch the order re-enter the processing pipeline with **VIP priority**, decrement stock to 9, and transition to **CONFIRMED**.

5. **Demonstrate Idempotency Protection:**
   - Click **"Idempotency Demo"** in the navbar.
   - Click **"Simulate Duplicate Submissions Live"**.
   - Show the two side-by-side responses: first is accepted (`duplicateSuppressed: false`), second is suppressed (`duplicateSuppressed: true`) without double-decrementing stock.

6. **CLI Load Test Script (Optional Headless Demo):**
   Run the standalone Python script from your terminal:
   ```bash
   python load_test_sim.py 50 10
   ```
   Inspect the terminal audit confirming **PASS: ZERO OVERSELLING CONFIRMED!**
