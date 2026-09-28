"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CreditCard, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, Card } from "@/lib/api";
import { formatMoney, formatDate, todayISO } from "@/lib/format";

export default function TarjetasPage() {
  const [cards, setCards] = useState<Card[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const [holderName, setHolderName] = useState("");
  const [dueDay, setDueDay] = useState(todayISO());
  const [amount, setAmount] = useState("");

  async function load() {
    setCards(await api.listCards());
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
      await api.createCard({
        holderName,
        dueDay,
        amount: Number(amount),
        active: true,
      });
      await load();
      setHolderName("");
      setAmount("");
      setShowForm(false);
      toast.success("Tarjeta creada");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteCard(id);
      await load();
      toast.success("Tarjeta eliminada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <CreditCard className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Tarjetas</h1>
            <p className="text-muted-foreground">Administrá tus tarjetas de crédito</p>
          </div>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva tarjeta
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="card-gradient rounded-xl p-6 mb-6 grid grid-cols-1 md:grid-cols-3 gap-4"
        >
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
        ) : cards.length === 0 ? (
          <p className="text-muted-foreground text-sm">No tenés tarjetas.</p>
        ) : (
          <div className="space-y-3">
            {cards.map((c) => (
              <div
                key={c.id}
                className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
              >
                <div>
                  <p className="font-medium">{c.holderName}</p>
                  <p className="text-sm text-muted-foreground">
                    Vence {formatDate(c.dueDay)}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <p className="font-display font-semibold text-expense">
                    {formatMoney(c.amount)}
                  </p>
                  <button
                    onClick={() => handleDelete(c.id)}
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
