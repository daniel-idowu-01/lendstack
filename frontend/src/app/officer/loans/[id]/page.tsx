"use client";

import { useParams } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { BadgeCheck, Banknote, FileSearch, Gauge, ThumbsDown, ThumbsUp } from "lucide-react";
import { officerApi } from "@/lib/endpoints";
import { API_BASE_URL, errorMessage } from "@/lib/api";
import { getToken } from "@/lib/auth";
import { formatDate, formatDateTime, formatNaira, formatNairaCompact, humanize } from "@/lib/format";
import type { RiskTier, VerificationStatus } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Select, Textarea } from "@/components/ui/input";
import {
  GuarantorStatusBadge,
  LoanStatusBadge,
  RiskTierBadge,
  VerificationBadge,
} from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { LoanTimeline } from "@/components/loan-timeline";
import { ErrorState, LoadingState } from "@/components/page-states";

type Modal =
  | null
  | { kind: "reject" }
  | { kind: "approve" }
  | { kind: "override" }
  | { kind: "writeOff" }
  | { kind: "verifyCollateral"; collateralId: string; verdict: VerificationStatus };

export default function OfficerLoanReview() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [modal, setModal] = useState<Modal>(null);
  const [reason, setReason] = useState("");
  const [rate, setRate] = useState("");
  const [overrideScore, setOverrideScore] = useState("");
  const [overrideTier, setOverrideTier] = useState<RiskTier>("MEDIUM");

  const detail = useQuery({
    queryKey: ["officer", "loan", id],
    queryFn: () => officerApi.detail(id),
  });
  const assessments = useQuery({
    queryKey: ["officer", "loan", id, "assessments"],
    queryFn: () => officerApi.assessments(id),
  });
  const guarantors = useQuery({
    queryKey: ["officer", "loan", id, "guarantors"],
    queryFn: () => officerApi.guarantors(id),
  });
  const collaterals = useQuery({
    queryKey: ["officer", "loan", id, "collaterals"],
    queryFn: () => officerApi.collaterals(id),
  });
  const documents = useQuery({
    queryKey: ["officer", "loan", id, "documents"],
    queryFn: () => officerApi.documents(id),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["officer"] });
    setModal(null);
    setReason("");
  };
  const onError = (error: unknown) => toast.error(errorMessage(error));

  const startReview = useMutation({
    mutationFn: () => officerApi.startReview(id),
    onSuccess: () => { toast.success("Application moved to Under Review"); refresh(); },
    onError,
  });
  const runCheck = useMutation({
    mutationFn: () => officerApi.runCreditCheck(id),
    onSuccess: (a) => { toast.success(`Credit check complete — score ${a.score} (${a.riskTier})`); refresh(); },
    onError,
  });
  const override = useMutation({
    mutationFn: () => officerApi.overrideScore(id, { score: Number(overrideScore), riskTier: overrideTier, reason }),
    onSuccess: () => { toast.success("Score overridden — logged with your reason"); refresh(); },
    onError,
  });
  const proceed = useMutation({
    mutationFn: () => officerApi.proceed(id),
    onSuccess: () => { toast.success("Moved to guarantor stage"); refresh(); },
    onError,
  });
  const approve = useMutation({
    mutationFn: () => officerApi.approve(id, rate ? Number(rate) : undefined),
    onSuccess: () => { toast.success("Approved and matched to lenders"); refresh(); },
    onError,
  });
  const reject = useMutation({
    mutationFn: () => officerApi.reject(id, reason),
    onSuccess: () => { toast.success("Application rejected"); refresh(); },
    onError,
  });
  const disburse = useMutation({
    mutationFn: () => officerApi.disburse(id),
    onSuccess: () => { toast.success("Disbursed — repayment schedule generated"); refresh(); },
    onError,
  });
  const writeOff = useMutation({
    mutationFn: () => officerApi.writeOff(id, reason),
    onSuccess: () => { toast.success("Loan written off"); refresh(); },
    onError,
  });
  const verifyCollateral = useMutation({
    mutationFn: (args: { collateralId: string; status: VerificationStatus; reason?: string }) =>
      officerApi.verifyCollateral(args.collateralId, { status: args.status, reason: args.reason }),
    onSuccess: () => { toast.success("Collateral verdict recorded"); refresh(); },
    onError,
  });

  if (detail.isLoading) return <LoadingState label="Loading application…" />;
  if (detail.isError)
    return <ErrorState message={errorMessage(detail.error)} onRetry={() => detail.refetch()} />;

  const { loan, borrower, timeline } = detail.data!;
  const latestAssessment = assessments.data?.[0];

  const downloadDocument = async (documentId: string, fileName: string) => {
    const response = await fetch(`${API_BASE_URL}/api/v1/officer/documents/${documentId}`, {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (!response.ok) { toast.error("Could not download the document"); return; }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">{loan.reference}</h1>
          <p className="text-sm text-slate-500">
            {loan.borrowerName} · {formatNairaCompact(loan.amount)} · {loan.tenureMonths} months
          </p>
        </div>
        <div className="flex items-center gap-2">
          <RiskTierBadge tier={loan.riskTier} />
          <LoanStatusBadge status={loan.status} />
        </div>
      </div>

      {/* Contextual workflow actions */}
      <Card>
        <CardContent className="flex flex-wrap items-center gap-2 p-4">
          {loan.status === "SUBMITTED" && (
            <Button loading={startReview.isPending} onClick={() => startReview.mutate()}>
              <FileSearch className="h-4 w-4" aria-hidden /> Start review
            </Button>
          )}
          {(loan.status === "UNDER_REVIEW" || loan.status === "CREDIT_CHECK") && (
            <>
              <Button loading={runCheck.isPending} onClick={() => runCheck.mutate()}>
                <Gauge className="h-4 w-4" aria-hidden />
                {loan.status === "UNDER_REVIEW" ? "Run credit check" : "Re-run credit check"}
              </Button>
              <Button variant="destructive" onClick={() => setModal({ kind: "reject" })}>
                <ThumbsDown className="h-4 w-4" aria-hidden /> Reject
              </Button>
            </>
          )}
          {loan.status === "CREDIT_CHECK" && (
            <>
              <Button variant="outline" onClick={() => setModal({ kind: "override" })}>
                Override score
              </Button>
              <Button
                variant="secondary"
                loading={proceed.isPending}
                onClick={() => proceed.mutate()}
              >
                Proceed to guarantors
              </Button>
            </>
          )}
          {loan.status === "PENDING_COLLATERAL" && (
            <Button onClick={() => setModal({ kind: "approve" })}>
              <ThumbsUp className="h-4 w-4" aria-hidden /> Approve loan
            </Button>
          )}
          {loan.status === "APPROVED" && (
            <Button loading={disburse.isPending} onClick={() => disburse.mutate()}>
              <Banknote className="h-4 w-4" aria-hidden /> Disburse funds
            </Button>
          )}
          {loan.status === "DEFAULTED" && (
            <Button variant="destructive" onClick={() => setModal({ kind: "writeOff" })}>
              Write off
            </Button>
          )}
          {["PENDING_GUARANTOR"].includes(loan.status) && (
            <p className="text-sm text-slate-500">
              Waiting on guarantor acceptance(s) — the loan advances automatically when all
              required guarantors accept, or returns to review on decline/expiry.
            </p>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Borrower</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <dt className="text-slate-500">Name</dt>
                <dd className="font-medium">{borrower.fullName}</dd>
                <dt className="text-slate-500">Email</dt>
                <dd>{borrower.email}</dd>
                <dt className="text-slate-500">Phone</dt>
                <dd>{borrower.phone ?? "—"}</dd>
                <dt className="text-slate-500">Employment</dt>
                <dd>
                  {humanize(borrower.employmentStatus)}
                  {borrower.employerName ? ` — ${borrower.employerName}` : ""}
                </dd>
                <dt className="text-slate-500">Monthly income</dt>
                <dd>{formatNaira(borrower.monthlyIncome)}</dd>
                <dt className="text-slate-500">BVN</dt>
                <dd className="flex items-center gap-1.5">
                  {borrower.bvnMasked ?? "not on file"}
                  {borrower.bvnVerified && (
                    <BadgeCheck className="h-4 w-4 text-emerald-600" aria-label="verified" />
                  )}
                </dd>
                <dt className="text-slate-500">Bank</dt>
                <dd>{borrower.bankName ?? "—"}</dd>
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Credit score</CardTitle>
              <CardDescription>
                Rule-based breakdown — every point is explainable to the borrower.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {!latestAssessment && (
                <p className="text-sm text-slate-500">No credit check has been run yet.</p>
              )}
              {latestAssessment && (
                <div className="space-y-3">
                  <div className="flex items-center gap-3">
                    <span className="text-3xl font-bold">{latestAssessment.score}</span>
                    <RiskTierBadge tier={latestAssessment.riskTier} />
                    {latestAssessment.overridden && (
                      <span className="rounded-full bg-violet-100 px-2 py-0.5 text-xs font-medium text-violet-800">
                        Overridden
                      </span>
                    )}
                  </div>
                  {latestAssessment.overrideReason && (
                    <p className="text-sm text-slate-600">
                      Override reason: {latestAssessment.overrideReason}
                    </p>
                  )}
                  {latestAssessment.breakdown && (
                    <ul className="space-y-1.5">
                      {latestAssessment.breakdown.map((rule) => (
                        <li key={rule.rule} className="text-sm">
                          <div className="flex items-center justify-between">
                            <span className="text-slate-700">{humanize(rule.rule)}</span>
                            <span className="font-medium">
                              {rule.points}/{rule.maxPoints}
                            </span>
                          </div>
                          <div className="mt-0.5 h-1.5 rounded-full bg-slate-100">
                            <div
                              className="h-1.5 rounded-full bg-emerald-500"
                              style={{ width: `${(rule.points / rule.maxPoints) * 100}%` }}
                            />
                          </div>
                          <p className="mt-0.5 text-xs text-slate-500">{rule.detail}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                  <p className="text-xs text-slate-400">
                    Assessed {formatDateTime(latestAssessment.createdAt)}
                    {latestAssessment.assessedBy ? ` by ${latestAssessment.assessedBy}` : ""}
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Guarantors</CardTitle>
              <CardDescription>
                {loan.guarantorsRequired === 0
                  ? "None required at this amount."
                  : `${loan.guarantorsRequired} acceptance(s) required before approval.`}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {(guarantors.data ?? []).length === 0 && (
                <p className="text-sm text-slate-500">No guarantors named.</p>
              )}
              {(guarantors.data ?? []).map((g) => (
                <div key={g.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3">
                  <div>
                    <p className="text-sm font-medium">{g.fullName}</p>
                    <p className="text-xs text-slate-500">
                      {g.relationship ? `${g.relationship} · ` : ""}
                      {g.occupation ? `${g.occupation} · ` : ""}
                      income {formatNaira(g.monthlyIncome)}
                    </p>
                    {g.declineReason && (
                      <p className="text-xs text-red-600">Declined: {g.declineReason}</p>
                    )}
                    {g.status === "PENDING" && g.expiresAt && (
                      <p className="text-xs text-slate-400">responds by {formatDateTime(g.expiresAt)}</p>
                    )}
                  </div>
                  <GuarantorStatusBadge status={g.status} />
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Collateral</CardTitle>
              <CardDescription>
                {loan.collateralRequired
                  ? "Required — must be VERIFIED before approval/disbursement."
                  : "Not required at this loan amount."}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {(collaterals.data ?? []).length === 0 && (
                <p className="text-sm text-slate-500">Nothing declared.</p>
              )}
              {(collaterals.data ?? []).map((c) => (
                <div key={c.id} className="rounded-lg border border-slate-100 p-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium">
                      {humanize(c.type)} · {formatNairaCompact(c.estimatedValue)}
                    </p>
                    <VerificationBadge status={c.verificationStatus} />
                  </div>
                  <p className="mt-1 text-xs text-slate-500">{c.description}</p>
                  {c.verifiedBy && (
                    <p className="mt-1 text-xs text-slate-400">
                      {humanize(c.verificationStatus)} by {c.verifiedBy} · {formatDate(c.verifiedAt)}
                    </p>
                  )}
                  {c.verificationStatus === "UNVERIFIED" && (
                    <div className="mt-2 flex gap-2">
                      <Button
                        size="sm"
                        onClick={() =>
                          verifyCollateral.mutate({ collateralId: c.id, status: "VERIFIED" })
                        }
                        loading={verifyCollateral.isPending}
                      >
                        Verify
                      </Button>
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() =>
                          setModal({ kind: "verifyCollateral", collateralId: c.id, verdict: "REJECTED" })
                        }
                      >
                        Reject
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Documents</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {(documents.data ?? []).length === 0 && (
                <p className="text-sm text-slate-500">No documents uploaded.</p>
              )}
              {(documents.data ?? []).map((d) => (
                <div key={d.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3 text-sm">
                  <div>
                    <p className="font-medium">{d.fileName}</p>
                    <p className="text-xs text-slate-500">
                      {humanize(d.docType)} · {(d.sizeBytes / 1024).toFixed(0)} KB ·{" "}
                      {formatDate(d.uploadedAt)}
                    </p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => downloadDocument(d.id, d.fileName)}>
                    Download
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        <Card className="h-fit">
          <CardHeader>
            <CardTitle>Audited timeline</CardTitle>
            <CardDescription>Includes the acting officer for each step.</CardDescription>
          </CardHeader>
          <CardContent>
            <LoanTimeline entries={timeline} />
          </CardContent>
        </Card>
      </div>

      {/* Reject */}
      <Dialog
        open={modal?.kind === "reject"}
        onClose={() => setModal(null)}
        title="Reject this application?"
        description="The borrower will see your reason. This action is final (only an admin can override it)."
      >
        <div className="space-y-3">
          <div>
            <Label>Reason (required, min 10 characters)</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
            <FieldError message={reason && reason.length < 10 ? "At least 10 characters" : undefined} />
          </div>
          <Button
            variant="destructive"
            className="w-full"
            disabled={reason.length < 10}
            loading={reject.isPending}
            onClick={() => reject.mutate()}
          >
            Confirm rejection
          </Button>
        </div>
      </Dialog>

      {/* Approve */}
      <Dialog
        open={modal?.kind === "approve"}
        onClose={() => setModal(null)}
        title="Approve this loan?"
        description="Approval fixes the interest rate and immediately commits funding from matched lenders."
      >
        <div className="space-y-3">
          <div>
            <Label>Annual interest rate % (leave blank for the configured default)</Label>
            <Input
              type="number"
              step="0.5"
              min="1"
              placeholder="e.g. 24"
              value={rate}
              onChange={(e) => setRate(e.target.value)}
            />
            <p className="mt-1 text-xs text-slate-500">
              Must be within the CBN cap. Reducing-balance method applies.
            </p>
          </div>
          <Button className="w-full" loading={approve.isPending} onClick={() => approve.mutate()}>
            Confirm approval
          </Button>
        </div>
      </Dialog>

      {/* Score override */}
      <Dialog
        open={modal?.kind === "override"}
        onClose={() => setModal(null)}
        title="Override the credit score"
        description="The rule-based assessment stays on record; your override is logged with your name and reason."
      >
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Score (0–100)</Label>
              <Input
                type="number"
                min="0"
                max="100"
                value={overrideScore}
                onChange={(e) => setOverrideScore(e.target.value)}
              />
            </div>
            <div>
              <Label>Risk tier</Label>
              <Select value={overrideTier} onChange={(e) => setOverrideTier(e.target.value as RiskTier)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="DECLINED">Declined</option>
              </Select>
            </div>
          </div>
          <div>
            <Label>Written reason (required, min 10 characters)</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <Button
            className="w-full"
            disabled={reason.length < 10 || overrideScore === ""}
            loading={override.isPending}
            onClick={() => override.mutate()}
          >
            Record override
          </Button>
        </div>
      </Dialog>

      {/* Write off */}
      <Dialog
        open={modal?.kind === "writeOff"}
        onClose={() => setModal(null)}
        title="Write off this loan?"
        description="Recognizes the outstanding debt as unrecoverable. This is terminal."
      >
        <div className="space-y-3">
          <div>
            <Label>Reason (required, min 10 characters)</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <Button
            variant="destructive"
            className="w-full"
            disabled={reason.length < 10}
            loading={writeOff.isPending}
            onClick={() => writeOff.mutate()}
          >
            Confirm write-off
          </Button>
        </div>
      </Dialog>

      {/* Reject collateral */}
      <Dialog
        open={modal?.kind === "verifyCollateral"}
        onClose={() => setModal(null)}
        title="Reject this collateral?"
        description="The borrower will see your reason and can submit different collateral."
      >
        <div className="space-y-3">
          <div>
            <Label>Reason (required)</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <Button
            variant="destructive"
            className="w-full"
            disabled={reason.length < 3}
            loading={verifyCollateral.isPending}
            onClick={() =>
              modal?.kind === "verifyCollateral" &&
              verifyCollateral.mutate({
                collateralId: modal.collateralId,
                status: "REJECTED",
                reason,
              })
            }
          >
            Confirm rejection
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
