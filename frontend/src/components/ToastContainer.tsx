import React from 'react';
import { AlertTriangle, Flame, ShieldAlert, X } from 'lucide-react';

export interface ToastItem {
  id: string;
  type: 'LOW_STOCK' | 'CHAOS' | 'DLQ' | 'INFO';
  title: string;
  message: string;
}

interface ToastContainerProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

export const ToastContainer: React.FC<ToastContainerProps> = ({ toasts, onDismiss }) => {
  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm pointer-events-none">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`pointer-events-auto p-3.5 rounded-xl border shadow-xl backdrop-blur-md flex items-start gap-3 transition-all animate-in slide-in-from-bottom-5 duration-200 ${
            t.type === 'LOW_STOCK'
              ? 'bg-amber-950/90 border-amber-800 text-amber-200'
              : t.type === 'CHAOS'
              ? 'bg-rose-950/90 border-rose-800 text-rose-200'
              : t.type === 'DLQ'
              ? 'bg-red-950/90 border-red-800 text-red-200'
              : 'bg-slate-900/90 border-slate-800 text-slate-200'
          }`}
        >
          {t.type === 'LOW_STOCK' && <AlertTriangle className="h-5 w-5 text-amber-400 flex-shrink-0 mt-0.5" />}
          {t.type === 'CHAOS' && <Flame className="h-5 w-5 text-rose-400 flex-shrink-0 mt-0.5" />}
          {t.type === 'DLQ' && <ShieldAlert className="h-5 w-5 text-red-400 flex-shrink-0 mt-0.5" />}

          <div className="flex-1 text-xs">
            <div className="font-bold text-slate-100">{t.title}</div>
            <div className="mt-0.5 opacity-90">{t.message}</div>
          </div>

          <button
            onClick={() => onDismiss(t.id)}
            className="text-slate-400 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  );
};
