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
import { FileText, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, Monotributo } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function MonotributoPage() {
  const [items, setItems] = useState<Monotributo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<Monotributo | null>(null);

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

  function openCreate() {
    setEditing(null);
    setName("");
    setMonthlyAmount("");
    setDueDay(todayISO());
    setShowForm(true);
  }

  function openEdit(item: Monotributo) {
    setEditing(item);
    setName(item.name);
    setMonthlyAmount(String(item.monthlyAmount));
    setDueDay(item.dueDay);
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
        name,
        monthlyAmount: Number(monthlyAmount),
        dueDay,
        status: editing?.status ?? "UP_TO_DATE",
      };
      if (editing) {
        await api.updateMonotributo(editing.id, payload);
      } else {
        await api.createMonotributo(payload);
      }
      await load();
      closeForm();
      setName("");
      setMonthlyAmount("");
      toast.success(editing ? "Monotributo actualizado" : "Monotributo registrado");
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
        <Button onClick={openCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Nuevo
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
            <DialogTitle>
              {editing ? "Editar monotributo" : "Nuevo monotributo"}
            </DialogTitle>
          </DialogHeader>
          <form
            onSubmit={handleSubmit}
            className="grid grid-cols-1 md:grid-cols-3 gap-4"
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
            <DialogFooter className="md:col-span-3">
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
                    onClick={() => openEdit(m)}
                    className="text-xs text-primary hover:underline"
                  >
                    Editar
                  </button>
                  <ConfirmDeleteButton
                    onConfirm={() => handleDelete(m.id)}
                    title="¿Eliminar monotributo?"
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
