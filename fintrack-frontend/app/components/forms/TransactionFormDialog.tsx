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
import {
  api,
  Account,
  Card,
  Category,
  Transaction,
  TransactionRequest,
} from "@/lib/api";
import { todayISO, formatMoney } from "@/lib/format";

interface TransactionFormDialogProps {
  type: "income" | "expense";
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void | Promise<void>;
  transaction?: Transaction | null;
}

const TransactionFormDialog = ({
  type,
  open,
  onOpenChange,
  onCreated,
  transaction,
}: TransactionFormDialogProps) => {
  const isIncome = type === "income";
  const backendType = isIncome ? "INCOME" : "EXPENSE";
  const isEditing = !!transaction;

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [cards, setCards] = useState<Card[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [saving, setSaving] = useState(false);

  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [target, setTarget] = useState<"account" | "card">("account");
  const [accountId, setAccountId] = useState("");
  const [cardId, setCardId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [installments, setInstallments] = useState("1");

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    (async () => {
      try {
        const [allAccounts, allCards, allCategories] = await Promise.all([
          api.listAccounts(),
          isIncome ? Promise.resolve<Card[]>([]) : api.listCards(),
          api.listCategories(),
        ]);
        if (cancelled) return;

        let accountOptions = allAccounts.filter((a) => a.active);
        let cardOptions = allCards.filter((c) => c.active);
        let categoryOptions = allCategories.filter((c) => c.type === backendType);

        if (transaction) {
          if (
            transaction.accountId &&
            !accountOptions.some((a) => a.id === transaction.accountId)
          ) {
            const current = allAccounts.find((a) => a.id === transaction.accountId);
            if (current) accountOptions = [...accountOptions, current];
          }
          if (
            transaction.cardId &&
            !cardOptions.some((c) => c.id === transaction.cardId)
          ) {
            const current = allCards.find((c) => c.id === transaction.cardId);
            if (current) cardOptions = [...cardOptions, current];
          }
          if (
            transaction.categoryId &&
            !categoryOptions.some((c) => c.id === transaction.categoryId)
          ) {
            const current = allCategories.find((c) => c.id === transaction.categoryId);
            if (current) categoryOptions = [...categoryOptions, current];
          }

          setDescription(transaction.description ?? "");
          setAmount(String(transaction.amount));
          setDate(transaction.date);
          setTarget(transaction.cardId ? "card" : "account");
          setAccountId(transaction.accountId ?? "");
          setCardId(transaction.cardId ?? "");
          setCategoryId(transaction.categoryId ?? "");
          setInstallments(String(transaction.installments ?? 1));
        }

        setAccounts(accountOptions);
        setCards(cardOptions);
        setCategories(categoryOptions);
      } catch {
        // los errores de auth ya redirigen
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, backendType, isIncome, transaction]);

  function reset() {
    setDescription("");
    setAmount("");
    setDate(todayISO());
    setTarget("account");
    setAccountId("");
    setCardId("");
    setCategoryId("");
    setInstallments("1");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!categoryId) {
      toast.error("Seleccioná una categoría");
      return;
    }
    if (target === "card" && !cardId) {
      toast.error("Seleccioná una tarjeta");
      return;
    }
    if (target === "account" && !accountId) {
      toast.error("Seleccioná una cuenta");
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
        ...(target === "card"
          ? { cardId, installments: Math.max(1, Number(installments) || 1) }
          : { accountId }),
      };
      if (isEditing && transaction) {
        await api.updateTransaction(transaction.id, payload);
      } else {
        await api.createTransaction(payload);
      }
      await onCreated?.();
      toast.success(
        isEditing
          ? isIncome
            ? "Ingreso actualizado"
            : "Gasto actualizado"
          : isIncome
            ? "Ingreso registrado"
            : "Gasto registrado"
      );
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
          <DialogTitle>
            {isEditing ? "Editar" : "Nuevo"} {isIncome ? "ingreso" : "gasto"}
          </DialogTitle>
        </DialogHeader>
        <form
          onSubmit={handleSubmit}
          className="grid grid-cols-1 md:grid-cols-2 gap-4"
        >
          <Field label="Descripción">
            <Input
              placeholder="Ej. Supermercado"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </Field>
          <Field label="Monto">
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
          {!isIncome ? (
            <Field label="¿Cómo se paga?">
              <select
                className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
                value={target}
                onChange={(e) => setTarget(e.target.value as "account" | "card")}
              >
                <option value="account">Pagar con cuenta</option>
                <option value="card">Cargar a tarjeta</option>
              </select>
            </Field>
          ) : null}
          {target === "card" ? (
            <>
              <Field label="Tarjeta">
                <select
                  className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
                  value={cardId}
                  onChange={(e) => setCardId(e.target.value)}
                  required
                >
                  <option value="">Elegí una tarjeta...</option>
                  {cards.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.holderName}
                    </option>
                  ))}
                </select>
              </Field>
              <Field
                label="Cuotas"
                hint={
                  Number(installments) > 1 && Number(amount) > 0
                    ? `${installments} cuotas de ${formatMoney(Number(amount) / Number(installments))}`
                    : "1 = pago en un solo pago"
                }
              >
                <Input
                  type="number"
                  min="1"
                  max="36"
                  step="1"
                  value={installments}
                  onChange={(e) => setInstallments(e.target.value)}
                  required
                />
              </Field>
            </>
          ) : (
            <Field label="Cuenta">
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
          )}
          <Field label="Categoría" className="md:col-span-2">
            <select
              className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              required
            >
              <option value="">Elegí una categoría...</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </Field>
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
