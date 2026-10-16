"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import ConfirmDeleteButton from "@/app/components/ConfirmDeleteButton";
import SavingGoalFormDialog from "@/app/components/forms/SavingGoalFormDialog";
import { Button } from "@/components/ui/button";
import { PiggyBank, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, SavingGoal } from "@/lib/api";
import { formatMoney, formatDate } from "@/lib/format";

export default function AhorrosPage() {
  const [goals, setGoals] = useState<SavingGoal[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<SavingGoal | null>(null);

  async function load() {
    setGoals(await api.listSavingGoals());
  }

  useEffect(() => {
    (async () => {
      try {
        await load();
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Error");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  async function handleDelete(id: string) {
    try {
      await api.deleteSavingGoal(id);
      await load();
      toast.success("Meta eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  function openCreate() {
    setEditing(null);
    setShowForm(true);
  }

  function openEdit(goal: SavingGoal) {
    setEditing(goal);
    setShowForm(true);
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
        <Button onClick={openCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva meta
        </Button>
      </div>

      <SavingGoalFormDialog
        open={showForm}
        onOpenChange={(v) => {
          setShowForm(v);
          if (!v) setEditing(null);
        }}
        onCreated={load}
        goal={editing}
      />

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
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => openEdit(g)}
                      className="text-xs text-primary hover:underline"
                    >
                      Editar
                    </button>
                    <ConfirmDeleteButton
                      onConfirm={() => handleDelete(g.id)}
                      title="¿Eliminar meta de ahorro?"
                      className="text-xs text-muted-foreground hover:text-expense"
                    >
                      Eliminar
                    </ConfirmDeleteButton>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </MainLayout>
  );
}
