"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ShieldCheck, ShieldAlert } from "lucide-react";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import type { EmploymentStatus } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Select } from "@/components/ui/input";
import { LoadingState, ErrorState } from "@/components/page-states";

const schema = z.object({
  bvn: z
    .string()
    .regex(/^\d{11}$/, "BVN must be exactly 11 digits")
    .optional()
    .or(z.literal("")),
  nin: z
    .string()
    .regex(/^\d{11}$/, "NIN must be exactly 11 digits")
    .optional()
    .or(z.literal("")),
  employmentStatus: z.enum([
    "EMPLOYED",
    "SELF_EMPLOYED",
    "BUSINESS_OWNER",
    "UNEMPLOYED",
    "RETIRED",
    "STUDENT",
  ]),
  employerName: z.string().max(255).optional().or(z.literal("")),
  monthlyIncome: z.number({ message: "Enter your monthly income" }).positive("Enter your monthly income"),
  bankAccountNumber: z
    .string()
    .regex(/^\d{10}$/, "Account number must be 10 digits (NUBAN)")
    .optional()
    .or(z.literal("")),
  bankName: z.string().max(255).optional().or(z.literal("")),
  dateOfBirth: z.string().optional().or(z.literal("")),
  address: z.string().max(255).optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

const employmentOptions: { value: EmploymentStatus; label: string }[] = [
  { value: "EMPLOYED", label: "Employed" },
  { value: "SELF_EMPLOYED", label: "Self-employed" },
  { value: "BUSINESS_OWNER", label: "Business owner" },
  { value: "RETIRED", label: "Retired" },
  { value: "STUDENT", label: "Student" },
  { value: "UNEMPLOYED", label: "Unemployed" },
];

export default function ProfilePage() {
  const queryClient = useQueryClient();
  const profile = useQuery({ queryKey: ["borrower", "profile"], queryFn: borrowerApi.profile });

  const { register, handleSubmit, reset, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (profile.data) {
      reset({
        bvn: "",
        nin: "",
        employmentStatus: profile.data.employmentStatus ?? "EMPLOYED",
        employerName: profile.data.employerName ?? "",
        monthlyIncome: profile.data.monthlyIncome ?? undefined,
        bankAccountNumber: "",
        bankName: profile.data.bankName ?? "",
        dateOfBirth: profile.data.dateOfBirth ?? "",
        address: profile.data.address ?? "",
      });
    }
  }, [profile.data, reset]);

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      borrowerApi.updateProfile({
        ...values,
        bvn: values.bvn || undefined,
        nin: values.nin || undefined,
        bankAccountNumber: values.bankAccountNumber || undefined,
        employerName: values.employerName || undefined,
        bankName: values.bankName || undefined,
        dateOfBirth: values.dateOfBirth || undefined,
        address: values.address || undefined,
      }),
    onSuccess: () => {
      toast.success("Profile updated");
      queryClient.invalidateQueries({ queryKey: ["borrower", "profile"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  if (profile.isLoading) return <LoadingState label="Loading your profile…" />;
  if (profile.isError)
    return <ErrorState message={errorMessage(profile.error)} onRetry={() => profile.refetch()} />;
  const data = profile.data!;

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-xl font-semibold">KYC profile</h1>

      <Card className={data.bvnVerified ? "border-emerald-200 bg-emerald-50" : "border-amber-200 bg-amber-50"}>
        <CardContent className="flex items-start gap-3 p-4">
          {data.bvnVerified ? (
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" aria-hidden />
          ) : (
            <ShieldAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" aria-hidden />
          )}
          <div className="text-sm">
            <p className="font-medium">
              BVN {data.bvnMasked ? `on file: ${data.bvnMasked}` : "not provided"} —{" "}
              {data.bvnVerified ? "verified" : "not yet verified"}
            </p>
            <p className="text-slate-600">
              Your BVN is encrypted and never shown in full. Verification happens during the
              credit check on your application.
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Personal & employment details</CardTitle>
          <CardDescription>
            Leave BVN/NIN/account fields blank to keep what&apos;s already on file.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-4" noValidate>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="bvn">BVN (11 digits)</Label>
                <Input id="bvn" inputMode="numeric" placeholder={data.bvnMasked ?? "22212345678"} {...register("bvn")} />
                <FieldError message={formState.errors.bvn?.message} />
              </div>
              <div>
                <Label htmlFor="nin">NIN (optional)</Label>
                <Input id="nin" inputMode="numeric" placeholder={data.ninMasked ?? "11 digits"} {...register("nin")} />
                <FieldError message={formState.errors.nin?.message} />
              </div>
              <div>
                <Label htmlFor="employmentStatus">Employment status</Label>
                <Select id="employmentStatus" {...register("employmentStatus")}>
                  {employmentOptions.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </Select>
              </div>
              <div>
                <Label htmlFor="employerName">Employer / business name</Label>
                <Input id="employerName" {...register("employerName")} />
              </div>
              <div>
                <Label htmlFor="monthlyIncome">Monthly income (₦)</Label>
                <Input id="monthlyIncome" type="number" min="0" step="1000" {...register("monthlyIncome", { valueAsNumber: true })} />
                <FieldError message={formState.errors.monthlyIncome?.message} />
              </div>
              <div>
                <Label htmlFor="dateOfBirth">Date of birth</Label>
                <Input id="dateOfBirth" type="date" {...register("dateOfBirth")} />
              </div>
              <div>
                <Label htmlFor="bankAccountNumber">Bank account (NUBAN, for disbursement)</Label>
                <Input
                  id="bankAccountNumber"
                  inputMode="numeric"
                  placeholder={data.bankAccountMasked ?? "10 digits"}
                  {...register("bankAccountNumber")}
                />
                <FieldError message={formState.errors.bankAccountNumber?.message} />
              </div>
              <div>
                <Label htmlFor="bankName">Bank name</Label>
                <Input id="bankName" placeholder="GTBank" {...register("bankName")} />
              </div>
            </div>
            <div>
              <Label htmlFor="address">Home address</Label>
              <Input id="address" {...register("address")} />
            </div>
            <Button type="submit" loading={save.isPending}>
              Save profile
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
