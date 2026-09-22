import React, { useState } from 'react';
import { Flame, X, Play, Zap, CheckCircle2, AlertTriangle } from 'lucide-react';
import { ChaosTestRequest, ChaosTestResponse } from '../types';

interface ChaosModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRunTest: (req: ChaosTestRequest) => Promise<ChaosTestResponse>;
}

export const ChaosModal: React.FC<ChaosModalProps> = ({ isOpen, onClose, onRunTest }) => {
  const [totalOrders, setTotalOrders] = useState<number>(100);
  const [concurrencyLevel, setConcurrencyLevel] = useState<number>(10);
  const [includeScarceStock, setIncludeScarceStock] = useState<boolean>(true);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [result, setResult] = useState<ChaosTestResponse | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsRunning(true);
      setResult(null);
      const res = await onRunTest({
        totalOrders,
        concurrencyLevel,
        includeScarceStock,
      });
      setResult(res);
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div className="bg-[#0f1422] border border-slate-700 w-full max-w-md rounded-2xl p-6 shadow-2xl relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-rose-600 to-amber-500 flex items-center justify-center shadow-lg shadow-rose-950/50">
            <Flame className="h-6 w-6 text-white" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Chaos Concurrency Simulator
            </h2>
            <p className="text-xs text-slate-400">
              Flood the engine with concurrent order bursts to stress optimistic locks
            </p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Total Orders Slider */}
          <div>
            <div className="flex justify-between text-xs font-medium mb-1">
              <span className="text-slate-300">Total Order Volume:</span>
              <span className="font-mono text-cyan-400 font-bold">{totalOrders} orders</span>
            </div>
            <input
              type="range"
              min="20"
              max="300"
              step="10"
              value={totalOrders}
              onChange={(e) => setTotalOrders(Number(e.target.value))}
              disabled={isRunning}
              className="w-full accent-cyan-400 bg-slate-800 rounded-lg cursor-pointer"
            />
            <div className="flex justify-between text-[10px] text-slate-500 mt-0.5">
              <span>20</span>
              <span>150</span>
              <span>300</span>
            </div>
          </div>

          {/* Concurrency Level Slider */}
          <div>
            <div className="flex justify-between text-xs font-medium mb-1">
              <span className="text-slate-300">Concurrent Client Workers:</span>
              <span className="font-mono text-indigo-400 font-bold">{concurrencyLevel} threads</span>
            </div>
            <input
              type="range"
              min="2"
              max="25"
              step="1"
              value={concurrencyLevel}
              onChange={(e) => setConcurrencyLevel(Number(e.target.value))}
              disabled={isRunning}
              className="w-full accent-indigo-400 bg-slate-800 rounded-lg cursor-pointer"
            />
            <div className="flex justify-between text-[10px] text-slate-500 mt-0.5">
              <span>2 threads</span>
              <span>12 threads</span>
              <span>25 threads</span>
            </div>
          </div>

          {/* Scarce Stock Bias */}
          <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-start gap-3">
            <input
              type="checkbox"
              id="scarceToggle"
              checked={includeScarceStock}
              onChange={(e) => setIncludeScarceStock(e.target.checked)}
              disabled={isRunning}
              className="mt-1 h-4 w-4 rounded border-slate-700 bg-slate-800 text-cyan-500 focus:ring-0"
            />
            <label htmlFor="scarceToggle" className="text-xs cursor-pointer">
              <span className="font-bold text-slate-200 block">
                Target Scarce Stock (RTX 5090 Blackwell)
              </span>
              <span className="text-slate-400 block text-[11px] mt-0.5">
                Forces 40% of orders to contend for 3 available GPU units, provoking optimistic lock retries and DLQ routing live!
              </span>
            </label>
          </div>

          {/* Result Alert */}
          {result && (
            <div className="p-3.5 rounded-xl bg-emerald-950/40 border border-emerald-800/60 text-xs">
              <div className="flex items-center gap-1.5 font-bold text-emerald-400 mb-1">
                <CheckCircle2 className="h-4 w-4" /> Chaos Test Complete!
              </div>
              <div className="font-mono text-slate-300 text-[11px]">
                Dispatched {result.totalOrdersSubmitted} orders in {result.durationMs}ms ({Math.round((result.totalOrdersSubmitted / (result.durationMs / 1000)))} req/sec). Watch the live feed & DLQ panel update!
              </div>
            </div>
          )}

          {/* Launch Button */}
          <button
            type="submit"
            disabled={isRunning}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-rose-600 via-amber-600 to-cyan-600 hover:opacity-90 font-bold text-white text-xs flex items-center justify-center gap-2 shadow-lg shadow-rose-950/50 transition disabled:opacity-50"
          >
            {isRunning ? (
              <>
                <Zap className="h-4 w-4 animate-spin" />
                <span>Bombarding Engine...</span>
              </>
            ) : (
              <>
                <Play className="h-4 w-4" />
                <span>Fire {totalOrders} Orders Now</span>
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
