import type { Metadata } from "next";
import { Providers } from "@/components/providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "LendStack — Loans made clear",
  description:
    "Personal loans for Nigerians: apply in minutes, track every step, repay with Paystack.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en-NG">
      <body className="min-h-screen bg-slate-50 font-sans text-slate-900 antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
