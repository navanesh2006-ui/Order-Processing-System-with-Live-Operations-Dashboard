import React from 'react';
import { Cpu, Layers, CheckCircle2, TrendingUp } from 'lucide-react';
import { ThreadPoolStats } from '../types';

interface ThreadPoolPanelProps {
  stats: ThreadPoolStats;
}

export const ThreadPoolPanel: React.FC<ThreadPoolPanelProps> = ({ stats }) => {
  const activePercent = stats.maxPoolSize > 0 ? (stats.activeThreads / stats.maxPoolSize) * 100 : 0;
  const queuePercent = Math.min((stats.queueDepth / 100) * 100, 100);

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {/* 1. Active Worker Threads */}
      <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800/80 relative overflow-hidden group">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Worker Threads</span>
          <Cpu className="h-4 w-4 text-indigo-400" />
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold font-mono text-white">{stats.activeThreads}</span>
          <span className="text-xs text-slate-500 font-mono">/ {stats.maxPoolSize} max</span>
        </div>
        <div className="mt-2 w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
          <div 
            className="bg-indigo-500 h-1.5 rounded-full transition-all duration-300"
            style={{ width: `${activePercent}%` }}
          />
        </div>
      </div>

      {/* 2. Priority Queue Depth */}
      <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800/80 relative overflow-hidden group">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Queue Depth</span>
          <Layers className="h-4 w-4 text-amber-400" />
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold font-mono text-amber-300">{stats.queueDepth}</span>
          <span className="text-xs text-slate-500 font-mono">in buffer</span>
        </div>
        <div className="mt-2 w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
          <div 
            className={`h-1.5 rounded-full transition-all duration-300 ${
              stats.queueDepth > 20 ? 'bg-rose-500' : stats.queueDepth > 5 ? 'bg-amber-500' : 'bg-emerald-500'
            }`}
            style={{ width: `${queuePercent}%` }}
          />
        </div>
      </div>

      {/* 3. Completed Tasks */}
      <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800/80 relative overflow-hidden group">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Completed Orders</span>
          <CheckCircle2 className="h-4 w-4 text-emerald-400" />
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold font-mono text-emerald-300">
            {stats.completedTasks.toLocaleString()}
          </span>
        </div>
        <div className="mt-2 text-[10px] text-slate-500">
          Thread pool executions
        </div>
      </div>

      {/* 4. Real-time TPS */}
      <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800/80 relative overflow-hidden group">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Throughput (TPS)</span>
          <TrendingUp className="h-4 w-4 text-cyan-400" />
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold font-mono text-cyan-300">
            {stats.ordersPerSecond.toFixed(1)}
          </span>
          <span className="text-xs text-slate-500">orders/sec</span>
        </div>
        <div className="mt-2 text-[10px] text-slate-500">
          Calculated rolling window
        </div>
      </div>
    </div>
  );
};
