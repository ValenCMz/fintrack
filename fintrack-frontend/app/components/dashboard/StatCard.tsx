import { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  title: string;
  value: string;
  subtitle?: string;
  icon: LucideIcon;
  trend?: {
    value: string;
    positive: boolean;
  };
  variant?: "default" | "income" | "expense";
}

const StatCard = ({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  variant = "default",
}: StatCardProps) => {
  return (
    <div className="stat-card animate-slide-up">
      <div className="flex items-start justify-between mb-4">
        <div
          className={cn(
            "p-3 rounded-xl",
            variant === "income" && "bg-income/20 text-income",
            variant === "expense" && "bg-expense/20 text-expense",
            variant === "default" && "bg-primary/20 text-primary"
          )}
        >
          <Icon className="w-6 h-6" />
        </div>
        {trend && (
          <span
            className={cn(
              "text-sm font-medium px-2 py-1 rounded-full",
              trend.positive
                ? "bg-income/20 text-income"
                : "bg-expense/20 text-expense"
            )}
          >
            {trend.positive ? "+" : ""}
            {trend.value}
          </span>
        )}
      </div>

      <p className="text-muted-foreground text-sm mb-1">{title}</p>
      <p
        className={cn(
          "font-display text-3xl font-bold",
          variant === "income" && "text-income",
          variant === "expense" && "text-expense"
        )}
      >
        {value}
      </p>
      {subtitle && (
        <p className="text-muted-foreground text-xs mt-1">{subtitle}</p>
      )}
    </div>
  );
};

export default StatCard;
