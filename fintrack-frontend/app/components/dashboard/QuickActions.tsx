import { Plus, ArrowUpCircle, ArrowDownCircle, CreditCard } from "lucide-react";
import { Button } from "@/components/ui/button";

const QuickActions = () => {
  return (
    <div className="card-gradient rounded-xl p-5 animate-slide-up">
      <h3 className="font-display font-semibold text-lg mb-4">
        Acciones Rápidas
      </h3>

      <div className="grid grid-cols-2 gap-3">
        <Button
          variant="outline"
          className="h-auto py-4 flex flex-col items-center gap-2 bg-income/10 border-income/30 hover:bg-income/20 text-income"
        >
          <ArrowUpCircle className="w-6 h-6" />
          <span className="text-sm">Nuevo Ingreso</span>
        </Button>

        <Button
          variant="outline"
          className="h-auto py-4 flex flex-col items-center gap-2 bg-expense/10 border-expense/30 hover:bg-expense/20 text-expense"
        >
          <ArrowDownCircle className="w-6 h-6" />
          <span className="text-sm">Nuevo Gasto</span>
        </Button>

        <Button
          variant="outline"
          className="h-auto py-4 flex flex-col items-center gap-2 bg-info/10 border-info/30 hover:bg-info/20 text-info"
        >
          <CreditCard className="w-6 h-6" />
          <span className="text-sm">Pago Tarjeta</span>
        </Button>

        <Button
          variant="outline"
          className="h-auto py-4 flex flex-col items-center gap-2 bg-primary/10 border-primary/30 hover:bg-primary/20 text-primary"
        >
          <Plus className="w-6 h-6" />
          <span className="text-sm">Nuevo Ahorro</span>
        </Button>
      </div>
    </div>
  );
};

export default QuickActions;
