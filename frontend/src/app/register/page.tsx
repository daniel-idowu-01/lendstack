"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Landmark } from "lucide-react";
import { authApi } from "@/lib/endpoints";
import { errorMessage } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FieldError, Input, Label } from "@/components/ui/input";

const schema = z.object({
  fullName: z.string().min(2, "Enter your full name"),
  email: z.string().email("Enter a valid email address"),
  phone: z
    .string()
    .regex(/^(\+234|0)[789][01]\d{8}$/, "Enter a valid Nigerian phone number, e.g. 08012345678"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});
type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const { register, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const signUp = useMutation({
    mutationFn: (values: FormValues) => authApi.register(values),
    onSuccess: (auth) => {
      saveSession(auth.accessToken, auth.user);
      toast.success("Account created! Next: complete your KYC profile.");
      router.replace("/borrower/profile");
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-b from-emerald-50 to-slate-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="items-center text-center">
          <div className="mb-1 flex items-center gap-2">
            <Landmark className="h-7 w-7 text-emerald-600" aria-hidden />
            <span className="text-xl font-bold">LendStack</span>
          </div>
          <CardTitle>Create your borrower account</CardTitle>
          <CardDescription>
            You&apos;ll add your BVN and employment details after sign-up — they&apos;re required
            before your first application can be processed (CBN rules).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit((v) => signUp.mutate(v))} className="space-y-4" noValidate>
            <div>
              <Label htmlFor="fullName">Full name</Label>
              <Input id="fullName" placeholder="Amaka Eze" {...register("fullName")} />
              <FieldError message={formState.errors.fullName?.message} />
            </div>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" placeholder="you@example.com" {...register("email")} />
              <FieldError message={formState.errors.email?.message} />
            </div>
            <div>
              <Label htmlFor="phone">Phone number</Label>
              <Input id="phone" placeholder="08012345678" {...register("phone")} />
              <FieldError message={formState.errors.phone?.message} />
            </div>
            <div>
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="At least 8 characters"
                {...register("password")}
              />
              <FieldError message={formState.errors.password?.message} />
            </div>
            <Button type="submit" className="w-full" loading={signUp.isPending}>
              Create account
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-600">
            Already registered?{" "}
            <Link href="/login" className="font-medium text-emerald-700 hover:underline">
              Sign in
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
