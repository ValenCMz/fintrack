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
import { Field } from "@/components/ui/field";
import { toast } from "sonner";
import { api, Account, Category, SavingGoal } from "@/lib/api";
import { todayISO } from "@/lib/format";

interface SavingGoalContributionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  goal: SavingGoal | null;
  onContributed?: () => void | Promise<void>;
}

const SavingGoalContributionDialog = ({
  open,
  onOpenChange,
  goal,
  onContributed,
}: SavingGoalContributionDialogProps) => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setAmount("");
    setDate(todayISO());
    setAccountId("");
    setCategoryId("");
    (async () => {
      try {
        const [allAccounts, allCategories] = await Promise.all([
          api.listAccounts(),
          api.listCategories(),
        ]);
        const activeAccounts = allAccounts.filter((a) => a.active);
        const incomeCategories = allCategories.filter((c) => c.type === "INCOME");
        setAccounts(activeAccounts);
        setCategories(incomeCategories);
        if (activeAccounts.length > 0) setAccountId(activeAccounts[0].id);
        const fallback = incomeCategories.find((c) => c.name === "Otros ingresos");
        if (fallback) setCategoryId(fallback.id);
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Error");
      }
    })();
  }, [open]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!goal) return;
    if (!accountId) {
      toast.error("Seleccioná una cuenta");
      return;
    }
    const parsed = Number(amount);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      toast.error("Ingresá un monto válido");
      return;
    }
    setSaving(true);
    try {
      await api.contributeToSavingGoal(goal.id, {
        amount: parsed,
        date,
        accountId,
        categoryId: categoryId || undefined,
      });
      toast.success("Aporte registrado");
      onOpenChange(false);
      await onContributed?.();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al registrar el aporte");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>Contribuir a {goal?.name ?? "la meta"}</DialogTitle>
        </DialogHeader>

        {accounts.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No tenés cuentas activas. Creá una para registrar un aporte.
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="grid gap-4">
            <Field label="Monto del aporte">
              <Input
                type="number"
                step="0.01"
                min="0"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </Field>
            <Field label="Fecha">
              <Input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </Field>
            <Field label="Cuenta de donde sale el dinero" hint="Se registra como un ingreso en esa cuenta">
              <select
                className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                required
              >
                <option value="">Elegí una cuenta...</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Categoría del ingreso">
              <select
                className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
              >
                <option value="">Sin categoría (usar «Otros ingresos»)</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={saving}>
                {saving ? "Registrando..." : "Registrar aporte"}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default SavingGoalContributionDialog;
