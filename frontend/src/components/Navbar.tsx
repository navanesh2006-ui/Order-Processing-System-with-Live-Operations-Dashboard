import React from 'react';
import { 
  Activity, 
  Flame, 
  ShieldCheck, 
  RotateCcw, 
  Cpu, 
  Layers, 
  Zap, 
  Wifi, 
  WifiOff 
} from 'lucide-react';
import { ThreadPoolStats } from '../types';
import { ConnectionState } from '../services/websocket';

interface NavbarProps {
  connectionState: ConnectionState;
  stats: ThreadPoolStats;
  onOpenChaosModal: () => void;
  onOpenIdempotencyModal: () => void;
  onReset: () => void;
  isResetting: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({
  connectionState,
  stats,
  onOpenChaosModal,
  onOpenIdempotencyModal,
  onReset,
  isResetting,
}) => {
  const isHealthy = connectionState === 'CONNECTED' && stats.queueDepth < 50;
  const isHighLoad = stats.queueDepth >= 50 || stats.activeThreads >= 8;

  return (
    <header className="border-b border-slate-800/80 bg-[#0d121d]/90 backdrop-blur sticky top-0 z-40 px-4 lg:px-8 py-3">
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        
        {/* Title & Brand */}
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-cyan-600 via-indigo-600 to-emerald-500 p-0.5 shadow-lg shadow-cyan-500/20">
            <div className="h-full w-full bg-[#0a0d14] rounded-[10px] flex items-center justify-center">
              <Activity className="h-5 w-5 text-cyan-400" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
                Acentra <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-950/80 border border-cyan-800/60 text-cyan-300 font-mono font-medium">OPS-CORE</span>
              </h1>
              {/* Live Connection Pill */}
              <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                connectionState === 'CONNECTED'
                  ? 'bg-emerald-950/60 border-emerald-800/60 text-emerald-400'
                  : connectionState === 'CONNECTING'
                  ? 'bg-amber-950/60 border-amber-800/60 text-amber-400'
                  : 'bg-rose-950/60 border-rose-800/60 text-rose-400'
              }`}>
                <span className={`h-1.5 w-1.5 rounded-full ${
                  connectionState === 'CONNECTED' ? 'bg-emerald-400 animate-ping' : 'bg-rose-400'
                }`} />
                {connectionState === 'CONNECTED' ? 'LIVE WS' : connectionState}
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Zero-Oversell Concurrency Engine • Optimistic Locking • DLQ Telemetry
            </p>
          </div>
        </div>

        {/* Live ThreadPool & Concurrency Proof Ticker */}
        <div className="hidden lg:flex items-center gap-6 px-4 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800">
          <div className="flex items-center gap-2">
            <Cpu className="h-4 w-4 text-indigo-400" />
            <div>
              <div className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">Active Workers</div>
              <div className="text-sm font-bold font-mono text-indigo-300">
                {stats.activeThreads} <span className="text-xs text-slate-500">/ {stats.maxPoolSize || 10}</span>
              </div>
            </div>
          </div>

          <div className="h-6 w-px bg-slate-800" />

          <div className="flex items-center gap-2">
            <Layers className="h-4 w-4 text-amber-400" />
            <div>
              <div className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">Queue Depth</div>
              <div className="text-sm font-bold font-mono text-amber-300">
                {stats.queueDepth}
              </div>
            </div>
          </div>

          <div className="h-6 w-px bg-slate-800" />

          <div className="flex items-center gap-2">
            <Zap className="h-4 w-4 text-cyan-400" />
            <div>
              <div className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">Throughput</div>
              <div className="text-sm font-bold font-mono text-cyan-300">
                {stats.ordersPerSecond.toFixed(1)} <span className="text-xs text-slate-500">ops/s</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2.5">
          {/* Chaos / Load Test Button */}
          <button
            onClick={onOpenChaosModal}
            className="flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-gradient-to-r from-rose-600 to-amber-600 hover:from-rose-500 hover:to-amber-500 text-white text-xs font-semibold shadow-lg shadow-rose-950/50 transition active:scale-95"
          >
            <Flame className="h-4 w-4 animate-bounce" />
            <span>Launch Chaos Test</span>
          </button>

          {/* Idempotency Demo Button */}
          <button
            onClick={onOpenIdempotencyModal}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 text-xs font-medium transition active:scale-95"
            title="Demonstrate duplicate order suppression"
          >
            <ShieldCheck className="h-4 w-4 text-emerald-400" />
            <span>Idempotency Demo</span>
          </button>

          {/* Reset Demo Button */}
          <button
            onClick={onReset}
            disabled={isResetting}
            className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-slate-200 text-xs transition disabled:opacity-50"
            title="Reset orders, DLQ and restore initial stock"
          >
            <RotateCcw className={`h-3.5 w-3.5 ${isResetting ? 'animate-spin text-cyan-400' : ''}`} />
            <span>Reset</span>
          </button>
        </div>

      </div>
    </header>
  );
};
