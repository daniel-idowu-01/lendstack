import * as React from "react";
import { cn } from "@/lib/cn";
import { humanize } from "@/lib/format";
import type { GuarantorStatus, InstallmentStatus, LoanStatus, RiskTier, VerificationStatus } from "@/lib/types";

export function Badge({
  className,
  tone = "slate",
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & {
  tone?: "slate" | "green" | "amber" | "red" | "blue" | "violet";
}) {
  const tones: Record<string, string> = {
    slate: "bg-slate-100 text-slate-700",
    green: "bg-emerald-100 text-emerald-800",
    amber: "bg-amber-100 text-amber-800",
    red: "bg-red-100 text-red-700",
    blue: "bg-blue-100 text-blue-800",
    violet: "bg-violet-100 text-violet-800",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        tones[tone],
        className,
      )}
      {...props}
    />
  );
}

const loanStatusTones: Record<LoanStatus, "slate" | "green" | "amber" | "red" | "blue" | "violet"> = {
  DRAFT: "slate",
  SUBMITTED: "blue",
  UNDER_REVIEW: "blue",
  CREDIT_CHECK: "violet",
  PENDING_GUARANTOR: "amber",
  PENDING_COLLATERAL: "amber",
  APPROVED: "green",
  DISBURSED: "green",
  ACTIVE: "green",
  DELINQUENT: "amber",
  DEFAULTED: "red",
  CLOSED: "slate",
  REJECTED: "red",
  WRITTEN_OFF: "red",
};

export function LoanStatusBadge({ status }: { status: LoanStatus }) {
  return <Badge tone={loanStatusTones[status]}>{humanize(status)}</Badge>;
}

export function RiskTierBadge({ tier }: { tier: RiskTier | null }) {
  if (!tier) return <Badge>—</Badge>;
  const tones = { LOW: "green", MEDIUM: "amber", HIGH: "red", DECLINED: "red" } as const;
  return <Badge tone={tones[tier]}>{humanize(tier)} risk</Badge>;
}

export function InstallmentStatusBadge({ status }: { status: InstallmentStatus }) {
  const tones = { PENDING: "slate", PAID: "green", OVERDUE: "red", WAIVED: "violet" } as const;
  return <Badge tone={tones[status]}>{humanize(status)}</Badge>;
}

export function GuarantorStatusBadge({ status }: { status: GuarantorStatus }) {
  const tones = { PENDING: "amber", ACCEPTED: "green", DECLINED: "red", EXPIRED: "slate" } as const;
  return <Badge tone={tones[status]}>{humanize(status)}</Badge>;
}

export function VerificationBadge({ status }: { status: VerificationStatus }) {
  const tones = { UNVERIFIED: "amber", VERIFIED: "green", REJECTED: "red" } as const;
  return <Badge tone={tones[status]}>{humanize(status)}</Badge>;
}
