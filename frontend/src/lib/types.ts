
export type Role = "BORROWER" | "LOAN_OFFICER" | "ADMIN";

export type LoanStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "CREDIT_CHECK"
  | "PENDING_GUARANTOR"
  | "PENDING_COLLATERAL"
  | "APPROVED"
  | "DISBURSED"
  | "ACTIVE"
  | "DELINQUENT"
  | "DEFAULTED"
  | "CLOSED"
  | "REJECTED"
  | "WRITTEN_OFF";

export type RiskTier = "LOW" | "MEDIUM" | "HIGH" | "DECLINED";
export type EmploymentStatus =
  | "EMPLOYED"
  | "SELF_EMPLOYED"
  | "BUSINESS_OWNER"
  | "UNEMPLOYED"
  | "RETIRED"
  | "STUDENT";
export type GuarantorStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "EXPIRED";
export type InstallmentStatus = "PENDING" | "PAID" | "OVERDUE" | "WAIVED";
export type VerificationStatus = "UNVERIFIED" | "VERIFIED" | "REJECTED";
export type CollateralType = "PROPERTY" | "VEHICLE" | "FIXED_DEPOSIT" | "EQUIPMENT";
export type LenderType = "INDIVIDUAL" | "INSTITUTION";

export interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: { code: string; message: string; details?: unknown };
  timestamp: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface UserDto {
  id: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  user: UserDto;
}

export interface LoanResponse {
  id: string;
  reference: string;
  borrowerName: string;
  amount: number;
  purpose: string;
  tenureMonths: number;
  interestRateAnnual: number | null;
  status: LoanStatus;
  riskTier: RiskTier | null;
  creditScore: number | null;
  guarantorsRequired: number;
  collateralRequired: boolean;
  outstandingPrincipal: number | null;
  submittedAt: string | null;
  approvedAt: string | null;
  disbursedAt: string | null;
  closedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
}

export interface TimelineEntry {
  action: string;
  fromStatus: string | null;
  toStatus: string | null;
  reason: string | null;
  performedBy: string | null;
  timestamp: string;
}

export interface LoanDetailResponse {
  loan: LoanResponse;
  timeline: TimelineEntry[];
}

export interface BorrowerSummary {
  userId: string;
  fullName: string;
  email: string;
  phone: string | null;
  employmentStatus: EmploymentStatus | null;
  employerName: string | null;
  monthlyIncome: number | null;
  bankName: string | null;
  bvnMasked: string | null;
  bvnVerified: boolean;
}

export interface OfficerLoanDetailResponse {
  loan: LoanResponse;
  borrower: BorrowerSummary;
  timeline: TimelineEntry[];
}

export interface ProfileResponse {
  bvnMasked: string | null;
  bvnVerified: boolean;
  ninMasked: string | null;
  employmentStatus: EmploymentStatus | null;
  employerName: string | null;
  monthlyIncome: number | null;
  bankAccountMasked: string | null;
  bankName: string | null;
  dateOfBirth: string | null;
  address: string | null;
  kycComplete: boolean;
}

export interface GuarantorResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  relationship: string | null;
  occupation: string | null;
  monthlyIncome: number | null;
  status: GuarantorStatus;
  requestedAt: string | null;
  respondedAt: string | null;
  expiresAt: string | null;
  declineReason: string | null;
}

export interface GuarantorInviteView {
  guarantorName: string;
  borrowerName: string;
  loanAmount: number;
  tenureMonths: number;
  purpose: string;
  status: GuarantorStatus;
  expiresAt: string | null;
}

export interface CollateralResponse {
  id: string;
  type: CollateralType;
  description: string;
  estimatedValue: number;
  verificationStatus: VerificationStatus;
  verifiedBy: string | null;
  verifiedAt: string | null;
  rejectionReason: string | null;
}

export interface DocumentResponse {
  id: string;
  docType: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  collateralId: string | null;
  uploadedBy: string;
  uploadedAt: string;
}

export interface InstallmentResponse {
  id: string;
  installmentNumber: number;
  dueDate: string;
  principalDue: number;
  interestDue: number;
  penaltyDue: number;
  totalDue: number;
  amountPaid: number;
  status: InstallmentStatus;
  paidAt: string | null;
}

export interface ScheduleResponse {
  loanId: string;
  loanReference: string;
  loanStatus: LoanStatus;
  loanAmount: number;
  interestRateAnnual: number | null;
  outstandingPrincipal: number | null;
  totalOutstanding: number;
  nextDueDate: string | null;
  installments: InstallmentResponse[];
}

export interface PaymentInitResponse {
  reference: string;
  amount: number;
  authorizationUrl: string;
}

export interface RuleResult {
  rule: string;
  points: number;
  maxPoints: number;
  detail: string;
}

export interface CreditAssessmentResponse {
  id: string;
  score: number;
  riskTier: RiskTier;
  breakdown: RuleResult[] | null;
  overridden: boolean;
  overrideReason: string | null;
  assessedBy: string | null;
  createdAt: string;
}

export interface LenderResponse {
  id: string;
  name: string;
  type: LenderType;
  email: string;
  walletBalance: number;
  maxExposure: number;
  currentExposure: number;
  preferredRiskTier: RiskTier;
  active: boolean;
  createdAt: string;
}

export interface FundingResponse {
  id: string;
  loanId: string;
  loanReference: string;
  lenderId: string;
  lenderName: string;
  amount: number;
  principalRepaid: number;
  interestEarned: number;
  createdAt: string;
}

export interface ConfigResponse {
  key: string;
  value: string;
  valueType: "NUMBER" | "BOOLEAN" | "STRING";
  description: string | null;
  updatedBy: string | null;
  updatedAt: string;
}

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface AuditLogResponse {
  id: number;
  entityType: string;
  entityId: string;
  action: string;
  performedBy: string;
  oldValue: string | null;
  newValue: string | null;
  reason: string | null;
  timestamp: string;
  ipAddress: string | null;
}
