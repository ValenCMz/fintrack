"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Landmark, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, Debt } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function DeudasPage() {
  const [debts, setDebts] = useState<Debt[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

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

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await api.createDebt({
        creditor,
        totalAmount: Number(totalAmount),
        remainingAmount: Number(remainingAmount),
        startDate,
        status: "ACTIVE",
      });
      await load();
      setCreditor("");
      setTotalAmount("");
      setRemainingAmount("");
      setShowForm(false);
      toast.success("Deuda registrada");
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
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva deuda
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="card-gradient rounded-xl p-6 mb-6 grid grid-cols-1 md:grid-cols-2 gap-4"
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
                    onClick={() => handleDelete(d.id)}
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
