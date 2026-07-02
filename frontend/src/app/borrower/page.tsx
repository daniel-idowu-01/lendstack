"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle } from "lucide-react";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, formatNairaCompact } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LoanStatusBadge } from "@/components/ui/badge";
import { Table, TBody, Td, Th, THead, Tr } from "@/components/ui/table";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

export default function BorrowerDashboard() {
  const loans = useQuery({ queryKey: ["borrower", "loans"], queryFn: () => borrowerApi.myLoans() });
  const profile = useQuery({ queryKey: ["borrower", "profile"], queryFn: borrowerApi.profile });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">My loans</h1>
        <Link href="/borrower/loans/new">
          <Button>Apply for a loan</Button>
        </Link>
      </div>

      {profile.data && !profile.data.kycComplete && (
        <Card className="border-amber-200 bg-amber-50">
          <CardContent className="flex items-start gap-3 p-4">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" aria-hidden />
            <div className="text-sm text-amber-900">
              <p className="font-medium">Complete your KYC profile</p>
              <p>
                Your BVN, employment and income details are required before any application can
                be processed (CBN customer due-diligence rules).{" "}
                <Link href="/borrower/profile" className="font-medium underline">
                  Complete it now
                </Link>
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Applications & active loans</CardTitle>
        </CardHeader>
        <CardContent>
          {loans.isLoading && <LoadingState label="Loading your loans…" />}
          {loans.isError && (
            <ErrorState message={errorMessage(loans.error)} onRetry={() => loans.refetch()} />
          )}
          {loans.data && loans.data.items.length === 0 && (
            <EmptyState
              title="No loans yet"
              hint="Start your first application — it takes about two minutes."
              action={
                <Link href="/borrower/loans/new">
                  <Button variant="outline">Start an application</Button>
                </Link>
              }
            />
          )}
          {loans.data && loans.data.items.length > 0 && (
            <Table>
              <THead>
                <Tr>
                  <Th>Reference</Th>
                  <Th>Amount</Th>
                  <Th>Tenure</Th>
                  <Th>Status</Th>
                  <Th>Applied</Th>
                  <Th />
                </Tr>
              </THead>
              <TBody>
                {loans.data.items.map((loan) => (
                  <Tr key={loan.id}>
                    <Td className="font-medium text-slate-900">{loan.reference}</Td>
                    <Td>{formatNairaCompact(loan.amount)}</Td>
                    <Td>{loan.tenureMonths} months</Td>
                    <Td>
                      <LoanStatusBadge status={loan.status} />
                    </Td>
                    <Td>{formatDate(loan.createdAt)}</Td>
                    <Td className="text-right">
                      <Link href={`/borrower/loans/${loan.id}`}>
                        <Button variant="ghost" size="sm">
                          View
                        </Button>
                      </Link>
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
