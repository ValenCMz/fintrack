"use client";

import {
  LayoutDashboard,
  ArrowUpCircle,
  ArrowDownCircle,
  CalendarClock,
  CreditCard,
  FileText,
  Landmark,
  PiggyBank,
  MessageCircle,
  Settings,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const navItems = [
  { icon: LayoutDashboard, label: "Dashboard", path: "/" },
  { icon: ArrowUpCircle, label: "Ingresos", path: "/ingresos" },
  { icon: ArrowDownCircle, label: "Egresos", path: "/egresos" },
  { icon: CalendarClock, label: "Gastos Fijos", path: "/gastos-fijos" },
  { icon: CreditCard, label: "Tarjetas", path: "/tarjetas" },
  { icon: FileText, label: "Monotributo", path: "/monotributo" },
  { icon: Landmark, label: "Deudas", path: "/deudas" },
  { icon: PiggyBank, label: "Ahorros", path: "/ahorros" },
];

const Sidebar = () => {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-sidebar border-r border-sidebar-border flex flex-col">
      {/* Logo */}
      <div className="p-6 border-b border-sidebar-border">
        <h1 className="font-display text-2xl font-bold text-foreground flex items-center gap-2">
          <span className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-primary-foreground text-sm">
            $
          </span>
          FinanzasApp
        </h1>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.path;

          return (
            <Link
              key={item.path}
              href={item.path}
              className={cn("nav-item", isActive && "active")}
            >
              <Icon className="w-5 h-5" />
              <span className="font-medium">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Bottom section */}
      <div className="p-4 border-t border-sidebar-border space-y-1">
        <Link href="/chat" className="nav-item">
          <MessageCircle className="w-5 h-5" />
          <span className="font-medium">Asistente IA</span>
        </Link>
        <Link href="/settings" className="nav-item">
          <Settings className="w-5 h-5" />
          <span className="font-medium">Configuración</span>
        </Link>
      </div>
    </aside>
  );
};

export default Sidebar;
