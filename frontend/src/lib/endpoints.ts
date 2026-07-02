import { api, unwrap } from "./api";
import type {
  ApiEnvelope,
  AuditLogResponse,
  AuthResponse,
  CollateralResponse,
  CollateralType,
  ConfigResponse,
  CreditAssessmentResponse,
  DocumentResponse,
  EmploymentStatus,
  FundingResponse,
  GuarantorInviteView,
  GuarantorResponse,
  InstallmentResponse,
  LenderResponse,
  LenderType,
  LoanDetailResponse,
  LoanResponse,
  LoanStatus,
  OfficerLoanDetailResponse,
  PageResponse,
  PaymentInitResponse,
  ProfileResponse,
  RiskTier,
  Role,
  ScheduleResponse,
  UserResponse,
  VerificationStatus,
} from "./types";

// ---------- Auth ----------

export const authApi = {
  login: async (email: string, password: string) =>
    unwrap((await api.post<ApiEnvelope<AuthResponse>>("/auth/login", { email, password })).data),
  register: async (body: { email: string; password: string; fullName: string; phone: string }) =>
    unwrap((await api.post<ApiEnvelope<AuthResponse>>("/auth/register", body)).data),
};

// ---------- Borrower ----------

export const borrowerApi = {
  profile: async () =>
    unwrap((await api.get<ApiEnvelope<ProfileResponse>>("/borrower/profile")).data),
  updateProfile: async (body: {
    bvn?: string;
    nin?: string;
    employmentStatus?: EmploymentStatus;
    employerName?: string;
    monthlyIncome?: number;
    bankAccountNumber?: string;
    bankName?: string;
    dateOfBirth?: string;
    address?: string;
  }) => unwrap((await api.put<ApiEnvelope<ProfileResponse>>("/borrower/profile", body)).data),

  myLoans: async (page = 0, size = 20) =>
    unwrap(
      (
        await api.get<ApiEnvelope<PageResponse<LoanResponse>>>("/borrower/loans", {
          params: { page, size },
        })
      ).data,
    ),
  loanDetail: async (loanId: string) =>
    unwrap((await api.get<ApiEnvelope<LoanDetailResponse>>(`/borrower/loans/${loanId}`)).data),
  createLoan: async (body: { amount: number; purpose: string; tenureMonths: number }) =>
    unwrap((await api.post<ApiEnvelope<LoanResponse>>("/borrower/loans", body)).data),
  updateLoan: async (loanId: string, body: { amount: number; purpose: string; tenureMonths: number }) =>
    unwrap((await api.put<ApiEnvelope<LoanResponse>>(`/borrower/loans/${loanId}`, body)).data),
  submitLoan: async (loanId: string) =>
    unwrap((await api.post<ApiEnvelope<LoanResponse>>(`/borrower/loans/${loanId}/submit`)).data),

  guarantors: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<GuarantorResponse[]>>(`/borrower/loans/${loanId}/guarantors`)).data,
    ),
  addGuarantor: async (
    loanId: string,
    body: {
      fullName: string;
      email: string;
      phone?: string;
      relationship?: string;
      occupation?: string;
      monthlyIncome?: number;
    },
  ) =>
    unwrap(
      (await api.post<ApiEnvelope<GuarantorResponse>>(`/borrower/loans/${loanId}/guarantors`, body))
        .data,
    ),

  collaterals: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<CollateralResponse[]>>(`/borrower/loans/${loanId}/collaterals`))
        .data,
    ),
  declareCollateral: async (
    loanId: string,
    body: { type: CollateralType; description: string; estimatedValue: number },
  ) =>
    unwrap(
      (await api.post<ApiEnvelope<CollateralResponse>>(`/borrower/loans/${loanId}/collaterals`, body))
        .data,
    ),

  documents: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<DocumentResponse[]>>(`/borrower/loans/${loanId}/documents`)).data,
    ),
  uploadDocument: async (loanId: string, docType: string, file: File, collateralId?: string) => {
    const form = new FormData();
    form.append("docType", docType);
    form.append("file", file);
    if (collateralId) form.append("collateralId", collateralId);
    return unwrap(
      (
        await api.post<ApiEnvelope<DocumentResponse>>(`/borrower/loans/${loanId}/documents`, form, {
          headers: { "Content-Type": "multipart/form-data" },
        })
      ).data,
    );
  },

  schedule: async (loanId: string) =>
    unwrap((await api.get<ApiEnvelope<ScheduleResponse>>(`/borrower/loans/${loanId}/schedule`)).data),
  payInstallment: async (installmentId: string) =>
    unwrap(
      (await api.post<ApiEnvelope<PaymentInitResponse>>(`/borrower/installments/${installmentId}/pay`))
        .data,
    ),
  verifyPayment: async (reference: string) =>
    unwrap(
      (await api.get<ApiEnvelope<InstallmentResponse>>(`/borrower/payments/${reference}/verify`))
        .data,
    ),
};

// ---------- Public (guarantor links) ----------

export const guarantorPublicApi = {
  view: async (token: string) =>
    unwrap((await api.get<ApiEnvelope<GuarantorInviteView>>(`/guarantor-response/${token}`)).data),
  respond: async (token: string, accept: boolean, declineReason?: string) =>
    unwrap(
      (
        await api.post<ApiEnvelope<GuarantorInviteView>>(`/guarantor-response/${token}`, {
          accept,
          declineReason,
        })
      ).data,
    ),
};

// ---------- Officer ----------

export const officerApi = {
  queue: async (params: {
    status?: LoanStatus;
    riskTier?: RiskTier;
    minAmount?: number;
    maxAmount?: number;
    page?: number;
    size?: number;
  }) =>
    unwrap(
      (await api.get<ApiEnvelope<PageResponse<LoanResponse>>>("/officer/loans", { params })).data,
    ),
  detail: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<OfficerLoanDetailResponse>>(`/officer/loans/${loanId}`)).data,
    ),
  startReview: async (loanId: string) =>
    unwrap((await api.post<ApiEnvelope<LoanResponse>>(`/officer/loans/${loanId}/start-review`)).data),
  runCreditCheck: async (loanId: string) =>
    unwrap(
      (await api.post<ApiEnvelope<CreditAssessmentResponse>>(`/officer/loans/${loanId}/credit-check`))
        .data,
    ),
  assessments: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<CreditAssessmentResponse[]>>(`/officer/loans/${loanId}/assessments`))
        .data,
    ),
  overrideScore: async (loanId: string, body: { score: number; riskTier: RiskTier; reason: string }) =>
    unwrap(
      (
        await api.post<ApiEnvelope<CreditAssessmentResponse>>(
          `/officer/loans/${loanId}/score-override`,
          body,
        )
      ).data,
    ),
  proceed: async (loanId: string) =>
    unwrap((await api.post<ApiEnvelope<LoanResponse>>(`/officer/loans/${loanId}/proceed`)).data),
  approve: async (loanId: string, interestRateAnnual?: number) =>
    unwrap(
      (
        await api.post<ApiEnvelope<LoanResponse>>(
          `/officer/loans/${loanId}/approve`,
          interestRateAnnual ? { interestRateAnnual } : {},
        )
      ).data,
    ),
  reject: async (loanId: string, reason: string) =>
    unwrap(
      (await api.post<ApiEnvelope<LoanResponse>>(`/officer/loans/${loanId}/reject`, { reason })).data,
    ),
  disburse: async (loanId: string) =>
    unwrap((await api.post<ApiEnvelope<LoanResponse>>(`/officer/loans/${loanId}/disburse`)).data),
  writeOff: async (loanId: string, reason: string) =>
    unwrap(
      (await api.post<ApiEnvelope<LoanResponse>>(`/officer/loans/${loanId}/write-off`, { reason }))
        .data,
    ),
  guarantors: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<GuarantorResponse[]>>(`/officer/loans/${loanId}/guarantors`)).data,
    ),
  collaterals: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<CollateralResponse[]>>(`/officer/loans/${loanId}/collaterals`)).data,
    ),
  verifyCollateral: async (
    collateralId: string,
    body: { status: VerificationStatus; reason?: string },
  ) =>
    unwrap(
      (await api.post<ApiEnvelope<CollateralResponse>>(`/officer/collaterals/${collateralId}/verify`, body))
        .data,
    ),
  documents: async (loanId: string) =>
    unwrap(
      (await api.get<ApiEnvelope<DocumentResponse[]>>(`/officer/loans/${loanId}/documents`)).data,
    ),
  documentDownloadUrl: (documentId: string) => `/officer/documents/${documentId}`,
  waiveInstallment: async (installmentId: string, reason: string) =>
    unwrap(
      (
        await api.post<ApiEnvelope<InstallmentResponse>>(`/officer/installments/${installmentId}/waive`, {
          reason,
        })
      ).data,
    ),
};

// ---------- Admin ----------

export const adminApi = {
  config: async () => unwrap((await api.get<ApiEnvelope<ConfigResponse[]>>("/admin/config")).data),
  updateConfig: async (key: string, value: string) =>
    unwrap((await api.put<ApiEnvelope<ConfigResponse>>(`/admin/config/${key}`, { value })).data),

  lenders: async (page = 0, size = 50) =>
    unwrap(
      (
        await api.get<ApiEnvelope<PageResponse<LenderResponse>>>("/admin/lenders", {
          params: { page, size },
        })
      ).data,
    ),
  registerLender: async (body: {
    name: string;
    type: LenderType;
    email: string;
    maxExposure: number;
    preferredRiskTier: RiskTier;
  }) => unwrap((await api.post<ApiEnvelope<LenderResponse>>("/admin/lenders", body)).data),
  updateLender: async (
    lenderId: string,
    body: { maxExposure?: number; preferredRiskTier?: RiskTier; active?: boolean },
  ) => unwrap((await api.put<ApiEnvelope<LenderResponse>>(`/admin/lenders/${lenderId}`, body)).data),
  topUpLender: async (lenderId: string, amount: number) =>
    unwrap(
      (await api.post<ApiEnvelope<LenderResponse>>(`/admin/lenders/${lenderId}/wallet-topup`, { amount }))
        .data,
    ),
  lenderPortfolio: async (lenderId: string, page = 0, size = 20) =>
    unwrap(
      (
        await api.get<ApiEnvelope<PageResponse<FundingResponse>>>(
          `/admin/lenders/${lenderId}/portfolio`,
          { params: { page, size } },
        )
      ).data,
    ),

  users: async (params: { role?: Role; page?: number; size?: number }) =>
    unwrap(
      (await api.get<ApiEnvelope<PageResponse<UserResponse>>>("/admin/users", { params })).data,
    ),
  createUser: async (body: {
    email: string;
    password: string;
    fullName: string;
    phone?: string;
    role: Role;
  }) => unwrap((await api.post<ApiEnvelope<UserResponse>>("/admin/users", body)).data),
  deactivateUser: async (userId: string, reason: string) =>
    unwrap(
      (await api.post<ApiEnvelope<UserResponse>>(`/admin/users/${userId}/deactivate`, { reason })).data,
    ),
  activateUser: async (userId: string) =>
    unwrap((await api.post<ApiEnvelope<UserResponse>>(`/admin/users/${userId}/activate`)).data),

  auditLogs: async (params: {
    entityType?: string;
    entityId?: string;
    performedBy?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }) =>
    unwrap(
      (await api.get<ApiEnvelope<PageResponse<AuditLogResponse>>>("/admin/audit-logs", { params }))
        .data,
    ),
  overrideLoanStatus: async (loanId: string, targetStatus: LoanStatus, reason: string) =>
    unwrap(
      (
        await api.post<ApiEnvelope<LoanResponse>>(`/admin/loans/${loanId}/status-override`, {
          targetStatus,
          reason,
        })
      ).data,
    ),
};
