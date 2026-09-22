import React from 'react';
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  Tooltip, 
  ResponsiveContainer, 
  CartesianGrid 
} from 'recharts';
import { LineChart as LineChartIcon } from 'lucide-react';
import { ThroughputPoint } from '../types';

interface AnalyticsPanelProps {
  data: ThroughputPoint[];
}

export const AnalyticsPanel: React.FC<AnalyticsPanelProps> = ({ data }) => {
  return (
    <div className="bg-[#0f1422] rounded-2xl border border-slate-800 p-5 shadow-xl">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <LineChartIcon className="h-5 w-5 text-emerald-400" />
          <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
            Real-Time Engine Telemetry
          </h2>
        </div>
        <div className="flex items-center gap-4 text-xs font-mono">
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-cyan-400" />
            <span className="text-slate-300">Throughput (TPS)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            <span className="text-slate-300">Confirmed Orders</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-rose-400" />
            <span className="text-slate-300">DLQ Failures</span>
          </div>
        </div>
      </div>

      <div className="h-44 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id="tpsGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#38bdf8" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#38bdf8" stopOpacity={0.0} />
              </linearGradient>
              <linearGradient id="confirmedGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#34d399" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#34d399" stopOpacity={0.0} />
              </linearGradient>
              <linearGradient id="failedGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#f43f5e" stopOpacity={0.0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
            <XAxis 
              dataKey="time" 
              stroke="#64748b" 
              fontSize={10} 
              tickLine={false} 
            />
            <YAxis 
              stroke="#64748b" 
              fontSize={10} 
              tickLine={false} 
              allowDecimals={false} 
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#0a0d14',
                borderColor: '#1e293b',
                borderRadius: '8px',
                fontSize: '11px',
                fontFamily: 'monospace',
              }}
            />
            <Area
              type="monotone"
              dataKey="tps"
              name="Throughput"
              stroke="#38bdf8"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#tpsGradient)"
            />
            <Area
              type="monotone"
              dataKey="confirmed"
              name="Confirmed"
              stroke="#34d399"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#confirmedGradient)"
            />
            <Area
              type="monotone"
              dataKey="failed"
              name="Failed/DLQ"
              stroke="#f43f5e"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#failedGradient)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
