/**
 * The one and only currency/date formatting layer — components never format
 * raw numbers or dates themselves.
 */

const nairaFormatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  currencyDisplay: "narrowSymbol",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const nairaWholeFormatter = new Intl.NumberFormat("en-NG", {
  style: "currency",
  currency: "NGN",
  currencyDisplay: "narrowSymbol",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

export function formatNaira(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return nairaFormatter.format(value);
}

/** Compact ₦ for dashboards: no kobo when the amount is whole. */
export function formatNairaCompact(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return Number.isInteger(value) ? nairaWholeFormatter.format(value) : nairaFormatter.format(value);
}

/** DD/MM/YYYY — the Nigerian convention. Accepts ISO dates and instants. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const dd = String(date.getDate()).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  return `${dd}/${mm}/${date.getFullYear()}`;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const hh = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");
  return `${formatDate(value)} ${hh}:${min}`;
}

/** SNAKE_CASE → Title Case for statuses and enum labels. */
export function humanize(value: string | null | undefined): string {
  if (!value) return "—";
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}
