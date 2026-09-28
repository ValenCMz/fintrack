"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FileText, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, Monotributo } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function MonotributoPage() {
  const [items, setItems] = useState<Monotributo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [monthlyAmount, setMonthlyAmount] = useState("");
  const [dueDay, setDueDay] = useState(todayISO());

  async function load() {
    setItems(await api.listMonotributo());
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
      await api.createMonotributo({
        name,
        monthlyAmount: Number(monthlyAmount),
        dueDay,
        status: "UP_TO_DATE",
      });
      await load();
      setName("");
      setMonthlyAmount("");
      setShowForm(false);
      toast.success("Monotributo registrado");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteMonotributo(id);
      await load();
      toast.success("Monotributo eliminado");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <FileText className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Monotributo</h1>
            <p className="text-muted-foreground">Controlá tus pagos de monotributo</p>
          </div>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="w-4 h-4 mr-2" />
          Nuevo
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="card-gradient rounded-xl p-6 mb-6 grid grid-cols-1 md:grid-cols-3 gap-4"
        >
          <Input
            placeholder="Categoría"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <Input
            type="date"
            value={dueDay}
            onChange={(e) => setDueDay(e.target.value)}
            required
          />
          <Input
            type="number"
            step="0.01"
            min="0"
            placeholder="Monto mensual"
            value={monthlyAmount}
            onChange={(e) => setMonthlyAmount(e.target.value)}
            required
          />
          <div className="md:col-span-3 flex gap-3">
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
          <p className="text-muted-foreground text-sm">No tenés registros.</p>
        ) : (
          <div className="space-y-3">
            {items.map((m) => (
              <div
                key={m.id}
                className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
              >
                <div>
                  <p className="font-medium">{m.name}</p>
                  <p className="text-sm text-muted-foreground">
                    Vence {formatDate(m.dueDay)} ·{" "}
                    {m.status === "UP_TO_DATE" ? "Al día" : "Con deuda"}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <p className="font-display font-semibold">
                    {formatMoney(m.monthlyAmount)}
                  </p>
                  <button
                    onClick={() => handleDelete(m.id)}
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
