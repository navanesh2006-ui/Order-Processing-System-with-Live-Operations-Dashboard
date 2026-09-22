import React, { useState } from 'react';
import { ShieldX, RotateCw, Trash2, CheckCircle2, AlertOctagon } from 'lucide-react';
import { DeadLetterOrder } from '../types';

interface DeadLetterQueuePanelProps {
  dlqOrders: DeadLetterOrder[];
  onReplay: (id: number) => Promise<void>;
  onDiscard: (id: number) => Promise<void>;
}

export const DeadLetterQueuePanel: React.FC<DeadLetterQueuePanelProps> = ({
  dlqOrders,
  onReplay,
  onDiscard,
}) => {
  const [actingId, setActingId] = useState<number | null>(null);
  const [actionType, setActionType] = useState<'replay' | 'discard' | null>(null);

  const handleAction = async (id: number, type: 'replay' | 'discard') => {
    try {
      setActingId(id);
      setActionType(type);
      if (type === 'replay') {
        await onReplay(id);
      } else {
        await onDiscard(id);
      }
    } finally {
      setActingId(null);
      setActionType(null);
    }
  };

  const activeOrders = dlqOrders.filter((item) => item.status === 'ACTIVE');

  return (
    <div className="bg-[#0f1422] rounded-2xl border border-slate-800 p-5 shadow-xl flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2">
          <ShieldX className="h-5 w-5 text-rose-400" />
          <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
            Dead Letter Queue (DLQ)
          </h2>
        </div>
        <span
          className={`text-xs font-mono font-bold px-2.5 py-0.5 rounded-full border ${
            activeOrders.length > 0
              ? 'bg-rose-950/80 border-rose-800 text-rose-400 animate-pulse'
              : 'bg-emerald-950/60 border-emerald-800 text-emerald-400'
          }`}
        >
          {activeOrders.length} Active
        </span>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto space-y-3 max-h-[520px] pr-1">
        {activeOrders.length === 0 ? (
          <div className="py-20 flex flex-col items-center justify-center text-center">
            <div className="h-12 w-12 rounded-full bg-emerald-950/50 border border-emerald-800/60 flex items-center justify-center mb-3">
              <CheckCircle2 className="h-6 w-6 text-emerald-400" />
            </div>
            <h3 className="text-sm font-semibold text-slate-200">
              Zero Dead-Lettered Orders
            </h3>
            <p className="text-xs text-slate-400 max-w-xs mt-1">
              All transactions cleanly reconciled or in-flight. Trigger a chaos test to induce deliberate lock contention.
            </p>
          </div>
        ) : (
          activeOrders.map((item) => {
            const isActing = actingId === item.id;
            return (
              <div
                key={item.id}
                className="p-3.5 rounded-xl bg-slate-900/70 border border-rose-950/80 hover:border-rose-900/60 transition"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    <AlertOctagon className="h-4 w-4 text-rose-400 flex-shrink-0" />
                    <span className="text-xs font-mono font-bold text-slate-200">
                      Order: {item.orderId.substring(0, 14)}...
                    </span>
                  </div>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-900">
                    Retries: {item.retryCount}
                  </span>
                </div>

                {/* Reason */}
                <div className="mt-2 p-2 rounded-lg bg-[#0a0d14] border border-slate-800/80 text-[11px] font-mono text-rose-300">
                  {item.failureReason}
                </div>

                {/* Metadata & Actions */}
                <div className="mt-3 flex items-center justify-between pt-2 border-t border-slate-800/60">
                  <span className="text-[10px] text-slate-500 font-mono">
                    {new Date(item.lastAttemptAt).toLocaleTimeString()}
                  </span>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleAction(item.id, 'replay')}
                      disabled={isActing}
                      className="flex items-center gap-1 px-2.5 py-1 rounded-md bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-800/80 text-emerald-300 text-xs font-medium transition disabled:opacity-50"
                      title="Replay order reservation against fresh stock"
                    >
                      <RotateCw
                        className={`h-3 w-3 ${
                          isActing && actionType === 'replay' ? 'animate-spin' : ''
                        }`}
                      />
                      <span>Replay</span>
                    </button>

                    <button
                      onClick={() => handleAction(item.id, 'discard')}
                      disabled={isActing}
                      className="flex items-center gap-1 px-2 py-1 rounded-md bg-slate-800 hover:bg-rose-950/60 hover:text-rose-300 border border-slate-700 text-slate-400 text-xs transition disabled:opacity-50"
                      title="Discard failed order"
                    >
                      <Trash2 className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
