"use client";

import { Banknote, ScrollText, Settings2, Users } from "lucide-react";
import { PortalShell } from "@/components/portal-shell";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <PortalShell
      role="ADMIN"
      title="Admin"
      nav={[
        { href: "/admin", label: "Configuration", icon: <Settings2 className="h-4 w-4" /> },
        { href: "/admin/lenders", label: "Lenders", icon: <Banknote className="h-4 w-4" /> },
        { href: "/admin/users", label: "Users", icon: <Users className="h-4 w-4" /> },
        { href: "/admin/audit", label: "Audit log", icon: <ScrollText className="h-4 w-4" /> },
      ]}
    >
      {children}
    </PortalShell>
  );
}
