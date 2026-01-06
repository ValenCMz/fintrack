import { Calendar, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface Payment {
  id: number;
  name: string;
  amount: number;
  dueDate: string;
  daysLeft: number;
  type: "fixed" | "card" | "monotributo";
}

const mockPayments: Payment[] = [
  {
    id: 1,
    name: "Netflix",
    amount: 5999,
    dueDate: "10 Ene",
    daysLeft: 6,
    type: "fixed",
  },
  {
    id: 2,
    name: "Tarjeta Visa",
    amount: 45000,
    dueDate: "15 Ene",
    daysLeft: 11,
    type: "card",
  },
  {
    id: 3,
    name: "Monotributo",
    amount: 8500,
    dueDate: "20 Ene",
    daysLeft: 16,
    type: "monotributo",
  },
  {
    id: 4,
    name: "Spotify",
    amount: 2999,
    dueDate: "22 Ene",
    daysLeft: 18,
    type: "fixed",
  },
];

const UpcomingPayments = () => {
  return (
    <div className="card-gradient rounded-xl p-5 animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-display font-semibold text-lg">Próximos Pagos</h3>
        <Calendar className="w-5 h-5 text-muted-foreground" />
      </div>

      <div className="space-y-3">
        {mockPayments.map((payment) => (
          <div
            key={payment.id}
            className="flex items-center justify-between p-3 rounded-lg bg-secondary/50"
          >
            <div className="flex items-center gap-3">
              {payment.daysLeft <= 7 && (
                <AlertCircle className="w-4 h-4 text-warning" />
              )}
              <div>
                <p className="font-medium text-sm">{payment.name}</p>
                <p className="text-xs text-muted-foreground">
                  {payment.dueDate}
                </p>
              </div>
            </div>
            <div className="text-right">
              <p className="font-display font-semibold text-expense">
                ${payment.amount.toLocaleString()}
              </p>
              <p
                className={cn(
                  "text-xs",
                  payment.daysLeft <= 7
                    ? "text-warning"
                    : "text-muted-foreground"
                )}
              >
                {payment.daysLeft} días
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UpcomingPayments;
