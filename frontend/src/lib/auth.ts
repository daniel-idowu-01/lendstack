import Cookies from "js-cookie";
import type { Role, UserDto } from "./types";

const TOKEN_KEY = "lendstack_token";
const USER_KEY = "lendstack_user";

export function saveSession(token: string, user: UserDto) {
  Cookies.set(TOKEN_KEY, token, { sameSite: "strict", expires: 1 });
  Cookies.set(USER_KEY, JSON.stringify(user), { sameSite: "strict", expires: 1 });
}

export function clearSession() {
  Cookies.remove(TOKEN_KEY);
  Cookies.remove(USER_KEY);
}

export function getToken(): string | undefined {
  return Cookies.get(TOKEN_KEY);
}

export function getUser(): UserDto | null {
  const raw = Cookies.get(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserDto;
  } catch {
    return null;
  }
}


export function homeFor(role: Role): string {
  switch (role) {
    case "BORROWER":
      return "/borrower";
    case "LOAN_OFFICER":
      return "/officer";
    case "ADMIN":
      return "/admin";
  }
}
