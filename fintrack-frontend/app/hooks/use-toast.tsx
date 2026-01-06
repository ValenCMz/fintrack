import { toast as sonnerToast } from "sonner";

type ToastProps = {
  title?: React.ReactNode;
  description?: React.ReactNode;
  variant?: "default" | "destructive" | "success" | "warning" | "info";
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
};

function toast({
  title,
  description,
  variant = "default",
  duration,
  action,
}: ToastProps) {
  const message = title
    ? description
      ? `${String(title)}: ${String(description)}`
      : String(title)
    : String(description || "");

  const toastOptions = {
    duration,
    action: action
      ? {
          label: action.label,
          onClick: action.onClick,
        }
      : undefined,
  };

  switch (variant) {
    case "success":
      return sonnerToast.success(message, toastOptions);
    case "destructive":
      return sonnerToast.error(message, toastOptions);
    case "warning":
      return sonnerToast.warning(message, toastOptions);
    case "info":
      return sonnerToast.info(message, toastOptions);
    default:
      return sonnerToast(message, toastOptions);
  }
}

// Extender la función toast con métodos adicionales de Sonner
toast.success = (
  message: string,
  options?: Parameters<typeof sonnerToast.success>[1]
) => sonnerToast.success(message, options);

toast.error = (
  message: string,
  options?: Parameters<typeof sonnerToast.error>[1]
) => sonnerToast.error(message, options);

toast.warning = (
  message: string,
  options?: Parameters<typeof sonnerToast.warning>[1]
) => sonnerToast.warning(message, options);

toast.info = (
  message: string,
  options?: Parameters<typeof sonnerToast.info>[1]
) => sonnerToast.info(message, options);

toast.loading = (
  message: string,
  options?: Parameters<typeof sonnerToast.loading>[1]
) => sonnerToast.loading(message, options);

toast.dismiss = (id?: string | number) => sonnerToast.dismiss(id);

toast.promise = <T,>(
  promise: Promise<T>,
  messages: {
    loading: string;
    success: string | ((data: T) => string);
    error: string | ((error: unknown) => string);
  },
  options?: Parameters<typeof sonnerToast.promise>[1]
) => sonnerToast.promise(promise, messages);

function useToast() {
  return {
    toast,
    dismiss: toast.dismiss,
  };
}

export { useToast, toast };
