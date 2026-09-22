#!/usr/bin/env python3
"""
CLI Concurrency Load Test & Verification Script for Acentra Order Processing System.
Simulates high concurrent shopping bursts, verifies zero-overselling, and checks DLQ routing.
"""

import concurrent.futures
import json
import random
import sys
import time
import urllib.request
import urllib.error
import uuid

BASE_URL = "http://localhost:8080/api"

def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "AcentraLoadTest/1.0"})
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

def post_json(url, payload):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", "User-Agent": "AcentraLoadTest/1.0"}
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8")), resp.getcode()
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8")), e.code

def run_load_test(total_orders=60, concurrency=10):
    print("=" * 70)
    print("  ACENTRA HIGH-CONCURRENCY ZERO-OVERSELL CLI LOAD TEST")
    print("=" * 70)
    
    # 1. Inspect initial inventory
    try:
        inventories = get_json(f"{BASE_URL}/inventory")
    except Exception as e:
        print(f"[-] ERROR: Cannot reach backend at {BASE_URL}. Is Spring Boot running?")
        print(f"    Details: {e}")
        sys.exit(1)
        
    print("\n[1] Initial Inventory Snapshot:")
    for inv in inventories:
        sku = inv.get("productSku")
        qty = inv.get("quantity")
        ver = inv.get("version")
        status = "SCARCE" if qty <= 3 else "LOW" if qty <= 8 else "HEALTHY"
        print(f"    - SKU: {sku:<16} | Stock: {qty:>3} units | DB Version: v{ver:<3} | [{status}]")

    # Select scarce item (RTX 5090 Blackwell)
    target = inventories[0]
    initial_stock = target.get("quantity")
    print(f"\n[2] Target for Contention Blast: {target.get('productName')} (SKU: {target.get('productSku')})")
    print(f"    Current stock: {initial_stock} units. Blasting with {total_orders} concurrent orders...")

    test_batch_id = str(uuid.uuid4())[:8]
    submitted = 0
    start_time = time.time()

    def submit_single_order(i):
        key = f"cli-test-{test_batch_id}-order-{i}"
        payload = {
            "productId": target["productId"],
            "quantity": 1,
            "idempotencyKey": key,
            "priority": "VIP" if i % 5 == 0 else "STANDARD"
        }
        res, code = post_json(f"{BASE_URL}/orders", payload)
        return res, code

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(submit_single_order, i) for i in range(total_orders)]
        for f in concurrent.futures.as_completed(futures):
            res, code = f.result()
            results.append((res, code))
            submitted += 1
            if submitted % 10 == 0 or submitted == total_orders:
                print(f"    Submitted {submitted}/{total_orders} orders...")

    duration = time.time() - start_time
    print(f"\n[3] Ingestion finished in {duration:.2f}s ({(total_orders/duration):.1f} ops/sec)")
    print("    Waiting 2.5s for worker thread pool to drain and finalize state machines...")
    time.sleep(2.5)

    # 4. Verification
    final_inventories = get_json(f"{BASE_URL}/inventory")
    final_target = next(i for i in final_inventories if i["productId"] == target["productId"])
    final_stock = final_target["quantity"]
    final_version = final_target["version"]

    dlq_orders = get_json(f"{BASE_URL}/dlq?limit=100")
    active_dlq = [d for d in dlq_orders if d["status"] == "ACTIVE"]

    orders = get_json(f"{BASE_URL}/orders?limit=100")
    confirmed = [o for o in orders if o.get("status") == "CONFIRMED" and o.get("productId") == target["productId"]]
    dead_lettered = [o for o in orders if o.get("status") == "DEAD_LETTERED" and o.get("productId") == target["productId"]]

    print("\n" + "=" * 70)
    print("  VERIFICATION AUDIT RESULTS")
    print("=" * 70)
    print(f"  Target SKU:                    {final_target.get('productSku')}")
    print(f"  Initial Stock:                 {initial_stock} units")
    print(f"  Final Stock Remaining:         {final_stock} units (NEVER negative: {final_stock >= 0})")
    print(f"  Hibernate Lock Version:        v{final_version}")
    print(f"  Confirmed Purchases:           {len(confirmed)}")
    print(f"  Failed / Routed to DLQ:        {len(dead_lettered)}")
    print(f"  DLQ Active Backlog:            {len(active_dlq)}")
    
    oversold = final_stock < 0
    correct_zero_oversell = (final_stock >= 0) and (len(confirmed) <= initial_stock)
    
    print("\n" + "-" * 70)
    if correct_zero_oversell and not oversold:
        print("  >>> PASS: ZERO OVERSELLING CONFIRMED! <<<")
        print("  Contention was cleanly resolved via Optimistic Locking & DLQ routing.")
    else:
        print("  >>> FAIL: Overselling detected! <<<")
    print("-" * 70 + "\n")

if __name__ == "__main__":
    orders_count = 50
    concurrency_count = 10
    if len(sys.argv) > 1:
        orders_count = int(sys.argv[1])
    if len(sys.argv) > 2:
        concurrency_count = int(sys.argv[2])
    run_load_test(orders_count, concurrency_count)
