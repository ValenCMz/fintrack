"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/app/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";

export default function LoginPage() {
  const { login, register } = useAuth();
  const router = useRouter();

  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      if (mode === "login") {
        await login(email, password);
      } else {
        await register(username, email, password);
      }
      router.replace("/");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al iniciar sesión");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-2 mb-8">
          <span className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-primary-foreground text-xl font-bold">
            $
          </span>
          <h1 className="font-display text-3xl font-bold">Fintrack</h1>
        </div>

        <form
          onSubmit={handleSubmit}
          className="card-gradient rounded-2xl p-8 space-y-4"
        >
          <h2 className="font-display text-2xl font-bold">
            {mode === "login" ? "Iniciar sesión" : "Crear cuenta"}
          </h2>
          <p className="text-muted-foreground text-sm -mt-2">
            {mode === "login"
              ? "Ingresá con tu email y contraseña"
              : "Completá tus datos para registrarte"}
          </p>

          {mode === "register" && (
            <Input
              placeholder="Nombre de usuario"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          )}

          <Input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <Input
            type="password"
            placeholder="Contraseña"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
          />

          <Button type="submit" className="w-full" disabled={loading}>
            {loading
              ? "Cargando..."
              : mode === "login"
                ? "Ingresar"
                : "Registrarme"}
          </Button>

          <button
            type="button"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
            className="w-full text-center text-sm text-primary hover:underline"
          >
            {mode === "login"
              ? "¿No tenés cuenta? Registrate"
              : "¿Ya tenés cuenta? Ingresá"}
          </button>
        </form>
      </div>
    </div>
  );
}
