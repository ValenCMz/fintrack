import MainLayout from "@/app/components/layout/MainLayout";
import { LucideIcon, Construction } from "lucide-react";

interface PlaceholderPageProps {
  title: string;
  description: string;
  icon: LucideIcon;
}

const PlaceholderPage = ({
  title,
  description,
  icon: Icon,
}: PlaceholderPageProps) => {
  return (
    <MainLayout>
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center animate-fade-in">
        <div className="p-4 rounded-2xl bg-primary/10 mb-6">
          <Icon className="w-12 h-12 text-primary" />
        </div>
        <h1 className="font-display text-3xl font-bold mb-3">{title}</h1>
        <p className="text-muted-foreground max-w-md mb-6">{description}</p>
        <div className="flex items-center gap-2 text-sm text-muted-foreground bg-secondary/50 px-4 py-2 rounded-full">
          <Construction className="w-4 h-4" />
          En desarrollo
        </div>
      </div>
    </MainLayout>
  );
};

export default PlaceholderPage;
