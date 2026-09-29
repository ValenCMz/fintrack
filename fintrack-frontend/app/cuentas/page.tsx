"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Landmark, Plus, Pencil } from "lucide-react";
import { toast } from "sonner";
import { api, Account, AccountType } from "@/lib/api";

const TYPE_LABELS: Record<AccountType, string> = {
  CASH: "Efectivo",
  WALLET: "Billetera",
  BANK: "Banco",
  CARD: "Tarjeta",
};

const EMPTY_FORM = { name: "", type: "CASH" as AccountType, owner: "" };

export default function CuentasPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [form, setForm] = useState(EMPTY_FORM);

  async function load() {
    setAccounts(await api.listAccounts());
  }

  useEffect(() => {
    load()
      .catch((e) => toast.error(e instanceof Error ? e.message : "Error"))
      .finally(() => setLoading(false));
  }, []);

  function resetForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setShowForm(false);
  }

  function startEdit(account: Account) {
    setForm({ name: account.name, type: account.type, owner: account.owner ?? "" });
    setEditingId(account.id);
    setShowForm(true);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        name: form.name,
        type: form.type,
        owner: form.owner,
        active: true,
      };

      if (editingId) {
        await api.updateAccount(editingId, payload);
        toast.success("Cuenta actualizada");
      } else {
        await api.createAccount(payload);
        toast.success("Cuenta creada");
      }

      await load();
      resetForm();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    try {
      await api.deleteAccount(id);
      await load();
      if (editingId === id) resetForm();
      toast.success("Cuenta desactivada");
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
            <h1 className="font-display text-3xl font-bold">Cuentas</h1>
            <p className="text-muted-foreground">Administrá tus cuentas y billeteras</p>
          </div>
        </div>
        <Button onClick={() => (editingId ? resetForm() : setShowForm((v) => !v))}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva cuenta
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit}
          className="card-gradient rounded-xl p-6 mb-6 grid grid-cols-1 md:grid-cols-3 gap-4"
        >
          <Input
            placeholder="Nombre"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <select
            className="bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as AccountType })}
          >
            {(Object.keys(TYPE_LABELS) as AccountType[]).map((t) => (
              <option key={t} value={t}>
                {TYPE_LABELS[t]}
              </option>
            ))}
          </select>
          <Input
            placeholder="Titular (opcional)"
            value={form.owner}
            onChange={(e) => setForm({ ...form, owner: e.target.value })}
          />
          <div className="md:col-span-3 flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? "Guardando..." : editingId ? "Guardar cambios" : "Guardar"}
            </Button>
            <Button type="button" variant="outline" onClick={resetForm}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <div className="card-gradient rounded-xl p-5">
        {loading ? (
          <p className="text-muted-foreground text-sm">Cargando...</p>
        ) : accounts.length === 0 ? (
          <div className="text-center py-6">
            <p className="text-muted-foreground text-sm">
              No tenés cuentas. Creá una para poder registrar movimientos.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {accounts.map((a) => (
              <div
                key={a.id}
                className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
              >
                <div>
                  <p className="font-medium">{a.name}</p>
                  <p className="text-sm text-muted-foreground">
                    {TYPE_LABELS[a.type]}
                    {a.owner ? ` · ${a.owner}` : ""}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <button
                    onClick={() => startEdit(a)}
                    className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                  >
                    <Pencil className="w-3 h-3" />
                    Editar
                  </button>
                  <button
                    onClick={() => handleDelete(a.id)}
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
