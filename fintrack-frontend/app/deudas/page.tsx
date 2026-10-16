"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import ConfirmDeleteButton from "@/app/components/ConfirmDeleteButton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Landmark, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, Debt } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function DeudasPage() {
  const [debts, setDebts] = useState<Debt[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<Debt | null>(null);

  const [creditor, setCreditor] = useState("");
  const [totalAmount, setTotalAmount] = useState("");
  const [remainingAmount, setRemainingAmount] = useState("");
  const [startDate, setStartDate] = useState(todayISO());

  async function load() {
    setDebts(await api.listDebts());
  }

  useEffect(() => {
    load()
      .catch((e) => toast.error(e instanceof Error ? e.message : "Error"))
      .finally(() => setLoading(false));
  }, []);

  function openCreate() {
    setEditing(null);
    setCreditor("");
    setTotalAmount("");
    setRemainingAmount("");
    setStartDate(todayISO());
    setShowForm(true);
  }

  function openEdit(debt: Debt) {
    setEditing(debt);
    setCreditor(debt.creditor);
    setTotalAmount(String(debt.totalAmount));
    setRemainingAmount(String(debt.remainingAmount));
    setStartDate(debt.startDate);
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditing(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        creditor,
        totalAmount: Number(totalAmount),
        remainingAmount: Number(remainingAmount),
        startDate,
        status: editing?.status ?? "ACTIVE",
      };
      if (editing) {
        await api.updateDebt(editing.id, payload);
      } else {
        await api.createDebt(payload);
      }
      await load();
      closeForm();
      setCreditor("");
      setTotalAmount("");
      setRemainingAmount("");
      toast.success(editing ? "Deuda actualizada" : "Deuda registrada");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handlePay(id: string) {
    try {
      const debt = debts.find((d) => d.id === id);
      if (!debt) return;
      await api.updateDebt(id, {
        creditor: debt.creditor,
        totalAmount: debt.totalAmount,
        remainingAmount: 0,
        startDate: debt.startDate,
        status: "PAID",
      });
      await load();
      toast.success("Deuda marcada como pagada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteDebt(id);
      await load();
      toast.success("Deuda eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <Landmark className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Deudas</h1>
            <p className="text-muted-foreground">Registrá y seguí tus deudas</p>
          </div>
        </div>
        <Button onClick={openCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva deuda
        </Button>
      </div>

      <Dialog
        open={showForm}
        onOpenChange={(v) => {
          setShowForm(v);
          if (!v) setEditing(null);
        }}
      >
        <DialogContent aria-describedby={undefined}>
          <DialogHeader>
            <DialogTitle>{editing ? "Editar deuda" : "Nueva deuda"}</DialogTitle>
          </DialogHeader>
          <form
            onSubmit={handleSubmit}
            className="grid grid-cols-1 md:grid-cols-2 gap-4"
          >
            <Input
              placeholder="Acreedor"
              value={creditor}
              onChange={(e) => setCreditor(e.target.value)}
              required
            />
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="Monto total"
              value={totalAmount}
              onChange={(e) => setTotalAmount(e.target.value)}
              required
            />
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="Pendiente"
              value={remainingAmount}
              onChange={(e) => setRemainingAmount(e.target.value)}
              required
            />
            <DialogFooter className="md:col-span-2">
              <Button
                type="button"
                variant="outline"
                onClick={closeForm}
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

      <div className="card-gradient rounded-xl p-5">
        {loading ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : debts.length === 0 ? (
          <p className="text-muted-foreground text-sm">No tenés deudas registradas.</p>
        ) : (
          <div className="space-y-3">
            {debts.map((d) => (
              <div
                key={d.id}
                className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
              >
                <div>
                  <p className="font-medium">{d.creditor}</p>
                  <p className="text-sm text-muted-foreground">
                    {d.status === "PAID" ? "Pagada" : "Activa"} · inicio{" "}
                    {formatDate(d.startDate)}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="font-display font-semibold">
                      {formatMoney(d.remainingAmount)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      de {formatMoney(d.totalAmount)}
                    </p>
                  </div>
                  {d.status !== "PAID" && (
                    <button
                      onClick={() => handlePay(d.id)}
                      className="text-xs text-income hover:underline"
                    >
                      Marcar pagada
                    </button>
                  )}
                  <button
                    onClick={() => openEdit(d)}
                    className="text-xs text-primary hover:underline"
                  >
                    Editar
                  </button>
                  <ConfirmDeleteButton
                    onConfirm={() => handleDelete(d.id)}
                    title="¿Eliminar deuda?"
                    className="text-xs text-muted-foreground hover:text-expense"
                  >
                    Eliminar
                  </ConfirmDeleteButton>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
}
