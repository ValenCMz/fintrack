"use client";

import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Settings, LogOut } from "lucide-react";
import { useAuth } from "@/app/context/AuthContext";

export default function SettingsPage() {
  const { user, logout } = useAuth();

  return (
    <MainLayout>
      <div className="flex items-center gap-4 mb-8">
        <div className="p-3 rounded-xl bg-primary/20">
          <Settings className="w-8 h-8 text-primary" />
        </div>
        <div>
          <h1 className="font-display text-3xl font-bold">Configuración</h1>
          <p className="text-muted-foreground">Tu cuenta y preferencias</p>
        </div>
      </div>

      <div className="card-gradient rounded-xl p-6 max-w-md">
        <h3 className="font-display font-semibold mb-4">Perfil</h3>
        <div className="space-y-3 text-sm">
          <div>
            <p className="text-muted-foreground">Usuario</p>
            <p className="font-medium">{user?.username ?? "—"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Email</p>
            <p className="font-medium">{user?.email ?? "—"}</p>
          </div>
        </div>

        <Button
          variant="outline"
          className="mt-6 gap-2 text-expense border-expense/40 hover:bg-expense/10"
          onClick={logout}
        >
          <LogOut className="w-4 h-4" />
          Cerrar sesión
        </Button>
      </div>
    </MainLayout>
  );
}
