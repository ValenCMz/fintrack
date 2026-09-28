"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PiggyBank, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, SavingGoal } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function AhorrosPage() {
  const [goals, setGoals] = useState<SavingGoal[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [currentAmount, setCurrentAmount] = useState("");
  const [targetDate, setTargetDate] = useState(todayISO());

  async function load() {
    setGoals(await api.listSavingGoals());
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
      await api.createSavingGoal({
        name,
        targetAmount: Number(targetAmount),
        currentAmount: Number(currentAmount || 0),
        targetDate,
        active: true,
      });
      await load();
      setName("");
      setTargetAmount("");
      setCurrentAmount("");
      setShowForm(false);
      toast.success("Meta creada");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteSavingGoal(id);
      await load();
      toast.success("Meta eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <PiggyBank className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Metas de Ahorro</h1>
            <p className="text-muted-foreground">Definí objetivos y seguí tu progreso</p>
          </div>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva meta
        </Button>
      </div>

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
            type="date"
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
            required
          />
          <Input
            type="number"
            step="0.01"
            min="0"
            placeholder="Objetivo"
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
            required
          />
          <Input
            type="number"
            step="0.01"
            min="0"
            placeholder="Ahorrado hasta ahora"
            value={currentAmount}
            onChange={(e) => setCurrentAmount(e.target.value)}
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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {loading ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : goals.length === 0 ? (
          <p className="text-muted-foreground text-sm">No tenés metas de ahorro.</p>
        ) : (
          goals.map((g) => {
            const progress = Math.min(
              (Number(g.currentAmount) / Number(g.targetAmount)) * 100,
              100
            );
            return (
              <div key={g.id} className="card-gradient rounded-xl p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h4 className="font-medium">{g.name}</h4>
                    <p className="text-xs text-muted-foreground">
                      Meta: {formatDate(g.targetDate)}
                    </p>
                  </div>
                  <span className="text-sm font-medium text-primary">
                    {progress.toFixed(0)}%
                  </span>
                </div>
                <div className="progress-bar mb-3">
                  <div className="progress-fill" style={{ width: `${progress}%` }} />
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-muted-foreground">
                    {formatMoney(g.currentAmount)} de {formatMoney(g.targetAmount)}
                  </span>
                  <button
                    onClick={() => handleDelete(g.id)}
                    className="text-xs text-muted-foreground hover:text-expense"
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </MainLayout>
  );
}
