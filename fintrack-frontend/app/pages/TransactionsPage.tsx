import MainLayout from "@/app/components/layout/MainLayout";
import TransactionItem from "@/app/components/dashboard/TransactionItem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Plus,
  Search,
  Filter,
  ArrowUpCircle,
  ArrowDownCircle,
} from "lucide-react";

interface TransactionsPageProps {
  type: "income" | "expense";
}

const mockIncomes = [
  {
    type: "income" as const,
    description: "Pago freelance - Proyecto web",
    category: "Trabajo",
    amount: 150000,
    date: "4 Ene 2026",
    account: "MercadoPago",
  },
  {
    type: "income" as const,
    description: "Venta online",
    category: "Ventas",
    amount: 45000,
    date: "3 Ene 2026",
    account: "Banco Galicia",
  },
  {
    type: "income" as const,
    description: "Devolución impuestos",
    category: "Otros",
    amount: 28000,
    date: "2 Ene 2026",
    account: "Banco Galicia",
  },
  {
    type: "income" as const,
    description: "Consultoría",
    category: "Trabajo",
    amount: 80000,
    date: "30 Dic 2025",
    account: "MercadoPago",
  },
  {
    type: "income" as const,
    description: "Intereses plazo fijo",
    category: "Inversiones",
    amount: 12500,
    date: "28 Dic 2025",
    account: "Banco",
  },
];

const mockExpenses = [
  {
    type: "expense" as const,
    description: "Supermercado Carrefour",
    category: "Alimentación",
    amount: 25000,
    date: "3 Ene 2026",
    account: "Efectivo",
  },
  {
    type: "expense" as const,
    description: "Uber",
    category: "Transporte",
    amount: 3500,
    date: "3 Ene 2026",
    account: "MercadoPago",
  },
  {
    type: "expense" as const,
    description: "Farmacia",
    category: "Salud",
    amount: 8500,
    date: "2 Ene 2026",
    account: "Efectivo",
  },
  {
    type: "expense" as const,
    description: "Netflix",
    category: "Entretenimiento",
    amount: 5999,
    date: "1 Ene 2026",
    account: "Tarjeta Visa",
  },
  {
    type: "expense" as const,
    description: "Almuerzo trabajo",
    category: "Alimentación",
    amount: 4500,
    date: "31 Dic 2025",
    account: "MercadoPago",
  },
  {
    type: "expense" as const,
    description: "Gasolina",
    category: "Transporte",
    amount: 15000,
    date: "30 Dic 2025",
    account: "Tarjeta Visa",
  },
];

const TransactionsPage = ({ type }: TransactionsPageProps) => {
  const isIncome = type === "income";
  const transactions = isIncome ? mockIncomes : mockExpenses;
  const totalAmount = transactions.reduce((sum, t) => sum + t.amount, 0);

  return (
    <MainLayout>
      {/* Header */}
      <div className="flex items-center justify-between mb-8 animate-fade-in">
        <div className="flex items-center gap-4">
          <div
            className={`p-3 rounded-xl ${
              isIncome ? "bg-income/20" : "bg-expense/20"
            }`}
          >
            {isIncome ? (
              <ArrowUpCircle className="w-8 h-8 text-income" />
            ) : (
              <ArrowDownCircle className="w-8 h-8 text-expense" />
            )}
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">
              {isIncome ? "Ingresos" : "Egresos"}
            </h1>
            <p className="text-muted-foreground">
              Gestiona tus {isIncome ? "ingresos" : "gastos"}
            </p>
          </div>
        </div>
        <Button
          className={
            isIncome
              ? "bg-income hover:bg-income/90"
              : "bg-expense hover:bg-expense/90"
          }
        >
          <Plus className="w-4 h-4 mr-2" />
          Nuevo {isIncome ? "Ingreso" : "Gasto"}
        </Button>
      </div>

      {/* Summary Card */}
      <div className="card-gradient rounded-xl p-6 mb-6 animate-slide-up">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-muted-foreground text-sm">Total del mes</p>
            <p
              className={`font-display text-4xl font-bold ${
                isIncome ? "text-income" : "text-expense"
              }`}
            >
              ${totalAmount.toLocaleString()}
            </p>
          </div>
          <div className="text-right">
            <p className="text-muted-foreground text-sm">
              {transactions.length} transacciones
            </p>
            <p className="text-sm text-muted-foreground">Enero 2026</p>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-4 mb-6 animate-slide-up">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Buscar transacciones..."
            className="pl-10 bg-secondary/50 border-border/50"
          />
        </div>
        <Button variant="outline" className="gap-2">
          <Filter className="w-4 h-4" />
          Filtros
        </Button>
      </div>

      {/* Transactions List */}
      <div className="card-gradient rounded-xl p-5 animate-slide-up">
        <div className="space-y-3">
          {transactions.map((transaction, index) => (
            <TransactionItem key={index} {...transaction} />
          ))}
        </div>
      </div>
    </MainLayout>
  );
};

export default TransactionsPage;
