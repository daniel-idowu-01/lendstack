"use client";

import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { ChevronDown, ChevronRight } from "lucide-react";
import { adminApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { AuditLogResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, Label, Select } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

const entityTypes = [
  "LOAN",
  "CREDIT_ASSESSMENT",
  "REPAYMENT",
  "LENDER",
  "COLLATERAL",
  "GUARANTOR",
  "CONFIG",
  "USER",
];

function pretty(json: string | null): string {
  if (!json) return "—";
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
}

function AuditRow({ entry }: { entry: AuditLogResponse }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="rounded-lg border border-slate-100">
      <button
        className="flex w-full items-center gap-3 p-3 text-left hover:bg-slate-50"
        onClick={() => setOpen((v) => !v)}
      >
        {open ? (
          <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" aria-hidden />
        ) : (
          <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" aria-hidden />
        )}
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={entry.action.includes("OVERRIDE") ? "violet" : "slate"}>{entry.action}</Badge>
            <span className="text-sm font-medium text-slate-800">
              {entry.entityType} · {entry.entityId.slice(0, 8)}…
            </span>
          </div>
          <p className="mt-0.5 text-xs text-slate-500">
            {entry.performedBy} · {formatDateTime(entry.timestamp)}
            {entry.ipAddress ? ` · ${entry.ipAddress}` : ""}
          </p>
        </div>
      </button>
      {open && (
        <div className="border-t border-slate-100 p-3 text-xs">
          {entry.reason && (
            <p className="mb-2 text-slate-700">
              <span className="font-medium">Reason:</span> {entry.reason}
            </p>
          )}
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <p className="mb-1 font-medium text-slate-500">Before</p>
              <pre className="overflow-x-auto rounded bg-slate-50 p-2 text-slate-700">{pretty(entry.oldValue)}</pre>
            </div>
            <div>
              <p className="mb-1 font-medium text-slate-500">After</p>
              <pre className="overflow-x-auto rounded bg-slate-50 p-2 text-slate-700">{pretty(entry.newValue)}</pre>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function AdminAuditPage() {
  const [entityType, setEntityType] = useState("");
  const [performedBy, setPerformedBy] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);

  const logs = useQuery({
    queryKey: ["admin", "audit", { entityType, performedBy, from, to, page }],
    queryFn: () =>
      adminApi.auditLogs({
        entityType: entityType || undefined,
        performedBy: performedBy || undefined,
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(`${to}T23:59:59`).toISOString() : undefined,
        page,
        size: 50,
      }),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Audit log</h1>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
          <CardDescription>
            Append-only: rows can never be edited or deleted (enforced by a database trigger).
            PII in snapshots is masked.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-4">
          <div>
            <Label>Entity type</Label>
            <Select value={entityType} onChange={(e) => { setEntityType(e.target.value); setPage(0); }}>
              <option value="">All</option>
              {entityTypes.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Performed by (email or SYSTEM)</Label>
            <Input value={performedBy} onChange={(e) => { setPerformedBy(e.target.value); setPage(0); }} />
          </div>
          <div>
            <Label>From</Label>
            <Input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} />
          </div>
          <div>
            <Label>To</Label>
            <Input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-2 pt-5">
          {logs.isLoading && <LoadingState label="Loading the audit trail…" />}
          {logs.isError && (
            <ErrorState message={errorMessage(logs.error)} onRetry={() => logs.refetch()} />
          )}
          {logs.data && logs.data.items.length === 0 && (
            <EmptyState title="No audit entries match these filters" />
          )}
          {(logs.data?.items ?? []).map((entry) => (
            <AuditRow key={entry.id} entry={entry} />
          ))}
          {logs.data && logs.data.items.length > 0 && (
            <div className="flex items-center justify-between pt-2 text-sm text-slate-500">
              <span>
                Page {logs.data.page + 1} of {Math.max(logs.data.totalPages, 1)} ·{" "}
                {logs.data.totalItems} entries
              </span>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= logs.data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
