"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { ConfigResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ErrorState, LoadingState } from "@/components/page-states";

function ConfigRow({ entry }: { entry: ConfigResponse }) {
  const queryClient = useQueryClient();
  const [value, setValue] = useState(entry.value);
  const dirty = value !== entry.value;

  const save = useMutation({
    mutationFn: () => adminApi.updateConfig(entry.key, value),
    onSuccess: () => {
      toast.success(`${entry.key} updated — applies to all new decisions immediately`);
      queryClient.invalidateQueries({ queryKey: ["admin", "config"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <p className="font-mono text-sm font-medium text-slate-900">{entry.key}</p>
        <p className="text-xs text-slate-500">{entry.description}</p>
        <p className="mt-0.5 text-xs text-slate-400">
          Last set by {entry.updatedBy ?? "SYSTEM"} · {formatDateTime(entry.updatedAt)}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <Input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          className="w-36"
          inputMode={entry.valueType === "NUMBER" ? "decimal" : undefined}
        />
        <Button size="sm" disabled={!dirty} loading={save.isPending} onClick={() => save.mutate()}>
          Save
        </Button>
      </div>
    </div>
  );
}

export default function AdminConfigPage() {
  const config = useQuery({ queryKey: ["admin", "config"], queryFn: adminApi.config });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">System configuration</h1>
      <Card>
        <CardHeader>
          <CardTitle>CBN & lending parameters</CardTitle>
          <CardDescription>
            The database is the source of truth: when the CBN revises a guideline, change it here
            — no redeploy. Every edit is audit-logged with old and new values. Existing loans keep
            the terms they were approved with.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {config.isLoading && <LoadingState label="Loading configuration…" />}
          {config.isError && (
            <ErrorState message={errorMessage(config.error)} onRetry={() => config.refetch()} />
          )}
          {(config.data ?? []).map((entry) => (
            <ConfigRow key={entry.key} entry={entry} />
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
