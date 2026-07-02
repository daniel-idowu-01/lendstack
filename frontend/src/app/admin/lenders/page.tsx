"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { PlusCircle, Wallet } from "lucide-react";
import { adminApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, formatNaira, formatNairaCompact, humanize } from "@/lib/format";
import type { LenderResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Select } from "@/components/ui/input";
import { Badge, RiskTierBadge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, TBody, Td, Th, THead, Tr } from "@/components/ui/table";
import { EmptyState, ErrorState, LoadingState } from "@/components/page-states";

const lenderSchema = z.object({
  name: z.string().min(2, "Enter the lender's name"),
  type: z.enum(["INDIVIDUAL", "INSTITUTION"]),
  email: z.string().email("Enter a valid email"),
  maxExposure: z.number({ message: "Enter the maximum exposure" }).positive("Enter the maximum exposure"),
  preferredRiskTier: z.enum(["LOW", "MEDIUM", "HIGH"]),
});
type LenderForm = z.infer<typeof lenderSchema>;

export default function AdminLendersPage() {
  const queryClient = useQueryClient();
  const [registerOpen, setRegisterOpen] = useState(false);
  const [topUpTarget, setTopUpTarget] = useState<LenderResponse | null>(null);
  const [topUpAmount, setTopUpAmount] = useState("");
  const [portfolioTarget, setPortfolioTarget] = useState<LenderResponse | null>(null);

  const lenders = useQuery({ queryKey: ["admin", "lenders"], queryFn: () => adminApi.lenders() });
  const portfolio = useQuery({
    queryKey: ["admin", "lender-portfolio", portfolioTarget?.id],
    queryFn: () => adminApi.lenderPortfolio(portfolioTarget!.id),
    enabled: !!portfolioTarget,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "lenders"] });

  const form = useForm<LenderForm>({
    resolver: zodResolver(lenderSchema),
    defaultValues: { type: "INSTITUTION", preferredRiskTier: "MEDIUM" },
  });

  const register = useMutation({
    mutationFn: (values: LenderForm) => adminApi.registerLender(values),
    onSuccess: () => {
      toast.success("Lender registered — top up their wallet so they can fund loans");
      setRegisterOpen(false);
      form.reset();
      refresh();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const topUp = useMutation({
    mutationFn: () => adminApi.topUpLender(topUpTarget!.id, Number(topUpAmount)),
    onSuccess: () => {
      toast.success("Wallet credited (stub settlement — audit-logged)");
      setTopUpTarget(null);
      setTopUpAmount("");
      refresh();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const toggleActive = useMutation({
    mutationFn: (lender: LenderResponse) =>
      adminApi.updateLender(lender.id, { active: !lender.active }),
    onSuccess: () => { toast.success("Lender updated"); refresh(); },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Lenders</h1>
        <Button onClick={() => setRegisterOpen(true)}>
          <PlusCircle className="h-4 w-4" aria-hidden /> Register lender
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Funding sources</CardTitle>
          <CardDescription>
            Approved loans are split across lenders by risk appetite, wallet balance and
            exposure headroom. Repayments flow back pro-rata.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {lenders.isLoading && <LoadingState label="Loading lenders…" />}
          {lenders.isError && (
            <ErrorState message={errorMessage(lenders.error)} onRetry={() => lenders.refetch()} />
          )}
          {lenders.data && lenders.data.items.length === 0 && (
            <EmptyState
              title="No lenders yet"
              hint="Register at least one funded lender or approvals will fail with INSUFFICIENT_LENDER_FUNDING."
            />
          )}
          {lenders.data && lenders.data.items.length > 0 && (
            <Table>
              <THead>
                <Tr>
                  <Th>Name</Th>
                  <Th>Type</Th>
                  <Th>Wallet</Th>
                  <Th>Exposure / limit</Th>
                  <Th>Risk appetite</Th>
                  <Th>Status</Th>
                  <Th />
                </Tr>
              </THead>
              <TBody>
                {lenders.data.items.map((lender) => (
                  <Tr key={lender.id}>
                    <Td className="font-medium text-slate-900">{lender.name}</Td>
                    <Td>{humanize(lender.type)}</Td>
                    <Td>{formatNairaCompact(lender.walletBalance)}</Td>
                    <Td>
                      {formatNairaCompact(lender.currentExposure)} /{" "}
                      {formatNairaCompact(lender.maxExposure)}
                    </Td>
                    <Td>
                      <RiskTierBadge tier={lender.preferredRiskTier} />
                    </Td>
                    <Td>
                      <Badge tone={lender.active ? "green" : "slate"}>
                        {lender.active ? "Active" : "Inactive"}
                      </Badge>
                    </Td>
                    <Td>
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => setPortfolioTarget(lender)}>
                          Portfolio
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => setTopUpTarget(lender)}>
                          <Wallet className="h-4 w-4" aria-hidden /> Top up
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => toggleActive.mutate(lender)}
                        >
                          {lender.active ? "Deactivate" : "Activate"}
                        </Button>
                      </div>
                    </Td>
                  </Tr>
                ))}
              </TBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog
        open={registerOpen}
        onClose={() => setRegisterOpen(false)}
        title="Register a lender"
        description="Risk appetite is a ceiling: a MEDIUM lender funds LOW and MEDIUM loans."
      >
        <form onSubmit={form.handleSubmit((v) => register.mutate(v))} className="space-y-3" noValidate>
          <div>
            <Label>Name</Label>
            <Input {...form.register("name")} />
            <FieldError message={form.formState.errors.name?.message} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Type</Label>
              <Select {...form.register("type")}>
                <option value="INSTITUTION">Institution</option>
                <option value="INDIVIDUAL">Individual</option>
              </Select>
            </div>
            <div>
              <Label>Risk appetite</Label>
              <Select {...form.register("preferredRiskTier")}>
                <option value="LOW">Low only</option>
                <option value="MEDIUM">Up to medium</option>
                <option value="HIGH">Up to high</option>
              </Select>
            </div>
          </div>
          <div>
            <Label>Email</Label>
            <Input type="email" {...form.register("email")} />
            <FieldError message={form.formState.errors.email?.message} />
          </div>
          <div>
            <Label>Maximum exposure (₦)</Label>
            <Input type="number" min="0" {...form.register("maxExposure", { valueAsNumber: true })} />
            <FieldError message={form.formState.errors.maxExposure?.message} />
          </div>
          <Button type="submit" className="w-full" loading={register.isPending}>
            Register lender
          </Button>
        </form>
      </Dialog>

      <Dialog
        open={!!topUpTarget}
        onClose={() => setTopUpTarget(null)}
        title={`Top up ${topUpTarget?.name ?? ""}`}
        description="Stub settlement: credits the wallet directly and audit-logs it. Production would reconcile real bank inflows."
      >
        <div className="space-y-3">
          <div>
            <Label>Amount (₦)</Label>
            <Input
              type="number"
              min="0"
              value={topUpAmount}
              onChange={(e) => setTopUpAmount(e.target.value)}
            />
          </div>
          <Button
            className="w-full"
            disabled={!topUpAmount || Number(topUpAmount) <= 0}
            loading={topUp.isPending}
            onClick={() => topUp.mutate()}
          >
            Credit wallet
          </Button>
        </div>
      </Dialog>

      <Dialog
        open={!!portfolioTarget}
        onClose={() => setPortfolioTarget(null)}
        title={`${portfolioTarget?.name ?? ""} — portfolio`}
        description="Every loan slice this lender has funded."
        className="max-w-2xl"
      >
        {portfolio.isLoading && <LoadingState label="Loading portfolio…" />}
        {portfolio.data && portfolio.data.items.length === 0 && (
          <p className="py-6 text-center text-sm text-slate-500">No fundings yet.</p>
        )}
        {portfolio.data && portfolio.data.items.length > 0 && (
          <Table>
            <THead>
              <Tr>
                <Th>Loan</Th>
                <Th>Funded</Th>
                <Th>Principal repaid</Th>
                <Th>Interest earned</Th>
                <Th>Date</Th>
              </Tr>
            </THead>
            <TBody>
              {portfolio.data.items.map((funding) => (
                <Tr key={funding.id}>
                  <Td className="font-medium">{funding.loanReference}</Td>
                  <Td>{formatNaira(funding.amount)}</Td>
                  <Td>{formatNaira(funding.principalRepaid)}</Td>
                  <Td>{formatNaira(funding.interestEarned)}</Td>
                  <Td>{formatDate(funding.createdAt)}</Td>
                </Tr>
              ))}
            </TBody>
          </Table>
        )}
      </Dialog>
    </div>
  );
}
