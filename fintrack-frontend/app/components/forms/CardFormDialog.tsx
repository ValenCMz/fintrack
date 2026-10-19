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
import { api, Card } from "@/lib/api";

interface CardFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void | Promise<void>;
  card?: Card | null;
}

const CardFormDialog = ({
  open,
  onOpenChange,
  onCreated,
  card,
}: CardFormDialogProps) => {
  const isEditing = !!card;
  const [saving, setSaving] = useState(false);

  const [holderName, setHolderName] = useState("");
  const [dueDayOfMonth, setDueDayOfMonth] = useState("10");

  useEffect(() => {
    if (!open) return;
    if (card) {
      setHolderName(card.holderName);
      setDueDayOfMonth(String(card.dueDayOfMonth));
    } else {
      setHolderName("");
      setDueDayOfMonth("10");
    }
  }, [open, card]);

  function reset() {
    setHolderName("");
    setDueDayOfMonth("10");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        holderName,
        dueDayOfMonth: Number(dueDayOfMonth),
        active: card?.active ?? true,
      };
      if (isEditing && card) {
        await api.updateCard(card.id, payload);
      } else {
        await api.createCard(payload);
      }
      await onCreated?.();
      toast.success(isEditing ? "Tarjeta actualizada" : "Tarjeta creada");
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
          <DialogTitle>{isEditing ? "Editar tarjeta" : "Nueva tarjeta"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="grid gap-4">
          <Field label="Titular">
            <Input
              placeholder="Ej. Visa Santander"
              value={holderName}
              onChange={(e) => setHolderName(e.target.value)}
              required
            />
          </Field>
          <Field
            label="Día de vencimiento del resumen"
            hint="Día del mes en que vence el resumen (1-31)"
          >
            <Input
              type="number"
              min="1"
              max="31"
              placeholder="10"
              value={dueDayOfMonth}
              onChange={(e) => setDueDayOfMonth(e.target.value)}
              required
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

export default CardFormDialog;
