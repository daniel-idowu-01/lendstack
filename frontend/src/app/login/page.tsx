"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Landmark } from "lucide-react";
import { authApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { homeFor, saveSession } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label } from "@/components/ui/input";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(1, "Enter your password"),
});
type FormValues = z.infer<typeof schema>;

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { register, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const login = useMutation({
    mutationFn: (values: FormValues) => authApi.login(values.email, values.password),
    onSuccess: (auth) => {
      saveSession(auth.accessToken, auth.user);
      toast.success(`Welcome back, ${auth.user.fullName.split(" ")[0]}!`);
      router.replace(homeFor(auth.user.role));
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="items-center text-center">
        <div className="mb-1 flex items-center gap-2">
          <Landmark className="h-7 w-7 text-emerald-600" aria-hidden />
          <span className="text-xl font-bold">LendStack</span>
        </div>
        <CardTitle>Sign in</CardTitle>
        <CardDescription>
          {params.get("expired")
            ? "Your session expired — please sign in again."
            : "Personal loans, clearly tracked from application to final repayment."}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit((v) => login.mutate(v))} className="space-y-4" noValidate>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" placeholder="you@example.com" {...register("email")} />
            <FieldError message={formState.errors.email?.message} />
          </div>
          <div>
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" placeholder="••••••••" {...register("password")} />
            <FieldError message={formState.errors.password?.message} />
          </div>
          <Button type="submit" className="w-full" loading={login.isPending}>
            Sign in
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-600">
          New here?{" "}
          <Link href="/register" className="font-medium text-emerald-700 hover:underline">
            Create a borrower account
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-b from-emerald-50 to-slate-50 p-4">
      <Suspense>
        <LoginForm />
      </Suspense>
    </main>
  );
}
