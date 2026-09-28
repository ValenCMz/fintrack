"use client";

import { useEffect, useState } from "react";
import { Calendar, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import { api } from "@/lib/api";
import { formatMoney, formatDate } from "@/lib/format";

interface Payment {
  id: string;
  name: string;
  amount: number;
  dueDate: string;
  type: "fixed" | "card" | "monotributo";
}

const UpcomingPayments = () => {
  const [payments, setPayments] = useState<Payment[]>([]);

  useEffect(() => {
    (async () => {
      try {
        const [fixed, cards, monotributo] = await Promise.all([
          api.listFixedExpenses(),
          api.listCards(),
          api.listMonotributo(),
        ]);
        const all: Payment[] = [
          ...fixed
            .filter((f) => f.active)
            .map((f) => ({
              id: f.id,
              name: f.name,
              amount: Number(f.amount),
              dueDate: f.dueDay,
              type: "fixed" as const,
            })),
          ...cards
            .filter((c) => c.active)
            .map((c) => ({
              id: c.id,
              name: c.holderName,
              amount: Number(c.amount),
              dueDate: c.dueDay,
              type: "card" as const,
            })),
          ...monotributo.map((m) => ({
            id: m.id,
            name: m.name,
            amount: Number(m.monthlyAmount),
            dueDate: m.dueDay,
            type: "monotributo" as const,
          })),
        ];
        all.sort((a, b) => a.dueDate.localeCompare(b.dueDate));
        setPayments(all.slice(0, 6));
      } catch {
        // silencioso
      }
    })();
  }, []);

  const daysLeft = (iso: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(iso);
    return Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  };

  return (
    <div className="card-gradient rounded-xl p-5 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-display font-semibold text-lg">Próximos Pagos</h3>
        <Calendar className="w-5 h-5 text-muted-foreground" />
      </div>

      {payments.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No hay vencimientos próximos.
        </p>
      ) : (
        <div className="space-y-3">
          {payments.map((payment) => {
            const left = daysLeft(payment.dueDate);
            return (
              <div
                key={payment.id}
                className="flex items-center justify-between p-3 rounded-lg bg-secondary/50"
              >
                <div className="flex items-center gap-3">
                  {left <= 7 && <AlertCircle className="w-4 h-4 text-warning" />}
                  <div>
                    <p className="font-medium text-sm">{payment.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {formatDate(payment.dueDate)}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="font-display font-semibold text-expense">
                    {formatMoney(payment.amount)}
                  </p>
                  <p
                    className={cn(
                      "text-xs",
                      left <= 7 ? "text-warning" : "text-muted-foreground"
                    )}
                  >
                    {left < 0 ? "vencido" : `${left} días`}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default UpcomingPayments;
