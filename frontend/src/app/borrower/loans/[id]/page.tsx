"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { FileText, Send, UserPlus, Building2 } from "lucide-react";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatDate, formatNairaCompact } from "@/lib/format";
import type { CollateralType } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Select, Textarea } from "@/components/ui/input";
import { GuarantorStatusBadge, LoanStatusBadge, VerificationBadge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { LoanTimeline } from "@/components/loan-timeline";
import { ErrorState, LoadingState } from "@/components/page-states";

const guarantorSchema = z.object({
  fullName: z.string().min(2, "Enter the guarantor's full name"),
  email: z.string().email("Enter a valid email"),
  phone: z.string().optional().or(z.literal("")),
  relationship: z.string().optional().or(z.literal("")),
  occupation: z.string().optional().or(z.literal("")),
  monthlyIncome: z.number().positive().optional(),
});
type GuarantorForm = z.infer<typeof guarantorSchema>;

const collateralSchema = z.object({
  type: z.enum(["PROPERTY", "VEHICLE", "FIXED_DEPOSIT", "EQUIPMENT"]),
  description: z.string().min(10, "Describe the collateral (at least 10 characters)"),
  estimatedValue: z.number({ message: "Enter the estimated value" }).positive("Enter the estimated value"),
});
type CollateralForm = z.infer<typeof collateralSchema>;

const collateralTypes: { value: CollateralType; label: string }[] = [
  { value: "PROPERTY", label: "Property" },
  { value: "VEHICLE", label: "Vehicle" },
  { value: "FIXED_DEPOSIT", label: "Fixed deposit" },
  { value: "EQUIPMENT", label: "Equipment" },
];

export default function LoanDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [guarantorOpen, setGuarantorOpen] = useState(false);
  const [collateralOpen, setCollateralOpen] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const [docType, setDocType] = useState("ID_CARD");

  const detail = useQuery({
    queryKey: ["borrower", "loan", id],
    queryFn: () => borrowerApi.loanDetail(id),
  });
  const guarantors = useQuery({
    queryKey: ["borrower", "loan", id, "guarantors"],
    queryFn: () => borrowerApi.guarantors(id),
  });
  const collaterals = useQuery({
    queryKey: ["borrower", "loan", id, "collaterals"],
    queryFn: () => borrowerApi.collaterals(id),
  });
  const documents = useQuery({
    queryKey: ["borrower", "loan", id, "documents"],
    queryFn: () => borrowerApi.documents(id),
  });

  const refreshAll = () => queryClient.invalidateQueries({ queryKey: ["borrower"] });

  const submit = useMutation({
    mutationFn: () => borrowerApi.submitLoan(id),
    onSuccess: () => {
      toast.success("Application submitted — we'll keep you posted at every step.");
      refreshAll();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const addGuarantor = useMutation({
    mutationFn: (values: GuarantorForm) =>
      borrowerApi.addGuarantor(id, {
        ...values,
        phone: values.phone || undefined,
        relationship: values.relationship || undefined,
        occupation: values.occupation || undefined,
      }),
    onSuccess: () => {
      toast.success("Guarantor added — they'll receive an email to accept or decline.");
      setGuarantorOpen(false);
      refreshAll();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const declareCollateral = useMutation({
    mutationFn: (values: CollateralForm) => borrowerApi.declareCollateral(id, values),
    onSuccess: () => {
      toast.success("Collateral declared — upload its documents so an officer can verify it.");
      setCollateralOpen(false);
      refreshAll();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const upload = useMutation({
    mutationFn: (file: File) => borrowerApi.uploadDocument(id, docType, file),
    onSuccess: () => {
      toast.success("Document uploaded");
      refreshAll();
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const guarantorForm = useForm<GuarantorForm>({ resolver: zodResolver(guarantorSchema) });
  const collateralForm = useForm<CollateralForm>({
    resolver: zodResolver(collateralSchema),
    defaultValues: { type: "PROPERTY" },
  });

  if (detail.isLoading) return <LoadingState label="Loading application…" />;
  if (detail.isError)
    return <ErrorState message={errorMessage(detail.error)} onRetry={() => detail.refetch()} />;

  const { loan, timeline } = detail.data!;
  const isDraft = loan.status === "DRAFT";
  const repayable = ["ACTIVE", "DELINQUENT", "DISBURSED", "CLOSED"].includes(loan.status);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">{loan.reference}</h1>
          <p className="text-sm text-slate-500">
            {formatNairaCompact(loan.amount)} · {loan.tenureMonths} months
            {loan.interestRateAnnual ? ` · ${loan.interestRateAnnual}% p.a. (reducing balance)` : ""}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <LoanStatusBadge status={loan.status} />
          {repayable && (
            <Link href={`/borrower/loans/${loan.id}/repayments`}>
              <Button variant="outline" size="sm">
                Repayment dashboard
              </Button>
            </Link>
          )}
          {isDraft && (
            <Button size="sm" loading={submit.isPending} onClick={() => submit.mutate()}>
              <Send className="h-4 w-4" aria-hidden />
              Submit application
            </Button>
          )}
        </div>
      </div>

      {loan.status === "REJECTED" && loan.rejectionReason && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4 text-sm text-red-800">
            <p className="font-medium">This application was rejected</p>
            <p>{loan.rejectionReason}</p>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Application timeline</CardTitle>
            <CardDescription>Every step, from our immutable audit trail.</CardDescription>
          </CardHeader>
          <CardContent>
            <LoanTimeline entries={timeline} />
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader className="flex-row items-center justify-between">
              <div>
                <CardTitle>Guarantors</CardTitle>
                <CardDescription>
                  {loan.guarantorsRequired === 0
                    ? "Not required at this amount"
                    : `${loan.guarantorsRequired} required — each gets 72 hours to respond`}
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" onClick={() => setGuarantorOpen(true)}>
                <UserPlus className="h-4 w-4" aria-hidden />
                Add
              </Button>
            </CardHeader>
            <CardContent className="space-y-3">
              {(guarantors.data ?? []).length === 0 && (
                <p className="text-sm text-slate-500">No guarantors added yet.</p>
              )}
              {(guarantors.data ?? []).map((g) => (
                <div key={g.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3">
                  <div>
                    <p className="text-sm font-medium">{g.fullName}</p>
                    <p className="text-xs text-slate-500">
                      {g.email}
                      {g.expiresAt && g.status === "PENDING"
                        ? ` · responds by ${formatDate(g.expiresAt)}`
                        : ""}
                    </p>
                    {g.declineReason && (
                      <p className="text-xs text-red-600">Declined: {g.declineReason}</p>
                    )}
                  </div>
                  <GuarantorStatusBadge status={g.status} />
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex-row items-center justify-between">
              <div>
                <CardTitle>Collateral</CardTitle>
                <CardDescription>
                  {loan.collateralRequired
                    ? "Required for this amount — must be verified before disbursement"
                    : "Not required at this amount"}
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" onClick={() => setCollateralOpen(true)}>
                <Building2 className="h-4 w-4" aria-hidden />
                Declare
              </Button>
            </CardHeader>
            <CardContent className="space-y-3">
              {(collaterals.data ?? []).length === 0 && (
                <p className="text-sm text-slate-500">No collateral declared.</p>
              )}
              {(collaterals.data ?? []).map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3">
                  <div>
                    <p className="text-sm font-medium">
                      {collateralTypes.find((t) => t.value === c.type)?.label} ·{" "}
                      {formatNairaCompact(c.estimatedValue)}
                    </p>
                    <p className="text-xs text-slate-500">{c.description}</p>
                    {c.rejectionReason && (
                      <p className="text-xs text-red-600">Rejected: {c.rejectionReason}</p>
                    )}
                  </div>
                  <VerificationBadge status={c.verificationStatus} />
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Documents</CardTitle>
              <CardDescription>ID card, payslip, bank statement, collateral papers — PDF/JPG/PNG, max 10MB.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <Select value={docType} onChange={(e) => setDocType(e.target.value)} className="w-44">
                  <option value="ID_CARD">ID card</option>
                  <option value="PAYSLIP">Payslip</option>
                  <option value="BANK_STATEMENT">Bank statement</option>
                  <option value="UTILITY_BILL">Utility bill</option>
                  <option value="COLLATERAL_DOC">Collateral document</option>
                  <option value="OTHER">Other</option>
                </Select>
                <input
                  ref={fileInput}
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) upload.mutate(file);
                    e.target.value = "";
                  }}
                />
                <Button
                  variant="outline"
                  size="sm"
                  loading={upload.isPending}
                  onClick={() => fileInput.current?.click()}
                >
                  <FileText className="h-4 w-4" aria-hidden />
                  Upload
                </Button>
              </div>
              {(documents.data ?? []).map((d) => (
                <div key={d.id} className="flex items-center justify-between rounded-lg border border-slate-100 p-3 text-sm">
                  <div>
                    <p className="font-medium">{d.fileName}</p>
                    <p className="text-xs text-slate-500">
                      {d.docType.replaceAll("_", " ").toLowerCase()} · {formatDate(d.uploadedAt)}
                    </p>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      </div>

      <Dialog
        open={guarantorOpen}
        onClose={() => setGuarantorOpen(false)}
        title="Add a guarantor"
        description="They'll receive an email with an accept/decline link (valid 72 hours)."
      >
        <form
          onSubmit={guarantorForm.handleSubmit((v) => addGuarantor.mutate(v))}
          className="space-y-3"
          noValidate
        >
          <div>
            <Label>Full name</Label>
            <Input {...guarantorForm.register("fullName")} />
            <FieldError message={guarantorForm.formState.errors.fullName?.message} />
          </div>
          <div>
            <Label>Email</Label>
            <Input type="email" {...guarantorForm.register("email")} />
            <FieldError message={guarantorForm.formState.errors.email?.message} />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label>Phone</Label>
              <Input {...guarantorForm.register("phone")} />
            </div>
            <div>
              <Label>Relationship</Label>
              <Input placeholder="e.g. Sister, Colleague" {...guarantorForm.register("relationship")} />
            </div>
            <div>
              <Label>Occupation</Label>
              <Input {...guarantorForm.register("occupation")} />
            </div>
            <div>
              <Label>Monthly income (₦)</Label>
              <Input
                type="number"
                min="0"
                {...guarantorForm.register("monthlyIncome", {
                  setValueAs: (v) => (v === "" || v === null ? undefined : Number(v)),
                })}
              />
            </div>
          </div>
          <Button type="submit" className="w-full" loading={addGuarantor.isPending}>
            Add guarantor
          </Button>
        </form>
      </Dialog>

      <Dialog
        open={collateralOpen}
        onClose={() => setCollateralOpen(false)}
        title="Declare collateral"
        description="A loan officer will verify it against the documents you upload."
      >
        <form
          onSubmit={collateralForm.handleSubmit((v) => declareCollateral.mutate(v))}
          className="space-y-3"
          noValidate
        >
          <div>
            <Label>Type</Label>
            <Select {...collateralForm.register("type")}>
              {collateralTypes.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Estimated value (₦)</Label>
            <Input type="number" min="0" {...collateralForm.register("estimatedValue", { valueAsNumber: true })} />
            <FieldError message={collateralForm.formState.errors.estimatedValue?.message} />
          </div>
          <div>
            <Label>Description</Label>
            <Textarea
              placeholder="e.g. Toyota Corolla 2016, registration LSD-123-XY"
              {...collateralForm.register("description")}
            />
            <FieldError message={collateralForm.formState.errors.description?.message} />
          </div>
          <Button type="submit" className="w-full" loading={declareCollateral.isPending}>
            Declare collateral
          </Button>
        </form>
      </Dialog>
    </div>
  );
}
