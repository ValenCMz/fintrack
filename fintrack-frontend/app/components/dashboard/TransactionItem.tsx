import { ArrowUpCircle, ArrowDownCircle, Pencil } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatMoney, formatDate } from "@/lib/format";
import ConfirmDeleteButton from "@/app/components/ConfirmDeleteButton";

interface TransactionItemProps {
  type: "income" | "expense";
  description: string;
  category: string;
  amount: number;
  date: string;
  account: string;
  onDelete?: () => void;
  onEdit?: () => void;
}

const TransactionItem = ({
  type,
  description,
  category,
  amount,
  date,
  account,
  onDelete,
  onEdit,
}: TransactionItemProps) => {
  const isIncome = type === "income";

  return (
    <div className="flex items-center gap-4 p-4 rounded-xl bg-secondary/50 hover:bg-secondary transition-colors">
      <div
        className={cn(
          "p-2.5 rounded-xl",
          isIncome ? "bg-income/20" : "bg-expense/20"
        )}
      >
        {isIncome ? (
          <ArrowUpCircle className="w-5 h-5 text-income" />
        ) : (
          <ArrowDownCircle className="w-5 h-5 text-expense" />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <p className="font-medium text-foreground truncate">{description}</p>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>{category}</span>
          <span>•</span>
          <span>{account}</span>
        </div>
      </div>

      <div className="text-right shrink-0">
        <p
          className={cn(
            "font-display font-semibold",
            isIncome ? "text-income" : "text-expense"
          )}
        >
          {isIncome ? "+" : "-"}
          {formatMoney(amount)}
        </p>
        <p className="text-xs text-muted-foreground">{formatDate(date)}</p>
      </div>

      {onEdit && (
        <button
          onClick={onEdit}
          title="Editar movimiento"
          aria-label="Editar movimiento"
          className="shrink-0 text-muted-foreground hover:text-primary transition-colors"
        >
          <Pencil className="w-4 h-4" />
        </button>
      )}

      {onDelete && (
        <ConfirmDeleteButton
          onConfirm={onDelete}
          title="¿Eliminar movimiento?"
          className="shrink-0 text-xs text-muted-foreground hover:text-expense"
        >
          Eliminar
        </ConfirmDeleteButton>
      )}
    </div>
  );
};

export default TransactionItem;
