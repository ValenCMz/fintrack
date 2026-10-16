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
import { toast } from "sonner";
import { api, Card } from "@/lib/api";
import { todayISO } from "@/lib/format";

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
  const [dueDay, setDueDay] = useState(todayISO());
  const [amount, setAmount] = useState("");

  useEffect(() => {
    if (!open) return;
    if (card) {
      setHolderName(card.holderName);
      setDueDay(card.dueDay);
      setAmount(String(card.amount));
    } else {
      setHolderName("");
      setDueDay(todayISO());
      setAmount("");
    }
  }, [open, card]);

  function reset() {
    setHolderName("");
    setDueDay(todayISO());
    setAmount("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        holderName,
        dueDay,
        amount: Number(amount),
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
          <Input
            placeholder="Titular"
            value={holderName}
            onChange={(e) => setHolderName(e.target.value)}
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
            placeholder="Monto a pagar"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
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
