import axios, { AxiosError } from "axios";
import { clearSession, getToken } from "./auth";
import type { ApiEnvelope } from "./types";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      // Session expired or invalid — send the user back to login.
      clearSession();
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login?expired=1";
      }
    }
    return Promise.reject(error);
  },
);

/**
 * Extracts a human-readable, Nigerian-context message from an API error for
 * toasts and inline errors. Falls back gracefully for network failures.
 */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const envelope = error.response?.data as ApiEnvelope<unknown> | undefined;
    if (envelope?.error?.message) return envelope.error.message;
    if (error.response?.status === 403)
      return "You do not have access to this resource.";
    if (!error.response)
      return "We could not reach the server — check your connection and try again.";
  }
  return "Something went wrong. Please try again or contact support.";
}

/** Unwraps the { success, data } envelope, throwing on business failure. */
export function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.success || envelope.data === undefined) {
    throw new Error(envelope.error?.message ?? "Unexpected response");
  }
  return envelope.data;
}
