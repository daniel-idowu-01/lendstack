"use client";

import { useParams } from "next/navigation";
import { useQuery, useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { CalendarClock, Wallet } from "lucide-react";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, formatNaira, formatNairaCompact } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { InstallmentStatusBadge, LoanStatusBadge } from "@/components/ui/badge";
import { Table, TBody, Td, Th, THead, Tr } from "@/components/ui/table";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

export default function RepaymentDashboard() {
  const { id } = useParams<{ id: string }>();
  const schedule = useQuery({
    queryKey: ["borrower", "loan", id, "schedule"],
    queryFn: () => borrowerApi.schedule(id),
  });

  const pay = useMutation({
    mutationFn: (installmentId: string) => borrowerApi.payInstallment(installmentId),
    onSuccess: (init) => {
      toast.info("Taking you to Paystack to complete payment…");
      window.location.href = init.authorizationUrl;
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  if (schedule.isLoading) return <LoadingState label="Loading your repayment schedule…" />;
  if (schedule.isError)
    return <ErrorState message={errorMessage(schedule.error)} onRetry={() => schedule.refetch()} />;

  const data = schedule.data!;
  if (data.installments.length === 0) {
    return (
      <EmptyState
        title="No repayment schedule yet"
        hint="Your schedule is generated the moment the loan is disbursed."
      />
    );
  }

  const nextPayable = data.installments.find(
    (i) => i.status === "PENDING" || i.status === "OVERDUE",
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">Repayments — {data.loanReference}</h1>
          <p className="text-sm text-slate-500">
            {formatNairaCompact(data.loanAmount)} at {data.interestRateAnnual}% p.a., reducing balance
          </p>
        </div>
        <LoanStatusBadge status={data.loanStatus} />
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <Wallet className="h-8 w-8 rounded-lg bg-emerald-50 p-1.5 text-emerald-600" aria-hidden />
            <div>
              <p className="text-xs text-slate-500">Total outstanding</p>
              <p className="text-lg font-semibold">{formatNaira(data.totalOutstanding)}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <Wallet className="h-8 w-8 rounded-lg bg-blue-50 p-1.5 text-blue-600" aria-hidden />
            <div>
              <p className="text-xs text-slate-500">Principal remaining</p>
              <p className="text-lg font-semibold">{formatNaira(data.outstandingPrincipal)}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <CalendarClock className="h-8 w-8 rounded-lg bg-amber-50 p-1.5 text-amber-600" aria-hidden />
            <div>
              <p className="text-xs text-slate-500">Next due date</p>
              <p className="text-lg font-semibold">{formatDate(data.nextDueDate)}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Full schedule</CardTitle>
          <CardDescription>
            Late installments accrue a daily penalty after a 3-day grace period. Pay with card,
            bank transfer or USSD via Paystack.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <THead>
              <Tr>
                <Th>#</Th>
                <Th>Due date</Th>
                <Th>Principal</Th>
                <Th>Interest</Th>
                <Th>Penalty</Th>
                <Th>Total</Th>
                <Th>Status</Th>
                <Th />
              </Tr>
            </THead>
            <TBody>
              {data.installments.map((installment) => {
                const payable =
                  installment.status === "PENDING" || installment.status === "OVERDUE";
                return (
                  <Tr key={installment.id}>
                    <Td>{installment.installmentNumber}</Td>
                    <Td>{formatDate(installment.dueDate)}</Td>
                    <Td>{formatNaira(installment.principalDue)}</Td>
                    <Td>{formatNaira(installment.interestDue)}</Td>
                    <Td className={installment.penaltyDue > 0 ? "text-red-600" : ""}>
                      {formatNaira(installment.penaltyDue)}
                    </Td>
                    <Td className="font-medium">
                      {formatNaira(installment.totalDue + installment.penaltyDue)}
                    </Td>
                    <Td>
                      <InstallmentStatusBadge status={installment.status} />
                    </Td>
                    <Td className="text-right">
                      {payable && (
                        <Button
                          size="sm"
                          loading={pay.isPending && pay.variables === installment.id}
                          onClick={() => pay.mutate(installment.id)}
                          variant={installment.id === nextPayable?.id ? "default" : "outline"}
                        >
                          Pay now
                        </Button>
                      )}
                    </Td>
                  </Tr>
                );
              })}
            </TBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
