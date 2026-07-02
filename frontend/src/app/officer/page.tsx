"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { officerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, formatNairaCompact } from "@/lib/format";
import type { LoanStatus, RiskTier } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input, Label, Select } from "@/components/ui/input";
import { LoanStatusBadge, RiskTierBadge } from "@/components/ui/badge";
import { Table, TBody, Td, Th, THead, Tr } from "@/components/ui/table";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

const statuses: LoanStatus[] = [
  "SUBMITTED",
  "UNDER_REVIEW",
  "CREDIT_CHECK",
  "PENDING_GUARANTOR",
  "PENDING_COLLATERAL",
  "APPROVED",
  "DISBURSED",
  "ACTIVE",
  "DELINQUENT",
  "DEFAULTED",
  "CLOSED",
  "REJECTED",
  "WRITTEN_OFF",
];

export default function OfficerQueue() {
  const [status, setStatus] = useState<LoanStatus | "">("");
  const [riskTier, setRiskTier] = useState<RiskTier | "">("");
  const [minAmount, setMinAmount] = useState("");
  const [maxAmount, setMaxAmount] = useState("");
  const [page, setPage] = useState(0);

  const queue = useQuery({
    queryKey: ["officer", "queue", { status, riskTier, minAmount, maxAmount, page }],
    queryFn: () =>
      officerApi.queue({
        status: status || undefined,
        riskTier: riskTier || undefined,
        minAmount: minAmount ? Number(minAmount) : undefined,
        maxAmount: maxAmount ? Number(maxAmount) : undefined,
        page,
        size: 20,
      }),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Application queue</h1>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
          <CardDescription>Oldest applications first, so nothing sits unattended.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-4">
          <div>
            <Label>State</Label>
            <Select value={status} onChange={(e) => { setStatus(e.target.value as LoanStatus | ""); setPage(0); }}>
              <option value="">All (except drafts)</option>
              {statuses.map((s) => (
                <option key={s} value={s}>
                  {s.replaceAll("_", " ")}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Risk tier</Label>
            <Select value={riskTier} onChange={(e) => { setRiskTier(e.target.value as RiskTier | ""); setPage(0); }}>
              <option value="">Any</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="DECLINED">Declined</option>
            </Select>
          </div>
          <div>
            <Label>Min amount (₦)</Label>
            <Input type="number" value={minAmount} onChange={(e) => { setMinAmount(e.target.value); setPage(0); }} />
          </div>
          <div>
            <Label>Max amount (₦)</Label>
            <Input type="number" value={maxAmount} onChange={(e) => { setMaxAmount(e.target.value); setPage(0); }} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="pt-5">
          {queue.isLoading && <LoadingState label="Loading the queue…" />}
          {queue.isError && (
            <ErrorState message={errorMessage(queue.error)} onRetry={() => queue.refetch()} />
          )}
          {queue.data && queue.data.items.length === 0 && (
            <EmptyState title="Queue is clear" hint="No applications match these filters." />
          )}
          {queue.data && queue.data.items.length > 0 && (
            <>
              <Table>
                <THead>
                  <Tr>
                    <Th>Reference</Th>
                    <Th>Borrower</Th>
                    <Th>Amount</Th>
                    <Th>Score</Th>
                    <Th>Risk</Th>
                    <Th>Status</Th>
                    <Th>Submitted</Th>
                    <Th />
                  </Tr>
                </THead>
                <TBody>
                  {queue.data.items.map((loan) => (
                    <Tr key={loan.id}>
                      <Td className="font-medium text-slate-900">{loan.reference}</Td>
                      <Td>{loan.borrowerName}</Td>
                      <Td>{formatNairaCompact(loan.amount)}</Td>
                      <Td>{loan.creditScore ?? "—"}</Td>
                      <Td>
                        <RiskTierBadge tier={loan.riskTier} />
                      </Td>
                      <Td>
                        <LoanStatusBadge status={loan.status} />
                      </Td>
                      <Td>{formatDate(loan.submittedAt)}</Td>
                      <Td className="text-right">
                        <Link href={`/officer/loans/${loan.id}`}>
                          <Button variant="ghost" size="sm">
                            Review
                          </Button>
                        </Link>
                      </Td>
                    </Tr>
                  ))}
                </TBody>
              </Table>
              <div className="mt-3 flex items-center justify-between text-sm text-slate-500">
                <span>
                  Page {queue.data.page + 1} of {Math.max(queue.data.totalPages, 1)} ·{" "}
                  {queue.data.totalItems} application(s)
                </span>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= queue.data.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
