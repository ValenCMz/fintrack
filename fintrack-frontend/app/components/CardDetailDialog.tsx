"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";
import { toast } from "sonner";
import { api, Account, CardDetail, CardInstallment } from "@/lib/api";
import { formatDate, formatMoney, todayISO } from "@/lib/format";

interface CardDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  cardId: string | null;
  onChanged?: () => void | Promise<void>;
}

type PayTarget =
  | { kind: "installment"; id: string; label: string; amount: number }
  | { kind: "item"; id: string; label: string; amount: number };

const CardDetailDialog = ({
  open,
  onOpenChange,
  cardId,
  onChanged,
}: CardDetailDialogProps) => {
  const [detail, setDetail] = useState<CardDetail | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [payTarget, setPayTarget] = useState<PayTarget | null>(null);
  const [accountId, setAccountId] = useState("");
  const [date, setDate] = useState(todayISO());

  const load = useCallback(async () => {
    if (!cardId) return;
    setLoading(true);
    try {
      const [card, allAccounts] = await Promise.all([
        api.getCardDetail(cardId),
        api.listAccounts(),
      ]);
      setDetail(card);
      const active = allAccounts.filter((a) => a.active);
      setAccounts(active);
      if (!accountId && active.length > 0) setAccountId(active[0].id);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    } finally {
      setLoading(false);
    }
    // accountId se lee para no pisar la seleccion del usuario al recargar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cardId]);

  useEffect(() => {
    if (!open) {
      setDetail(null);
      setPayTarget(null);
      setAccountId("");
      setDate(todayISO());
      return;
    }
    load();
  }, [open, load]);

  const grouped = useMemo(() => {
    if (!detail) return [];
    const map = new Map<string, CardInstallment[]>();
    for (const installment of detail.installments) {
      const list = map.get(installment.transactionId) ?? [];
      list.push(installment);
      map.set(installment.transactionId, list);
    }
    return Array.from(map.entries()).map(([transactionId, installments]) => {
      const purchase = detail.purchases.find((p) => p.id === transactionId);
      const unpaid = installments.filter((i) => !i.paid);
      return {
        transactionId,
        installments,
        description: purchase?.description ?? installments[0]?.description ?? "Compra",
        total: purchase?.amount ?? installments.reduce((sum, i) => sum + Number(i.amount), 0),
        unpaidAmount: unpaid.reduce((sum, i) => sum + Number(i.amount), 0),
        unpaidCount: unpaid.length,
      };
    });
  }, [detail]);

  async function handlePay(e: React.FormEvent) {
    e.preventDefault();
    if (!cardId || !payTarget) return;
    if (!accountId) {
      toast.error("Seleccioná una cuenta");
      return;
    }
    setSaving(true);
    try {
      if (payTarget.kind === "installment") {
        await api.payCardInstallment(cardId, payTarget.id, { accountId, date });
      } else {
        await api.payCardItem(cardId, payTarget.id, { accountId, date });
      }
      toast.success("Pago registrado");
      setPayTarget(null);
      await load();
      await onChanged?.();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al registrar el pago");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl" aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>
            {detail ? detail.holderName : "Detalle de la tarjeta"}
          </DialogTitle>
        </DialogHeader>

        {loading || !detail ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : (
          <div className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="rounded-xl bg-secondary/50 p-4">
                <p className="text-xs text-muted-foreground">Saldo pendiente</p>
                <p className="font-display text-2xl font-semibold text-expense">
                  {formatMoney(detail.outstanding)}
                </p>
              </div>
              <div className="rounded-xl bg-secondary/50 p-4">
                <p className="text-xs text-muted-foreground">Próxima cuota</p>
                <p className="font-display text-2xl font-semibold">
                  {detail.nextInstallmentAmount != null
                    ? formatMoney(detail.nextInstallmentAmount)
                    : "—"}
                </p>
                <p className="text-xs text-muted-foreground">
                  {detail.nextInstallmentDueDate
                    ? formatDate(detail.nextInstallmentDueDate)
                    : "Sin cuotas impagas"}
                </p>
              </div>
              <div className="rounded-xl bg-secondary/50 p-4">
                <p className="text-xs text-muted-foreground">Cierre</p>
                <p className="font-display text-2xl font-semibold">
                  {formatDate(detail.nextDueDate)}
                </p>
              </div>
            </div>

            <div>
              <h3 className="font-medium mb-2">Detalles</h3>
              {grouped.length === 0 ? (
                <p className="text-sm text-muted-foreground">Sin compras cargadas.</p>
              ) : (
                <div className="space-y-3">
                  {grouped.map((item) => (
                    <div key={item.transactionId} className="rounded-lg bg-secondary/40 p-3">
                      <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                        <div>
                          <p className="text-sm font-medium">{item.description}</p>
                          <p className="text-xs text-muted-foreground">
                            {item.installments.length}{" "}
                            {item.installments.length === 1 ? "cuota" : "cuotas"} ·{" "}
                            {formatMoney(item.total)}
                          </p>
                        </div>
                        {item.unpaidCount > 0 && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() =>
                              setPayTarget({
                                kind: "item",
                                id: item.transactionId,
                                label: item.description,
                                amount: item.unpaidAmount,
                              })
                            }
                          >
                            Pagar detalle ({formatMoney(item.unpaidAmount)})
                          </Button>
                        )}
                      </div>

                      <div className="space-y-1">
                        {item.installments.map((installment) => (
                          <div
                            key={installment.id}
                            className="flex items-center justify-between text-sm px-2 py-1.5 rounded-md bg-background/40"
                          >
                            <span className="flex items-center gap-2">
                              <span className="text-xs text-muted-foreground w-16">
                                Cuota {installment.number}/{installment.installments}
                              </span>
                              <span>{formatDate(installment.dueDate)}</span>
                            </span>
                            <span className="flex items-center gap-3">
                              <span className="font-medium">
                                {formatMoney(installment.amount)}
                              </span>
                              {installment.paid ? (
                                <span className="text-xs text-income">
                                  Paga {installment.paidDate ? `· ${formatDate(installment.paidDate)}` : ""}
                                </span>
                              ) : (
                                <button
                                  onClick={() =>
                                    setPayTarget({
                                      kind: "installment",
                                      id: installment.id,
                                      label: `Cuota ${installment.number} de ${item.description}`,
                                      amount: Number(installment.amount),
                                    })
                                  }
                                  className="text-xs text-primary hover:underline"
                                >
                                  Pagar
                                </button>
                              )}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {payTarget && (
              <form
                onSubmit={handlePay}
                className="grid gap-3 border-t border-border/50 pt-4"
              >
                <h3 className="font-medium">
                  Pagar {payTarget.label} · {formatMoney(payTarget.amount)}
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
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
                  <Field label="Fecha del pago">
                    <Input
                      type="date"
                      value={date}
                      onChange={(e) => setDate(e.target.value)}
                      required
                    />
                  </Field>
                </div>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => setPayTarget(null)}>
                    Cancelar
                  </Button>
                  <Button type="submit" disabled={saving}>
                    {saving ? "Registrando..." : "Confirmar pago"}
                  </Button>
                </div>
              </form>
            )}

            <div>
              <h3 className="font-medium mb-2">Pagos registrados</h3>
              {detail.payments.length === 0 ? (
                <p className="text-sm text-muted-foreground">Sin pagos registrados.</p>
              ) : (
                <div className="space-y-2">
                  {detail.payments.map((p) => (
                    <div
                      key={p.id}
                      className="flex items-center justify-between text-sm rounded-lg bg-secondary/40 px-3 py-2"
                    >
                      <span>
                        {p.accountName ?? "Cuenta"}
                        <span className="text-muted-foreground"> · {formatDate(p.date)}</span>
                      </span>
                      <span className="font-medium text-income">
                        {formatMoney(p.amount)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default CardDetailDialog;
