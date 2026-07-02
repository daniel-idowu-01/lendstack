"use client";

import { ListChecks } from "lucide-react";
import { PortalShell } from "@/components/portal-shell";

export default function OfficerLayout({ children }: { children: React.ReactNode }) {
  return (
    <PortalShell
      role="LOAN_OFFICER"
      title="Loan Officer"
      nav={[
        { href: "/officer", label: "Application queue", icon: <ListChecks className="h-4 w-4" /> },
      ]}
    >
      {children}
    </PortalShell>
  );
}
