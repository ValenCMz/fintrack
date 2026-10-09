"use client";

import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import { api, Account, Category, TransactionRequest } from "@/lib/api";
import { todayISO } from "@/lib/format";

interface TransactionFormDialogProps {
  type: "income" | "expense";
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void | Promise<void>;
}

const TransactionFormDialog = ({
  type,
  open,
  onOpenChange,
  onCreated,
}: TransactionFormDialogProps) => {
  const isIncome = type === "income";
  const backendType = isIncome ? "INCOME" : "EXPENSE";

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [saving, setSaving] = useState(false);

  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    (async () => {
      try {
        const [acc, cat] = await Promise.all([
          api.listAccounts(),
          api.listCategories(),
        ]);
        if (cancelled) return;
        setAccounts(acc.filter((a) => a.active));
        setCategories(cat.filter((c) => c.type === backendType));
      } catch {
        // los errores de auth ya redirigen
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, backendType]);

  function reset() {
    setDescription("");
    setAmount("");
    setDate(todayISO());
    setAccountId("");
    setCategoryId("");
  }

  async function handleSubmit(e: React.FormEvent) {
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
      await onCreated?.();
      toast.success(isIncome ? "Ingreso registrado" : "Gasto registrado");
      reset();
      onOpenChange(false);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl" aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>Nuevo {isIncome ? "ingreso" : "gasto"}</DialogTitle>
        </DialogHeader>
        <form
          onSubmit={handleSubmit}
          className="grid grid-cols-1 md:grid-cols-2 gap-4"
        >
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
            className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm md:col-span-2"
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
          <DialogFooter className="md:col-span-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Guardando..." : "Guardar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default TransactionFormDialog;
