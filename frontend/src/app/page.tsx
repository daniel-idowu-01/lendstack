"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getToken, getUser, homeFor } from "@/lib/auth";
import { LoadingState } from "@/components/page-states";

/** Entry point: signed-in users land on their portal, everyone else on login. */
export default function Home() {
  const router = useRouter();
  useEffect(() => {
    const user = getUser();
    router.replace(getToken() && user ? homeFor(user.role) : "/login");
  }, [router]);
  return <LoadingState label="Taking you to your dashboard…" />;
}
