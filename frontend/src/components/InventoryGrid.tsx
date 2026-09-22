import React, { useState } from 'react';
import { Package, Plus, AlertTriangle, ShieldAlert, CheckCircle } from 'lucide-react';
import { Inventory } from '../types';

interface InventoryGridProps {
  inventories: Inventory[];
  onRestock: (productId: number, amount: number) => Promise<void>;
}

export const InventoryGrid: React.FC<InventoryGridProps> = ({ inventories, onRestock }) => {
  const [restockingId, setRestockingId] = useState<number | null>(null);

  const handleRestock = async (productId: number, amount: number) => {
    try {
      setRestockingId(productId);
      await onRestock(productId, amount);
    } finally {
      setRestockingId(null);
    }
  };

  return (
    <div className="bg-[#0f1422] rounded-2xl border border-slate-800 p-5 shadow-xl">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Package className="h-5 w-5 text-cyan-400" />
          <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
            Live Inventory Storage
          </h2>
        </div>
        <span className="text-xs text-slate-400 font-mono">
          Optimistic Lock Protected (@Version)
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
        {inventories.map((item) => {
          const isScarce = item.quantity <= 3;
          const isLow = item.quantity > 3 && item.quantity <= 8;
          const isHealthy = item.quantity > 8;

          return (
            <div
              key={item.id}
              className={`relative rounded-xl p-4 transition-all duration-300 border ${
                isScarce
                  ? 'bg-rose-950/20 border-rose-800/60 shadow-lg shadow-rose-950/30'
                  : isLow
                  ? 'bg-amber-950/20 border-amber-800/60'
                  : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
              }`}
            >
              {/* Top row: Name & SKU */}
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h3 className="text-sm font-bold text-slate-100 line-clamp-1">
                    {item.productName}
                  </h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-[11px] font-mono font-medium text-slate-400">
                      {item.productSku}
                    </span>
                    <span className="text-[10px] font-mono px-1.5 py-0.2 bg-slate-800 rounded text-slate-300">
                      ${item.price}
                    </span>
                  </div>
                </div>

                {/* Stock Level Badge */}
                <span
                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                    isScarce
                      ? 'bg-rose-900/60 text-rose-300 border border-rose-700 animate-pulse'
                      : isLow
                      ? 'bg-amber-900/60 text-amber-300 border border-amber-700'
                      : 'bg-emerald-950/60 text-emerald-300 border border-emerald-800'
                  }`}
                >
                  {isScarce ? (
                    <>
                      <ShieldAlert className="h-3 w-3" /> Scarce
                    </>
                  ) : isLow ? (
                    <>
                      <AlertTriangle className="h-3 w-3" /> Low Stock
                    </>
                  ) : (
                    <>
                      <CheckCircle className="h-3 w-3" /> Healthy
                    </>
                  )}
                </span>
              </div>

              {/* Middle row: Big Quantity Display & Lock Version */}
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <div className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">
                    Available Stock
                  </div>
                  <div className="flex items-baseline gap-2">
                    <span
                      className={`text-3xl font-extrabold font-mono transition-transform duration-300 ${
                        isScarce ? 'text-rose-400' : isLow ? 'text-amber-400' : 'text-emerald-400'
                      }`}
                    >
                      {item.quantity}
                    </span>
                    <span className="text-xs text-slate-500 font-medium">units</span>
                  </div>
                </div>

                {/* Version chip for concurrency proof */}
                <div className="text-right">
                  <div className="text-[10px] font-medium text-slate-500 uppercase tracking-wider">
                    DB Version
                  </div>
                  <div className="text-xs font-mono font-bold text-indigo-400 bg-indigo-950/50 px-2 py-0.5 rounded border border-indigo-900/60">
                    v{item.version}
                  </div>
                </div>
              </div>

              {/* Bottom row: Restock Quick Actions */}
              <div className="mt-4 pt-3 border-t border-slate-800/60 flex items-center justify-between">
                <span className="text-[11px] text-slate-500 font-mono">Restock:</span>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => handleRestock(item.productId, 10)}
                    disabled={restockingId === item.productId}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition disabled:opacity-50"
                  >
                    <Plus className="h-3 w-3" />
                    <span>+10</span>
                  </button>
                  <button
                    onClick={() => handleRestock(item.productId, 50)}
                    disabled={restockingId === item.productId}
                    className="flex items-center gap-1 px-2.5 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition disabled:opacity-50"
                  >
                    <Plus className="h-3 w-3" />
                    <span>+50</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
