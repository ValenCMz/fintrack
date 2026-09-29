"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tags, Plus, Pencil } from "lucide-react";
import { toast } from "sonner";
import { api, Category, CategoryType } from "@/lib/api";

const TYPE_LABELS: Record<CategoryType, string> = {
  INCOME: "Ingreso",
  EXPENSE: "Egreso",
};

const EMPTY_FORM = { name: "", color: "#6366f1", type: "EXPENSE" as CategoryType };

export default function CategoriasPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [form, setForm] = useState(EMPTY_FORM);

  async function load() {
    setCategories(await api.listCategories());
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

  function startEdit(category: Category) {
    setForm({ name: category.name, color: category.color, type: category.type });
    setEditingId(category.id);
    setShowForm(true);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        name: form.name,
        color: form.color,
        type: form.type,
        active: true,
      };

      if (editingId) {
        await api.updateCategory(editingId, payload);
        toast.success("Categoría actualizada");
      } else {
        await api.createCategory(payload);
        toast.success("Categoría creada");
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
      await api.deleteCategory(id);
      await load();
      if (editingId === id) resetForm();
      toast.success("Categoría desactivada");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error");
    }
  }

  const income = categories.filter((c) => c.type === "INCOME");
  const expense = categories.filter((c) => c.type === "EXPENSE");

  return (
    <MainLayout>
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <div className="p-3 rounded-xl bg-primary/20">
            <Tags className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Categorías</h1>
            <p className="text-muted-foreground">Clasificá tus ingresos y egresos</p>
          </div>
        </div>
        <Button onClick={() => (editingId ? resetForm() : setShowForm((v) => !v))}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva categoría
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
            onChange={(e) => setForm({ ...form, type: e.target.value as CategoryType })}
          >
            {(Object.keys(TYPE_LABELS) as CategoryType[]).map((t) => (
              <option key={t} value={t}>
                {TYPE_LABELS[t]}
              </option>
            ))}
          </select>
          <label className="flex items-center gap-3 bg-secondary/50 border border-border/50 rounded-md px-3 py-2 text-sm">
            Color
            <input
              type="color"
              value={form.color}
              onChange={(e) => setForm({ ...form, color: e.target.value })}
              className="w-8 h-8 rounded cursor-pointer bg-transparent border-0"
            />
          </label>
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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {[
          { title: "Ingresos", items: income },
          { title: "Egresos", items: expense },
        ].map((group) => (
          <div key={group.title} className="card-gradient rounded-xl p-5">
            <h2 className="font-display font-semibold mb-4">{group.title}</h2>
            {loading ? (
              <p className="text-muted-foreground text-sm">Cargando...</p>
            ) : group.items.length === 0 ? (
              <p className="text-muted-foreground text-sm">Sin categorías.</p>
            ) : (
              <div className="space-y-3">
                {group.items.map((c) => (
                  <div
                    key={c.id}
                    className="flex items-center justify-between p-4 rounded-xl bg-secondary/50"
                  >
                    <div className="flex items-center gap-3">
                      <span
                        className="w-3 h-3 rounded-full shrink-0"
                        style={{ backgroundColor: c.color }}
                      />
                      <p className="font-medium">{c.name}</p>
                    </div>
                    <div className="flex items-center gap-4">
                      <button
                        onClick={() => startEdit(c)}
                        className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                      >
                        <Pencil className="w-3 h-3" />
                        Editar
                      </button>
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
        ))}
      </div>

      {!loading && categories.length === 0 && (
        <p className="text-muted-foreground text-sm text-center mt-4">
          No tenés categorías. Creá una para poder registrar movimientos.
        </p>
      )}
    </MainLayout>
  );
}
