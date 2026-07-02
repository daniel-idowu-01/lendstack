"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, XCircle } from "lucide-react";
import { borrowerApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { formatNaira } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { LoadingState } from "@/components/page-states";

/**
 * Paystack redirects here after checkout (?reference=... or ?trxref=...).
 * We confirm the charge server-side; the webhook usually got there first —
 * either way this is safe to reload.
 */
function CallbackContent() {
  const params = useSearchParams();
  const reference = params.get("reference") ?? params.get("trxref");

  const verify = useQuery({
    queryKey: ["payment-verify", reference],
    queryFn: () => borrowerApi.verifyPayment(reference!),
    enabled: !!reference,
    retry: 2,
  });

  if (!reference) {
    return (
      <Card className="w-full max-w-md">
        <CardContent className="p-6 text-center text-sm text-slate-600">
          Missing payment reference. If you were charged, your payment will still be applied
          automatically — check your repayment dashboard.
        </CardContent>
      </Card>
    );
  }

  if (verify.isLoading) return <LoadingState label="Confirming your payment with Paystack…" />;

  const paid = verify.data?.status === "PAID";
  return (
    <Card className="w-full max-w-md">
      <CardContent className="flex flex-col items-center gap-3 p-8 text-center">
        {paid ? (
          <>
            <CheckCircle2 className="h-12 w-12 text-emerald-600" aria-hidden />
            <h1 className="text-lg font-semibold">Payment received — thank you!</h1>
            <p className="text-sm text-slate-600">
              Installment {verify.data!.installmentNumber} is settled
              {verify.data!.amountPaid ? ` (${formatNaira(verify.data!.amountPaid)} paid)` : ""}.
            </p>
          </>
        ) : verify.isError ? (
          <>
            <XCircle className="h-12 w-12 text-red-500" aria-hidden />
            <h1 className="text-lg font-semibold">We couldn&apos;t confirm this payment</h1>
            <p className="text-sm text-slate-600">{errorMessage(verify.error)}</p>
          </>
        ) : (
          <>
            <XCircle className="h-12 w-12 text-amber-500" aria-hidden />
            <h1 className="text-lg font-semibold">Payment not completed</h1>
            <p className="text-sm text-slate-600">
              The charge didn&apos;t go through. No money left your account — you can try again
              from your repayment dashboard.
            </p>
          </>
        )}
        <Link href="/borrower">
          <Button variant="outline">Back to my loans</Button>
        </Link>
      </CardContent>
    </Card>
  );
}

export default function PaymentCallbackPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <Suspense>
        <CallbackContent />
      </Suspense>
    </main>
  );
}
