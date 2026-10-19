"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import MainLayout from "@/app/components/layout/MainLayout";
import TransactionItem from "@/app/components/dashboard/TransactionItem";
import TransactionFormDialog from "@/app/components/forms/TransactionFormDialog";
import { Button } from "@/components/ui/button";
import { Plus, ArrowUpCircle, ArrowDownCircle } from "lucide-react";
import { toast } from "sonner";
import { api, Transaction, Account, Category } from "@/lib/api";
import { formatMoney } from "@/lib/format";

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
  const [editing, setEditing] = useState<Transaction | null>(null);

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

  async function handleDelete(id: string) {
    try {
      await api.deleteTransaction(id);
      await load();
      toast.success("Transacción eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al eliminar");
    }
  }

  function openCreate() {
    setEditing(null);
    setShowForm(true);
  }

  function openEdit(transaction: Transaction) {
    setEditing(transaction);
    setShowForm(true);
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
          onClick={openCreate}
        >
          <Plus className="w-4 h-4 mr-2" />
          Nuevo {isIncome ? "Ingreso" : "Gasto"}
        </Button>
      </div>

      {(accounts.length === 0 || categories.length === 0) && (
        <div className="card-gradient rounded-xl p-4 mb-6">
          <p className="text-sm text-muted-foreground">
            {accounts.length === 0 ? (
              <>
                Necesitás{" "}
                <Link href="/cuentas" className="text-primary underline">
                  crear una cuenta
                </Link>{" "}
                para registrar movimientos.
              </>
            ) : (
              <>
                No tenés categorías de {isIncome ? "ingreso" : "egreso"}.{" "}
                <Link href="/categorias" className="text-primary underline">
                  Creá una
                </Link>
                .
              </>
            )}
          </p>
        </div>
      )}

      <TransactionFormDialog
        type={type}
        open={showForm}
        onOpenChange={(v) => {
          setShowForm(v);
          if (!v) setEditing(null);
        }}
        onCreated={load}
        transaction={editing}
      />

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
              <TransactionItem
                key={t.id}
                type={t.type === "INCOME" ? "income" : "expense"}
                description={t.description}
                category={t.categoryName ?? "Sin categoría"}
                amount={Number(t.amount)}
                date={t.date}
                account={t.accountName ?? t.cardName ?? "—"}
                onDelete={() => handleDelete(t.id)}
                onEdit={() => openEdit(t)}
              />
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default TransactionsPage;
