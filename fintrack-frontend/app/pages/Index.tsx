import MainLayout from "@/app/components/layout/MainLayout";
import StatCard from "@/app/components/dashboard/StatCard";
import TransactionItem from "@/app/components/dashboard/TransactionItem";
import SavingGoalCard from "@/app/components/dashboard/SavingGoalCard";
import QuickActions from "@/app/components/dashboard/QuickActions";
import UpcomingPayments from "@/app/components/dashboard/UpcomingPayments";
import { Wallet, TrendingUp, TrendingDown, PiggyBank } from "lucide-react";

const mockTransactions = [
  {
    type: "income" as const,
    description: "Pago freelance",
    category: "Trabajo",
    amount: 150000,
    date: "Hoy",
    account: "MercadoPago",
  },
  {
    type: "expense" as const,
    description: "Supermercado",
    category: "Alimentación",
    amount: 25000,
    date: "Ayer",
    account: "Efectivo",
  },
  {
    type: "expense" as const,
    description: "Uber",
    category: "Transporte",
    amount: 3500,
    date: "Ayer",
    account: "MercadoPago",
  },
  {
    type: "income" as const,
    description: "Venta online",
    category: "Ventas",
    amount: 45000,
    date: "3 Ene",
    account: "Banco",
  },
  {
    type: "expense" as const,
    description: "Farmacia",
    category: "Salud",
    amount: 8500,
    date: "2 Ene",
    account: "Efectivo",
  },
];

const mockSavingGoals = [
  {
    name: "Fondo de emergencia",
    currentAmount: 250000,
    targetAmount: 500000,
    targetDate: "Jun 2026",
  },
  {
    name: "Vacaciones",
    currentAmount: 80000,
    targetAmount: 300000,
    targetDate: "Dic 2026",
  },
];

const Index = () => {
  return (
    <MainLayout>
      {/* Header */}
      <div className="mb-8 animate-fade-in">
        <h1 className="font-display text-3xl font-bold text-foreground mb-2">
          Bienvenido de vuelta 👋
        </h1>
        <p className="text-muted-foreground">
          Aquí está el resumen de tus finanzas de Enero 2026
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatCard
          title="Balance Total"
          value="$485,000"
          subtitle="Todas las cuentas"
          icon={Wallet}
          trend={{ value: "12%", positive: true }}
        />
        <StatCard
          title="Ingresos del Mes"
          value="$320,000"
          subtitle="4 transacciones"
          icon={TrendingUp}
          variant="income"
          trend={{ value: "8%", positive: true }}
        />
        <StatCard
          title="Gastos del Mes"
          value="$145,000"
          subtitle="12 transacciones"
          icon={TrendingDown}
          variant="expense"
          trend={{ value: "5%", positive: false }}
        />
        <StatCard
          title="Ahorros Totales"
          value="$330,000"
          subtitle="2 metas activas"
          icon={PiggyBank}
          trend={{ value: "15%", positive: true }}
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Transactions - Takes 2 columns */}
        <div className="lg:col-span-2 card-gradient rounded-xl p-5 animate-slide-up">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-display font-semibold text-lg">
              Transacciones Recientes
            </h3>
            <button className="text-sm text-primary hover:underline">
              Ver todas
            </button>
          </div>

          <div className="space-y-3">
            {mockTransactions.map((transaction, index) => (
              <TransactionItem key={index} {...transaction} />
            ))}
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          <QuickActions />
          <UpcomingPayments />
        </div>
      </div>

      {/* Saving Goals */}
      <div className="mt-8">
        <h3 className="font-display font-semibold text-xl mb-4">
          Metas de Ahorro
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {mockSavingGoals.map((goal, index) => (
            <SavingGoalCard key={index} {...goal} />
          ))}
        </div>
      </div>
    </MainLayout>
  );
};

export default Index;
