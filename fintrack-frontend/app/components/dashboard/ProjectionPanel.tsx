import { Projection } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { TrendingUp } from "lucide-react";

interface ProjectionPanelProps {
  projections: Projection[];
}

/**
 * "2026-10" viene del backend como YearMonth.toString(). No se puede pasar
 * directo a new Date() porque "2026-10" se parsea como UTC y en Argentina
 * corre hacia el dia anterior, mostrando "septiembre". Se parte el string.
 */
function monthLabel(ym: string): string {
  const [y, m] = ym.split("-");
  if (!y || !m) return ym;
  const date = new Date(Number(y), Number(m) - 1, 1);
  return date.toLocaleDateString("es-AR", { month: "short", year: "2-digit" });
}

const ProjectionPanel = ({ projections }: ProjectionPanelProps) => {
  return (
    <div className="card-gradient rounded-xl p-5 animate-slide-up">
      <div className="flex items-start justify-between mb-5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-primary/20">
            <TrendingUp className="w-5 h-5 text-primary" />
          </div>
          <div>
            <h3 className="font-display font-semibold text-lg">Proyección</h3>
            <p className="text-xs text-muted-foreground">
              Basada en el promedio de tus últimos 3 meses
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {projections.map((p) => {
          const net = Number(p.estimatedNet);
          const positive = net >= 0;
          return (
            <div key={p.month} className="space-y-2">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
                {monthLabel(p.month)}
              </p>
              <p
                className={
                  positive
                    ? "font-display text-xl font-bold text-income"
                    : "font-display text-xl font-bold text-expense"
                }
              >
                {formatMoney(net)}
              </p>
              <div className="space-y-1 text-xs text-muted-foreground">
                <p className="text-income">
                  +{formatMoney(p.estimatedIncome)}
                </p>
                <p className="text-expense">
                  -{formatMoney(p.estimatedExpense)}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ProjectionPanel;
