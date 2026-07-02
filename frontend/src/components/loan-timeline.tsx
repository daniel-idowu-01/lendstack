"use client";

import { CheckCircle2, Circle } from "lucide-react";
import { formatDateTime, humanize } from "@/lib/format";
import type { TimelineEntry } from "@/lib/types";

/** Vertical visual timeline of every state a loan has passed through. */
export function LoanTimeline({ entries }: { entries: TimelineEntry[] }) {
  if (entries.length === 0) {
    return <p className="py-4 text-sm text-slate-500">No activity yet.</p>;
  }
  return (
    <ol className="relative ml-2 space-y-5 border-l border-slate-200 pl-5">
      {entries.map((entry, index) => {
        const latest = index === entries.length - 1;
        return (
          <li key={`${entry.timestamp}-${index}`} className="relative">
            <span className="absolute -left-[27px] top-0.5 bg-white">
              {latest ? (
                <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden />
              ) : (
                <Circle className="h-4 w-4 text-slate-300" aria-hidden />
              )}
            </span>
            <p className="text-sm font-medium text-slate-900">
              {entry.toStatus
                ? entry.fromStatus
                  ? `${humanize(entry.fromStatus)} → ${humanize(entry.toStatus)}`
                  : humanize(entry.toStatus)
                : humanize(entry.action)}
            </p>
            <p className="text-xs text-slate-500">{formatDateTime(entry.timestamp)}</p>
            {entry.reason && <p className="mt-1 text-xs text-slate-600">{entry.reason}</p>}
            {entry.performedBy && (
              <p className="mt-0.5 text-xs text-slate-400">by {entry.performedBy}</p>
            )}
          </li>
        );
      })}
    </ol>
  );
}
