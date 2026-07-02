"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatNairaCompact } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label, Textarea } from "@/components/ui/input";

const schema = z.object({
  amount: z
    .number({ message: "Enter the loan amount" })
    .min(50_000, "Minimum loan is ₦50,000")
    .max(10_000_000, "Maximum loan is ₦10,000,000"),
  tenureMonths: z
    .number({ message: "Enter the tenure" })
    .int()
    .min(1, "Minimum tenure is 1 month")
    .max(24, "Maximum tenure is 24 months (CBN personal-loan guideline)"),
  purpose: z.string().min(10, "Tell us what the loan is for (at least 10 characters)").max(1000),
});
type FormValues = z.infer<typeof schema>;

export default function NewLoanPage() {
  const router = useRouter();
  const { register, handleSubmit, watch, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { amount: 500_000, tenureMonths: 12 },
  });
  const amount = watch("amount");

  const create = useMutation({
    mutationFn: (values: FormValues) => borrowerApi.createLoan(values),
    onSuccess: (loan) => {
      toast.success("Draft saved — add guarantors/collateral if needed, then submit.");
      router.push(`/borrower/loans/${loan.id}`);
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-xl font-semibold">Apply for a loan</h1>
      <Card>
        <CardHeader>
          <CardTitle>Loan details</CardTitle>
          <CardDescription>
            Your application is saved as a draft first — you can add guarantors, declare
            collateral and upload documents before submitting it for review.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit((v) => create.mutate(v))} className="space-y-4" noValidate>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="amount">Amount (₦)</Label>
                <Input id="amount" type="number" step="10000" min="50000" {...register("amount", { valueAsNumber: true })} />
                <FieldError message={formState.errors.amount?.message} />
              </div>
              <div>
                <Label htmlFor="tenureMonths">Tenure (months, 1–24)</Label>
                <Input id="tenureMonths" type="number" min="1" max="24" {...register("tenureMonths", { valueAsNumber: true })} />
                <FieldError message={formState.errors.tenureMonths?.message} />
              </div>
            </div>
            <div>
              <Label htmlFor="purpose">Purpose</Label>
              <Textarea
                id="purpose"
                placeholder="e.g. Restocking my shop ahead of the festive season"
                {...register("purpose")}
              />
              <FieldError message={formState.errors.purpose?.message} />
            </div>

            <div className="rounded-lg bg-slate-50 p-4 text-sm text-slate-600">
              <p className="font-medium text-slate-800">Good to know</p>
              <ul className="mt-1 list-inside list-disc space-y-1">
                <li>
                  Loans above {formatNairaCompact(500_000)} need 1 guarantor; above{" "}
                  {formatNairaCompact(2_000_000)}, 2 guarantors and verified collateral.
                </li>
                <li>Interest uses the reducing-balance method — you never pay interest on money you&apos;ve already repaid.</li>
                <li>Your BVN must be on your profile before you can submit.</li>
              </ul>
            </div>

            <Button type="submit" loading={create.isPending}>
              Save draft{amount ? ` for ${formatNairaCompact(Number(amount))}` : ""}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
