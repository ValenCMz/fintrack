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
import { api, SavingGoal } from "@/lib/api";
import { todayISO } from "@/lib/format";

interface SavingGoalFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void | Promise<void>;
  goal?: SavingGoal | null;
}

const SavingGoalFormDialog = ({
  open,
  onOpenChange,
  onCreated,
  goal,
}: SavingGoalFormDialogProps) => {
  const isEditing = !!goal;
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [targetDate, setTargetDate] = useState(todayISO());
  const [targetAmount, setTargetAmount] = useState("");
  const [currentAmount, setCurrentAmount] = useState("");

  useEffect(() => {
    if (!open) return;
    if (goal) {
      setName(goal.name);
      setTargetDate(goal.targetDate);
      setTargetAmount(String(goal.targetAmount));
      setCurrentAmount(String(goal.currentAmount));
    } else {
      setName("");
      setTargetDate(todayISO());
      setTargetAmount("");
      setCurrentAmount("");
    }
  }, [open, goal]);

  function reset() {
    setName("");
    setTargetDate(todayISO());
    setTargetAmount("");
    setCurrentAmount("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        name,
        targetAmount: Number(targetAmount),
        currentAmount: Number(currentAmount || 0),
        targetDate,
        active: goal?.active ?? true,
      };
      if (isEditing && goal) {
        await api.updateSavingGoal(goal.id, payload);
      } else {
        await api.createSavingGoal(payload);
      }
      await onCreated?.();
      toast.success(isEditing ? "Meta actualizada" : "Meta creada");
      reset();
      onOpenChange(false);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "Editar meta de ahorro" : "Nueva meta de ahorro"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="grid gap-4">
          <Field label="Nombre de la meta">
            <Input
              placeholder="Ej. Viaje a Bariloche"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </Field>
          <Field label="Fecha objetivo">
            <Input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              required
            />
          </Field>
          <Field label="Monto objetivo">
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={targetAmount}
              onChange={(e) => setTargetAmount(e.target.value)}
              required
            />
          </Field>
          <Field label="Monto ahorrado hasta ahora" hint="Opcional. Usá «Contribuir» para sumar aportes reales.">
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={currentAmount}
              onChange={(e) => setCurrentAmount(e.target.value)}
            />
          </Field>
          <DialogFooter>
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

export default SavingGoalFormDialog;
