"use client";

import { useState } from "react";
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
import { api } from "@/lib/api";
import { todayISO } from "@/lib/format";

interface CardFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void | Promise<void>;
}

const CardFormDialog = ({
  open,
  onOpenChange,
  onCreated,
}: CardFormDialogProps) => {
  const [saving, setSaving] = useState(false);

  const [holderName, setHolderName] = useState("");
  const [dueDay, setDueDay] = useState(todayISO());
  const [amount, setAmount] = useState("");

  function reset() {
    setHolderName("");
    setDueDay(todayISO());
    setAmount("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await api.createCard({
        holderName,
        dueDay,
        amount: Number(amount),
        active: true,
      });
      await onCreated?.();
      toast.success("Tarjeta creada");
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
          <DialogTitle>Nueva tarjeta</DialogTitle>
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
