const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

const TOKEN_KEY = "fintrack_token";

export class ApiClientError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (typeof window === "undefined") return;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: { status: number; message: string };
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    setToken(null);
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new ApiClientError("Sesión expirada", 401);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const body = (await res.json().catch(() => null)) as ApiEnvelope<T> | null;

  if (!res.ok) {
    const message =
      body?.error?.message ?? (body as unknown as { message?: string })?.message ?? "Error inesperado";
    throw new ApiClientError(message, res.status);
  }

  if (body && typeof body === "object" && "success" in body) {
    return body.data as T;
  }
  return body as T;
}

function get<T>(path: string) {
  return request<T>(path);
}

function post<T>(path: string, data?: unknown) {
  return request<T>(path, {
    method: "POST",
    body: data === undefined ? undefined : JSON.stringify(data),
  });
}

function put<T>(path: string, data: unknown) {
  return request<T>(path, { method: "PUT", body: JSON.stringify(data) });
}

function del<T = void>(path: string) {
  return request<T>(path, { method: "DELETE" });
}

// ─────────────────────────────── Tipos ───────────────────────────────

export type TransactionType = "INCOME" | "EXPENSE";
export type AccountType = "CASH" | "WALLET" | "BANK" | "CARD";
export type CategoryType = "INCOME" | "EXPENSE";
export type DebtStatus = "PAID" | "ACTIVE";
export type MonotributoStatus = "UP_TO_DATE" | "DEBT";

export interface User {
  id: string;
  username: string;
  email: string;
}

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  owner: string;
  active: boolean;
}

export interface Category {
  id: string;
  name: string;
  color: string;
  type: CategoryType;
  active: boolean;
}

export interface Transaction {
  id: string;
  type: TransactionType;
  description: string;
  amount: number;
  date: string;
  notes?: string;
  categoryName?: string;
  accountName?: string;
}

export interface FixedExpense {
  id: string;
  name: string;
  amount: number;
  dueDay: string;
  frequency?: string;
  active: boolean;
  categoryName?: string;
  accountName?: string;
}

export interface Card {
  id: string;
  holderName: string;
  dueDay: string;
  amount: number;
  active: boolean;
}

export interface Debt {
  id: string;
  creditor: string;
  totalAmount: number;
  remainingAmount: number;
  startDate: string;
  status: DebtStatus;
}

export interface SavingGoal {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  active: boolean;
}

export interface Monotributo {
  id: string;
  name: string;
  monthlyAmount: number;
  dueDay: string;
  status: MonotributoStatus;
}

export interface TransactionSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
  from?: string;
  to?: string;
}

export interface MonthlySummary {
  year: number;
  month: number;
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export interface CategoryExpense {
  categoryName: string;
  total: number;
}

export interface AccountBalance {
  accountId: string;
  accountName: string;
  balance: number;
}

export interface AccountRequest {
  name: string;
  type: AccountType;
  owner: string;
  active: boolean;
}

export interface CategoryRequest {
  name: string;
  color: string;
  type: CategoryType;
  active: boolean;
}

export interface TransactionRequest {
  type: TransactionType;
  description: string;
  amount: number;
  date: string;
  notes?: string;
  categoryId: string;
  accountId: string;
}

export interface FixedExpenseRequest {
  name: string;
  amount: number;
  dueDay: string;
  frequency?: string;
  active: boolean;
  categoryId: string;
  accountId: string;
}

export interface CardRequest {
  holderName: string;
  dueDay: string;
  amount: number;
  active: boolean;
}

export interface DebtRequest {
  creditor: string;
  totalAmount: number;
  remainingAmount: number;
  startDate: string;
  status: DebtStatus;
}

export interface SavingGoalRequest {
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  active: boolean;
}

export interface MonotributoRequest {
  name: string;
  monthlyAmount: number;
  dueDay: string;
  status: MonotributoStatus;
}

// ─────────────────────────────── API ───────────────────────────────

export const api = {
  // Auth
  login: (email: string, password: string) =>
    post<{ accessToken: string }>("/auth/login", { email, password }),
  register: (username: string, email: string, password: string) =>
    post<unknown>("/auth/register", { username, email, password }),
  me: () => get<User>("/users/me"),

  // Accounts
  listAccounts: () => get<Account[]>("/accounts"),
  createAccount: (data: AccountRequest) => post<Account>("/accounts", data),
  updateAccount: (id: string, data: AccountRequest) =>
    put<Account>(`/accounts/${id}`, data),
  deleteAccount: (id: string) => del(`/accounts/${id}`),

  // Categories
  listCategories: () => get<Category[]>("/categories"),
  createCategory: (data: CategoryRequest) => post<Category>("/categories", data),
  updateCategory: (id: string, data: CategoryRequest) =>
    put<Category>(`/categories/${id}`, data),
  deleteCategory: (id: string) => del(`/categories/${id}`),

  // Transactions
  listTransactions: () => get<Transaction[]>("/transactions"),
  createTransaction: (data: TransactionRequest) =>
    post<Transaction>("/transactions", data),
  updateTransaction: (id: string, data: TransactionRequest) =>
    put<Transaction>(`/transactions/${id}`, data),
  deleteTransaction: (id: string) => del(`/transactions/${id}`),
  transactionSummary: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const qs = params.toString();
    return get<TransactionSummary>(`/transactions/summary${qs ? `?${qs}` : ""}`);
  },

  // Fixed expenses
  listFixedExpenses: () => get<FixedExpense[]>("/fixed-expenses"),
  createFixedExpense: (data: FixedExpenseRequest) =>
    post<FixedExpense>("/fixed-expenses", data),
  updateFixedExpense: (id: string, data: FixedExpenseRequest) =>
    put<FixedExpense>(`/fixed-expenses/${id}`, data),
  deleteFixedExpense: (id: string) => del(`/fixed-expenses/${id}`),

  // Cards
  listCards: () => get<Card[]>("/cards"),
  createCard: (data: CardRequest) => post<Card>("/cards", data),
  updateCard: (id: string, data: CardRequest) => put<Card>(`/cards/${id}`, data),
  deleteCard: (id: string) => del(`/cards/${id}`),

  // Debts
  listDebts: () => get<Debt[]>("/debts"),
  createDebt: (data: DebtRequest) => post<Debt>("/debts", data),
  updateDebt: (id: string, data: DebtRequest) => put<Debt>(`/debts/${id}`, data),
  deleteDebt: (id: string) => del(`/debts/${id}`),

  // Saving goals
  listSavingGoals: () => get<SavingGoal[]>("/saving-goals"),
  createSavingGoal: (data: SavingGoalRequest) =>
    post<SavingGoal>("/saving-goals", data),
  updateSavingGoal: (id: string, data: SavingGoalRequest) =>
    put<SavingGoal>(`/saving-goals/${id}`, data),
  deleteSavingGoal: (id: string) => del(`/saving-goals/${id}`),

  // Monotributo
  listMonotributo: () => get<Monotributo[]>("/monotributo"),
  createMonotributo: (data: MonotributoRequest) =>
    post<Monotributo>("/monotributo", data),
  updateMonotributo: (id: string, data: MonotributoRequest) =>
    put<Monotributo>(`/monotributo/${id}`, data),
  deleteMonotributo: (id: string) => del(`/monotributo/${id}`),

  // Reports
  monthlySummary: (year: number, month: number) =>
    get<MonthlySummary>(`/reports/monthly-summary?year=${year}&month=${month}`),
  byCategory: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const qs = params.toString();
    return get<CategoryExpense[]>(`/reports/by-category${qs ? `?${qs}` : ""}`);
  },
  balance: () => get<AccountBalance[]>("/reports/balance"),
};
