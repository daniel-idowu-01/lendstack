"use client";

import { FilePlus2, LayoutDashboard, UserRound } from "lucide-react";
import { PortalShell } from "@/components/portal-shell";

export default function BorrowerLayout({ children }: { children: React.ReactNode }) {
  return (
    <PortalShell
      role="BORROWER"
      title="Borrower"
      nav={[
        { href: "/borrower", label: "My loans", icon: <LayoutDashboard className="h-4 w-4" /> },
        { href: "/borrower/loans/new", label: "Apply for a loan", icon: <FilePlus2 className="h-4 w-4" /> },
        { href: "/borrower/profile", label: "KYC profile", icon: <UserRound className="h-4 w-4" /> },
      ]}
    >
      {children}
    </PortalShell>
  );
}
