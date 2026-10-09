"use client";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

interface ConfirmDeleteButtonProps {
  onConfirm: () => void | Promise<void>;
  title?: string;
  description?: string;
  className?: string;
  children: React.ReactNode;
}

const ConfirmDeleteButton = ({
  onConfirm,
  title = "¿Eliminar registro?",
  description = "Esta acción no se puede deshacer.",
  className,
  children,
}: ConfirmDeleteButtonProps) => (
  <AlertDialog>
    <AlertDialogTrigger asChild>
      <button type="button" className={className}>
        {children}
      </button>
    </AlertDialogTrigger>
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>{title}</AlertDialogTitle>
        <AlertDialogDescription>{description}</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>Cancelar</AlertDialogCancel>
        <AlertDialogAction onClick={() => onConfirm()}>
          Eliminar
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
);

export default ConfirmDeleteButton;
