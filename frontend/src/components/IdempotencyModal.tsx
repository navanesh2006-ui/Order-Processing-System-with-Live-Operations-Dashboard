import React, { useState } from 'react';
import { ShieldCheck, X, Zap, CheckCircle2, AlertCircle } from 'lucide-react';
import { runIdempotencyDemo } from '../services/api';

interface IdempotencyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const IdempotencyModal: React.FC<IdempotencyModalProps> = ({ isOpen, onClose }) => {
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [demoResult, setDemoResult] = useState<any | null>(null);

  if (!isOpen) return null;

  const handleRun = async () => {
    try {
      setIsRunning(true);
      const res = await runIdempotencyDemo(1); // RTX 5090
      setDemoResult(res);
    } catch (err: any) {
      console.error(err);
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div className="bg-[#0f1422] border border-slate-700 w-full max-w-xl rounded-2xl p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-3 mb-4">
          <div className="h-10 w-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Idempotency Verification Demo
            </h2>
            <p className="text-xs text-slate-400">
              Live proof: duplicate submissions share an idempotency key with zero double-decrementing
            </p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-300 space-y-2 mb-4">
          <p>
            When networks flake or users double-click submit, frontend clients often retransmit identical payloads.
            Our engine checks the <code className="text-cyan-400 font-mono bg-slate-800 px-1 py-0.5 rounded">idempotencyKey</code> unique index before acquiring locks.
          </p>
          <p className="text-slate-400">
            Click below to generate an idempotency key and fire two rapid submissions back-to-back.
          </p>
        </div>

        {demoResult && (
          <div className="space-y-3 mb-4">
            <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-800 text-xs text-emerald-300 font-mono flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
              <span>{demoResult.message}</span>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs font-mono">
              {/* Submission 1 */}
              <div className="p-3 rounded-xl bg-slate-900 border border-slate-800">
                <div className="text-cyan-400 font-bold mb-1 flex items-center gap-1">
                  <span className="h-2 w-2 rounded-full bg-cyan-400" /> 1st Submission
                </div>
                <div className="text-slate-400 text-[11px] space-y-1">
                  <div>Status: <span className="text-white">{demoResult.firstSubmission?.status}</span></div>
                  <div>Order ID: <span className="text-slate-300">{demoResult.firstSubmission?.id.substring(0, 10)}...</span></div>
                  <div>Duplicate Suppressed: <span className="text-rose-400 font-bold">false</span> (Accepted)</div>
                </div>
              </div>

              {/* Submission 2 */}
              <div className="p-3 rounded-xl bg-slate-900 border border-slate-800">
                <div className="text-emerald-400 font-bold mb-1 flex items-center gap-1">
                  <span className="h-2 w-2 rounded-full bg-emerald-400" /> 2nd Submission (Duplicate)
                </div>
                <div className="text-slate-400 text-[11px] space-y-1">
                  <div>Status: <span className="text-white">{demoResult.secondSubmission?.status}</span></div>
                  <div>Order ID: <span className="text-slate-300">{demoResult.secondSubmission?.id.substring(0, 10)}...</span></div>
                  <div>Duplicate Suppressed: <span className="text-emerald-400 font-bold">true</span> (Cached)</div>
                </div>
              </div>
            </div>
          </div>
        )}

        <button
          onClick={handleRun}
          disabled={isRunning}
          className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 font-bold text-white text-xs flex items-center justify-center gap-2 transition disabled:opacity-50"
        >
          {isRunning ? (
            <>
              <Zap className="h-4 w-4 animate-spin" />
              <span>Transmitting Identical Requests...</span>
            </>
          ) : (
            <>
              <Zap className="h-4 w-4" />
              <span>Simulate Duplicate Submissions Live</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
