"use client";

import { useEffect, useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import StatCard from "@/app/components/dashboard/StatCard";
import TransactionItem from "@/app/components/dashboard/TransactionItem";
import SavingGoalCard from "@/app/components/dashboard/SavingGoalCard";
import QuickActions from "@/app/components/dashboard/QuickActions";
import UpcomingPayments from "@/app/components/dashboard/UpcomingPayments";
import ProjectionPanel from "@/app/components/dashboard/ProjectionPanel";
import { Wallet, TrendingUp, TrendingDown, PiggyBank } from "lucide-react";
import {
  api,
  Transaction,
  SavingGoal,
  MonthlySummary,
  AccountBalance,
  Projection,
} from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { useAuth } from "@/app/context/AuthContext";

export default function Index() {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [savingGoals, setSavingGoals] = useState<SavingGoal[]>([]);
  const [summary, setSummary] = useState<MonthlySummary | null>(null);
  const [balances, setBalances] = useState<AccountBalance[]>([]);
  const [projections, setProjections] = useState<Projection[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const now = new Date();
        const [tx, goals, sum, bal, proj] = await Promise.all([
          api.listTransactions(),
          api.listSavingGoals(),
          api.monthlySummary(now.getFullYear(), now.getMonth() + 1),
          api.balance(),
          api.projections(3),
        ]);
        setTransactions(tx);
        setSavingGoals(goals.filter((g) => g.active));
        setSummary(sum);
        setBalances(bal);
        setProjections(proj);
      } catch {
        // los errores de auth ya redirigen; los demás se ignoran en el dashboard
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const totalBalance = balances.reduce((s, b) => s + Number(b.balance), 0);
  const totalSaved = savingGoals.reduce((s, g) => s + Number(g.currentAmount), 0);
  const recent = transactions.slice(0, 5);

  const monthLabel = new Date().toLocaleDateString("es-AR", {
    month: "long",
    year: "numeric",
  });

  return (
    <MainLayout>
      <div className="mb-8 animate-fade-in">
        <h1 className="font-display text-3xl font-bold text-foreground mb-2">
          Hola, {user?.username ?? "usuario"} 👋
        </h1>
        <p className="text-muted-foreground">
          Este es el resumen de tus finanzas de {monthLabel}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatCard
          title="Balance Total"
          value={formatMoney(totalBalance)}
          subtitle="Todas las cuentas"
          icon={Wallet}
        />
        <StatCard
          title="Ingresos del Mes"
          value={formatMoney(summary?.totalIncome)}
          subtitle="Este mes"
          icon={TrendingUp}
          variant="income"
        />
        <StatCard
          title="Gastos del Mes"
          value={formatMoney(summary?.totalExpense)}
          subtitle="Este mes"
          icon={TrendingDown}
          variant="expense"
        />
        <StatCard
          title="Ahorros Totales"
          value={formatMoney(totalSaved)}
          subtitle={`${savingGoals.length} metas activas`}
          icon={PiggyBank}
        />
      </div>

      {!loading && projections.length > 0 && (
        <div className="mb-8">
          <ProjectionPanel projections={projections} />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card-gradient rounded-xl p-5 animate-slide-up">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-display font-semibold text-lg">
              Transacciones Recientes
            </h3>
          </div>

          {loading ? (
            <p className="text-muted-foreground text-sm">Cargando...</p>
          ) : recent.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              Todavía no hay transacciones.
            </p>
          ) : (
            <div className="space-y-3">
              {recent.map((t) => (
                <TransactionItem
                  key={t.id}
                  type={t.type === "INCOME" ? "income" : "expense"}
                  description={t.description}
                  category={t.categoryName ?? "Sin categoría"}
                  amount={Number(t.amount)}
                  date={t.date}
                  account={t.accountName ?? "—"}
                />
              ))}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <QuickActions />
          <UpcomingPayments />
        </div>
      </div>

      <div className="mt-8">
        <h3 className="font-display font-semibold text-xl mb-4">
          Metas de Ahorro
        </h3>
        {savingGoals.length === 0 ? (
          <p className="text-muted-foreground text-sm">
            No tenés metas de ahorro activas.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {savingGoals.map((goal) => (
              <SavingGoalCard
                key={goal.id}
                name={goal.name}
                currentAmount={Number(goal.currentAmount)}
                targetAmount={Number(goal.targetAmount)}
                targetDate={goal.targetDate}
              />
            ))}
          </div>
        )}
      </div>
    </MainLayout>
  );
}
