import { ArrowUpCircle, ArrowDownCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface TransactionItemProps {
  type: "income" | "expense";
  description: string;
  category: string;
  amount: number;
  date: string;
  account: string;
}

const TransactionItem = ({
  type,
  description,
  category,
  amount,
  date,
  account,
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

      <div className="text-right">
        <p
          className={cn(
            "font-display font-semibold",
            isIncome ? "text-income" : "text-expense"
          )}
        >
          {isIncome ? "+" : "-"}${amount.toLocaleString()}
        </p>
        <p className="text-xs text-muted-foreground">{date}</p>
      </div>
    </div>
  );
};

export default TransactionItem;
