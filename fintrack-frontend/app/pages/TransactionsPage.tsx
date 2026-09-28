"use client";

import { useCallback, useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import TransactionItem from "@/app/components/dashboard/TransactionItem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Plus, ArrowUpCircle, ArrowDownCircle } from "lucide-react";
import { toast } from "sonner";
import { api, Transaction, Account, Category, TransactionRequest } from "@/lib/api";
import { formatMoney, todayISO } from "@/lib/format";

interface TransactionsPageProps {
  type: "income" | "expense";
}

const TransactionsPage = ({ type }: TransactionsPageProps) => {
  const isIncome = type === "income";
  const backendType = isIncome ? "INCOME" : "EXPENSE";

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");

  const load = useCallback(async () => {
    const [tx, acc, cat] = await Promise.all([
      api.listTransactions(),
      api.listAccounts(),
      api.listCategories(),
    ]);
    setTransactions(tx.filter((t) => t.type === backendType));
    setAccounts(acc.filter((a) => a.active));
    setCategories(cat.filter((c) => c.type === backendType));
  }, [backendType]);

  useEffect(() => {
    (async () => {
      try {
        await load();
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Error al cargar");
      } finally {
        setLoading(false);
      }
    })();
  }, [load]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!accountId || !categoryId) {
      toast.error("Seleccioná cuenta y categoría");
      return;
    }
    setSaving(true);
    try {
      const payload: TransactionRequest = {
        type: backendType,
        description,
        amount: Number(amount),
        date,
        categoryId,
        accountId,
      };
      await api.createTransaction(payload);
      await load();
      setDescription("");
      setAmount("");
      setDate(todayISO());
      setShowForm(false);
      toast.success(isIncome ? "Ingreso registrado" : "Gasto registrado");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteTransaction(id);
      await load();
      toast.success("Transacción eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al eliminar");
    }
  }

  const totalAmount = transactions.reduce((s, t) => s + Number(t.amount), 0);

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8 animate-fade-in">
        <div className="flex items-center gap-4">
          <div
            className={`p-3 rounded-xl ${isIncome ? "bg-income/20" : "bg-expense/20"}`}
          >
            {isIncome ? (
              <ArrowUpCircle className="w-8 h-8 text-income" />
            ) : (
              <ArrowDownCircle className="w-8 h-8 text-expense" />
            )}
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">
              {isIncome ? "Ingresos" : "Egresos"}
            </h1>
            <p className="text-muted-foreground">
              Gestioná tus {isIncome ? "ingresos" : "gastos"}
            </p>
          </div>
        </div>
        <Button
          className={isIncome ? "bg-income hover:bg-income/90" : "bg-expense hover:bg-expense/90"}
          onClick={() => setShowForm((v) => !v)}
        >
          <Plus className="w-4 h-4 mr-2" />
          Nuevo {isIncome ? "Ingreso" : "Gasto"}
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="card-gradient rounded-xl p-6 mb-6 space-y-4 animate-slide-up"
        >
          <h3 className="font-display font-semibold">
            Nuevo {isIncome ? "ingreso" : "gasto"}
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              placeholder="Descripción"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="Monto"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
            <Input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
            <select
              className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
              value={accountId}
              onChange={(e) => setAccountId(e.target.value)}
              required
            >
              <option value="">Cuenta...</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
            </select>
            <select
              className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              required
            >
              <option value="">Categoría...</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
            <Button type="button" variant="outline" onClick={() => setShowForm(false)}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <div className="card-gradient rounded-xl p-6 mb-6 animate-slide-up">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-muted-foreground text-sm">Total</p>
            <p
              className={`font-display text-4xl font-bold ${
                isIncome ? "text-income" : "text-expense"
              }`}
            >
              {formatMoney(totalAmount)}
            </p>
          </div>
          <div className="text-right">
            <p className="text-muted-foreground text-sm">
              {transactions.length} transacciones
            </p>
          </div>
        </div>
      </div>

      <div className="card-gradient rounded-xl p-5 animate-slide-up">
        {loading ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : transactions.length === 0 ? (
          <p className="text-muted-foreground text-sm">
            No hay {isIncome ? "ingresos" : "gastos"} registrados.
          </p>
        ) : (
          <div className="space-y-3">
            {transactions.map((t) => (
              <div key={t.id} className="relative">
                <TransactionItem
                  type={t.type === "INCOME" ? "income" : "expense"}
                  description={t.description}
                  category={t.categoryName ?? "Sin categoría"}
                  amount={Number(t.amount)}
                  date={t.date}
                  account={t.accountName ?? "—"}
                />
                <button
                  onClick={() => handleDelete(t.id)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-expense"
                >
                  Eliminar
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default TransactionsPage;
