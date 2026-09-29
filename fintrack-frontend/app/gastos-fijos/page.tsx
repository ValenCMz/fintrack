"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CalendarClock, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, FixedExpense, Account, Category } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function GastosFijosPage() {
  const [items, setItems] = useState<FixedExpense[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [dueDay, setDueDay] = useState(todayISO());
  const [frequency, setFrequency] = useState("mensual");
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");

  async function load() {
    const [fx, acc, cat] = await Promise.all([
      api.listFixedExpenses(),
      api.listAccounts(),
      api.listCategories(),
    ]);
    setItems(fx);
    setAccounts(acc.filter((a) => a.active));
    setCategories(cat.filter((c) => c.type === "EXPENSE"));
  }

  useEffect(() => {
    load()
      .catch((e) => toast.error(e instanceof Error ? e.message : "Error"))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!accountId || !categoryId) {
      toast.error("Seleccioná cuenta y categoría");
      return;
    }
    setSaving(true);
    try {
      await api.createFixedExpense({
        name,
        amount: Number(amount),
        dueDay,
        frequency,
        active: true,
        accountId,
        categoryId,
      });
      await load();
      setName("");
      setAmount("");
      setShowForm(false);
      toast.success("Gasto fijo creado");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteFixedExpense(id);
      await load();
      toast.success("Gasto fijo eliminado");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <CalendarClock className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Gastos Fijos</h1>
            <p className="text-muted-foreground">Gestioná tus pagos recurrentes</p>
          </div>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="w-4 h-4 mr-2" />
          Nuevo gasto fijo
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
                para registrar un gasto fijo.
              </>
            ) : (
              <>
                No tenés categorías de egreso.{" "}
                <Link href="/categorias" className="text-primary underline">
                  Creá una
                </Link>
                .
              </>
            )}
          </p>
        </div>
      )}

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="card-gradient rounded-xl p-6 mb-6 grid grid-cols-1 md:grid-cols-2 gap-4"
        >
          <Input
            placeholder="Nombre"
            value={name}
            onChange={(e) => setName(e.target.value)}
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
            value={dueDay}
            onChange={(e) => setDueDay(e.target.value)}
            required
          />
          <Input
            placeholder="Frecuencia"
            value={frequency}
            onChange={(e) => setFrequency(e.target.value)}
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
          <div className="md:col-span-2 flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
            <Button type="button" variant="outline" onClick={() => setShowForm(false)}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <div className="card-gradient rounded-xl p-5">
        {loading ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : items.length === 0 ? (
          <p className="text-muted-foreground text-sm">No tenés gastos fijos.</p>
        ) : (
          <div className="space-y-3">
            {items.map((f) => (
              <div
                key={f.id}
                className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
              >
                <div>
                  <p className="font-medium">{f.name}</p>
                  <p className="text-sm text-muted-foreground">
                    Vence {formatDate(f.dueDay)} · {f.categoryName ?? "—"}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <p className="font-display font-semibold text-expense">
                    {formatMoney(f.amount)}
                  </p>
                  <button
                    onClick={() => handleDelete(f.id)}
                    className="text-xs text-muted-foreground hover:text-expense"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
}
