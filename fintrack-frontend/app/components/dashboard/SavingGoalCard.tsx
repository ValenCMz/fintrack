import { Target } from "lucide-react";

interface SavingGoalCardProps {
  name: string;
  currentAmount: number;
  targetAmount: number;
  targetDate?: string;
}

const SavingGoalCard = ({
  name,
  currentAmount,
  targetAmount,
  targetDate,
}: SavingGoalCardProps) => {
  const progress = Math.min((currentAmount / targetAmount) * 100, 100);
  const remaining = targetAmount - currentAmount;

  return (
    <div className="card-gradient rounded-xl p-5 animate-slide-up">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-primary/20">
            <Target className="w-5 h-5 text-primary" />
          </div>
          <div>
            <h4 className="font-medium text-foreground">{name}</h4>
            {targetDate && (
              <p className="text-xs text-muted-foreground">
                Meta: {targetDate}
              </p>
            )}
          </div>
        </div>
        <span className="text-sm font-medium text-primary">
          {progress.toFixed(0)}%
        </span>
      </div>

      <div className="progress-bar mb-3">
        <div className="progress-fill" style={{ width: `${progress}%` }} />
      </div>

      <div className="flex justify-between text-sm">
        <span className="text-muted-foreground">
          ${currentAmount.toLocaleString()} ahorrados
        </span>
        <span className="text-foreground font-medium">
          ${targetAmount.toLocaleString()}
        </span>
      </div>

      {remaining > 0 && (
        <p className="text-xs text-muted-foreground mt-2">
          Faltan ${remaining.toLocaleString()} para tu meta
        </p>
      )}
    </div>
  );
};

export default SavingGoalCard;
