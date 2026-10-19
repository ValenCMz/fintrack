import * as React from "react"

import { cn } from "@/lib/utils"

/**
 * Campo de formulario: un <label> que envuelve al control. El wrapping asocia
 * el texto al input sin necesidad de ids (HTML implícito) y hace que el clic
 * en el texto enfoque el control.
 *
 * No usar si el campo contiene más de un control o un botón.
 */
function Field({
  className,
  label,
  hint,
  children,
  ...props
}: React.ComponentProps<"label"> & {
  label: string
  hint?: string
}) {
  return (
    <label className={cn("grid gap-1.5 text-sm", className)} {...props}>
      <span className="font-medium text-foreground">{label}</span>
      {children}
      {hint ? (
        <span className="text-xs text-muted-foreground">{hint}</span>
      ) : null}
    </label>
  )
}

export { Field }
