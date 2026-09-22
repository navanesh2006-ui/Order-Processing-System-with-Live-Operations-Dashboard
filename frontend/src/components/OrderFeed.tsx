import React, { useState } from 'react';
import { 
  Radio, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Crown, 
  ChevronDown, 
  ChevronUp, 
  Filter, 
  Sparkles,
  GitCommit
} from 'lucide-react';
import { Order, OrderStatus } from '../types';

interface OrderFeedProps {
  orders: Order[];
}

export const OrderFeed: React.FC<OrderFeedProps> = ({ orders }) => {
  const [filter, setFilter] = useState<'ALL' | 'VIP' | 'CONFIRMED' | 'FAILED'>('ALL');
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);

  const filteredOrders = orders.filter((order) => {
    if (filter === 'VIP') return order.priority === 'VIP';
    if (filter === 'CONFIRMED') return order.status === 'CONFIRMED';
    if (filter === 'FAILED') return order.status === 'DEAD_LETTERED' || order.status === 'FAILED';
    return true;
  });

  const toggleExpand = (id: string) => {
    setExpandedOrderId(expandedOrderId === id ? null : id);
  };

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'RECEIVED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-sky-950/70 border border-sky-800 text-sky-400">
            <Clock className="h-3 w-3" /> RECEIVED
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-950/70 border border-amber-800 text-amber-400 animate-pulse">
            <Radio className="h-3 w-3 animate-spin" /> PROCESSING
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-950/70 border border-emerald-800 text-emerald-400">
            <CheckCircle2 className="h-3 w-3" /> CONFIRMED
          </span>
        );
      case 'FAILED':
      case 'DEAD_LETTERED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-950/70 border border-rose-800 text-rose-400">
            <XCircle className="h-3 w-3" /> DEAD-LETTERED
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="bg-[#0f1422] rounded-2xl border border-slate-800 p-5 shadow-xl flex flex-col h-full">
      {/* Header & Filter Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2">
          <Radio className="h-5 w-5 text-indigo-400 animate-pulse" />
          <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
            Live Ingestion Feed
          </h2>
          <span className="text-xs font-mono font-medium px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
            {filteredOrders.length}
          </span>
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-lg border border-slate-800 text-xs">
          {(['ALL', 'VIP', 'CONFIRMED', 'FAILED'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setFilter(tab)}
              className={`px-2.5 py-1 rounded-md font-medium transition ${
                filter === tab
                  ? 'bg-indigo-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {tab === 'FAILED' ? 'DLQ' : tab}
            </button>
          ))}
        </div>
      </div>

      {/* Order Feed Stream */}
      <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 max-h-[520px]">
        {filteredOrders.length === 0 ? (
          <div className="py-16 text-center text-slate-500 text-xs">
            No orders found matching the filter. Run a load test to flood the feed!
          </div>
        ) : (
          filteredOrders.map((order) => {
            const isExpanded = expandedOrderId === order.id;
            return (
              <div
                key={order.id}
                className={`rounded-xl border transition-all duration-200 ${
                  order.status === 'CONFIRMED'
                    ? 'bg-slate-900/40 border-slate-800/80 hover:border-emerald-800/60'
                    : order.status === 'DEAD_LETTERED'
                    ? 'bg-rose-950/10 border-rose-900/40 hover:border-rose-800'
                    : 'bg-slate-900/40 border-slate-800/80'
                }`}
              >
                {/* Order Item Card */}
                <div
                  onClick={() => toggleExpand(order.id)}
                  className="p-3.5 cursor-pointer flex items-center justify-between gap-3 select-none"
                >
                  <div className="flex items-center gap-3">
                    {/* Priority Indicator */}
                    {order.priority === 'VIP' ? (
                      <div className="h-8 w-8 rounded-lg bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400" title="VIP Priority Lane">
                        <Crown className="h-4 w-4" />
                      </div>
                    ) : (
                      <div className="h-8 w-8 rounded-lg bg-slate-800/80 border border-slate-700/60 flex items-center justify-center text-slate-400">
                        <Sparkles className="h-4 w-4" />
                      </div>
                    )}

                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono font-bold text-slate-200">
                          {order.productSku}
                        </span>
                        <span className="text-[11px] text-slate-400">
                          × {order.quantity} units
                        </span>
                        {order.priority === 'VIP' && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-amber-950 text-amber-300 border border-amber-800">
                            VIP
                          </span>
                        )}
                        {order.duplicateSuppressed && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">
                            IDEMPOTENT CACHE
                          </span>
                        )}
                      </div>
                      <div className="text-[10px] font-mono text-slate-500 truncate max-w-[200px] sm:max-w-xs mt-0.5">
                        ID: {order.id.substring(0, 18)}...
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    {getStatusBadge(order.status)}
                    <button className="text-slate-500 hover:text-slate-300">
                      {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                {/* Expanded Timeline Drawer */}
                {isExpanded && (
                  <div className="px-4 pb-3.5 pt-2 border-t border-slate-800/60 bg-slate-950/40 rounded-b-xl">
                    <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <GitCommit className="h-3.5 w-3.5 text-cyan-400" />
                      State Machine Audit Log ({order.events?.length || 0} transitions)
                    </div>

                    <div className="relative pl-4 space-y-2 border-l border-slate-800 ml-1.5 my-2">
                      {order.events && order.events.length > 0 ? (
                        order.events.map((event, idx) => (
                          <div key={idx} className="relative text-xs">
                            <span className="absolute -left-[21px] top-1 h-2 w-2 rounded-full bg-cyan-400 ring-4 ring-slate-900" />
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-slate-200">
                                {event.fromStatus ? `${event.fromStatus} → ` : ''}{event.toStatus}
                              </span>
                              <span className="text-[10px] font-mono text-slate-500">
                                {new Date(event.createdAt).toLocaleTimeString()}
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-400 mt-0.5">
                              {event.reason}
                            </p>
                          </div>
                        ))
                      ) : (
                        <div className="text-xs text-slate-500">Initial event queued.</div>
                      )}
                    </div>

                    <div className="mt-2 pt-2 border-t border-slate-800/50 flex items-center justify-between text-[11px] font-mono text-slate-500">
                      <span>Idempotency Key:</span>
                      <span className="text-slate-400 truncate max-w-[240px]">
                        {order.idempotencyKey}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
