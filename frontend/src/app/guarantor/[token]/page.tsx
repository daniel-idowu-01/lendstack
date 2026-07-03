"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Landmark, ShieldCheck, ShieldX } from "lucide-react";
import { guarantorPublicApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDateTime, formatNairaCompact } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label, Textarea } from "@/components/ui/input";
import { GuarantorStatusBadge } from "@/components/ui/badge";
import { ErrorState, LoadingState } from "@/components/page-states";


export default function GuarantorResponsePage() {
  const { token } = useParams<{ token: string }>();
  const queryClient = useQueryClient();
  const [declining, setDeclining] = useState(false);
  const [declineReason, setDeclineReason] = useState("");

  const invite = useQuery({
    queryKey: ["guarantor-invite", token],
    queryFn: () => guarantorPublicApi.view(token),
    retry: 1,
  });

  const respond = useMutation({
    mutationFn: (accept: boolean) =>
      guarantorPublicApi.respond(token, accept, accept ? undefined : declineReason || undefined),
    onSuccess: (_, accepted) => {
      toast.success(accepted ? "Thank you — your acceptance has been recorded." : "Your response has been recorded.");
      queryClient.invalidateQueries({ queryKey: ["guarantor-invite", token] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-b from-emerald-50 to-slate-50 p-4">
      <Card className="w-full max-w-lg">
        <CardHeader className="items-center text-center">
          <div className="mb-1 flex items-center gap-2">
            <Landmark className="h-7 w-7 text-emerald-600" aria-hidden />
            <span className="text-xl font-bold">LendStack</span>
          </div>
          <CardTitle>Loan guarantee request</CardTitle>
          <CardDescription>
            Standing as guarantor means you may be called upon if the borrower fails to repay.
            Please decide carefully.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {invite.isLoading && <LoadingState label="Loading the request…" />}
          {invite.isError && (
            <ErrorState message="This link is invalid or has been removed. If you believe this is a mistake, ask the borrower to re-add you." />
          )}
          {invite.data && (
            <div className="space-y-4">
              <div className="rounded-lg bg-slate-50 p-4 text-sm">
                <p>
                  Hello <span className="font-medium">{invite.data.guarantorName}</span>,
                </p>
                <p className="mt-2">
                  <span className="font-medium">{invite.data.borrowerName}</span> has named you as
                  guarantor for a personal loan of{" "}
                  <span className="font-semibold">{formatNairaCompact(invite.data.loanAmount)}</span>{" "}
                  over <span className="font-semibold">{invite.data.tenureMonths} months</span>.
                </p>
                <p className="mt-2 text-slate-600">Purpose: “{invite.data.purpose}”</p>
                {invite.data.expiresAt && invite.data.status === "PENDING" && (
                  <p className="mt-2 text-xs text-slate-500">
                    Respond by {formatDateTime(invite.data.expiresAt)} — after that this request
                    expires automatically.
                  </p>
                )}
              </div>

              {invite.data.status !== "PENDING" ? (
                <div className="flex flex-col items-center gap-2 py-4">
                  <GuarantorStatusBadge status={invite.data.status} />
                  <p className="text-sm text-slate-600">
                    {invite.data.status === "ACCEPTED"
                      ? "You have accepted this request. Thank you!"
                      : invite.data.status === "DECLINED"
                        ? "You declined this request."
                        : "This request expired without a response."}
                  </p>
                </div>
              ) : declining ? (
                <div className="space-y-3">
                  <div>
                    <Label htmlFor="declineReason">Reason (optional)</Label>
                    <Textarea
                      id="declineReason"
                      value={declineReason}
                      onChange={(e) => setDeclineReason(e.target.value)}
                      placeholder="e.g. I'm not in a position to guarantee this amount right now"
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="destructive"
                      className="flex-1"
                      loading={respond.isPending}
                      onClick={() => respond.mutate(false)}
                    >
                      <ShieldX className="h-4 w-4" aria-hidden />
                      Confirm decline
                    </Button>
                    <Button variant="outline" className="flex-1" onClick={() => setDeclining(false)}>
                      Back
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="flex gap-2">
                  <Button
                    className="flex-1"
                    loading={respond.isPending}
                    onClick={() => respond.mutate(true)}
                  >
                    <ShieldCheck className="h-4 w-4" aria-hidden />
                    Accept — I&apos;ll stand as guarantor
                  </Button>
                  <Button variant="outline" className="flex-1" onClick={() => setDeclining(true)}>
                    Decline
                  </Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
