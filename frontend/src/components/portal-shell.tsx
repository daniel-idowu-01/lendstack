"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Landmark, LogOut, Menu, ShieldX } from "lucide-react";
import { clearSession, getToken, getUser, homeFor } from "@/lib/auth";
import { cn } from "@/lib/cn";
import type { Role, UserDto } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { LoadingState } from "@/components/page-states";

export interface NavItem {
  href: string;
  label: string;
  icon: React.ReactNode;
}

/**
 * Role-guarded dashboard shell shared by the three portals. Unauthenticated →
 * /login; wrong role → 403 screen with a link to the user's own portal.
 */
export function PortalShell({
  role,
  title,
  nav,
  children,
}: {
  role: Role;
  title: string;
  nav: NavItem[];
  children: React.ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const [state, setState] = useState<"checking" | "ok" | "forbidden">("checking");
  const [user, setUser] = useState<UserDto | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const token = getToken();
    const current = getUser();
    if (!token || !current) {
      router.replace("/login");
      return;
    }
    setUser(current);
    setState(current.role === role ? "ok" : "forbidden");
  }, [role, router]);

  if (state === "checking") return <LoadingState label="Checking your session…" />;

  if (state === "forbidden") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
        <ShieldX className="h-12 w-12 text-red-500" aria-hidden />
        <h1 className="text-xl font-semibold text-slate-900">403 — You don&apos;t have access here</h1>
        <p className="max-w-md text-sm text-slate-600">
          This section is for {title.toLowerCase()} accounts. You are signed in as{" "}
          {user?.email}.
        </p>
        <Button onClick={() => router.replace(user ? homeFor(user.role) : "/login")}>
          Go to my dashboard
        </Button>
      </div>
    );
  }

  const logout = () => {
    clearSession();
    router.replace("/login");
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
          <div className="flex items-center gap-2">
            <button
              className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 md:hidden"
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="Toggle menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <Landmark className="h-6 w-6 text-emerald-600" aria-hidden />
            <span className="font-semibold text-slate-900">LendStack</span>
            <span className="ml-2 hidden rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700 sm:inline">
              {title}
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-slate-600 sm:inline">{user?.fullName}</span>
            <Button variant="ghost" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4" aria-hidden />
              <span className="hidden sm:inline">Sign out</span>
            </Button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-6xl gap-6 px-4 py-6">
        <nav
          className={cn(
            "w-52 shrink-0 md:block",
            menuOpen ? "block" : "hidden",
          )}
        >
          <ul className="space-y-1">
            {nav.map((item) => {
              const active =
                item.href === homeFor(role)
                  ? pathname === item.href
                  : pathname.startsWith(item.href);
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    onClick={() => setMenuOpen(false)}
                    className={cn(
                      "flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium",
                      active
                        ? "bg-emerald-600 text-white"
                        : "text-slate-700 hover:bg-slate-100",
                    )}
                  >
                    {item.icon}
                    {item.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}
