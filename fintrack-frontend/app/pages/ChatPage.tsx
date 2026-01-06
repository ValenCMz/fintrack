"use client";
import { useState } from "react";
import MainLayout from "@/app/components/layout/MainLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Send, Bot, User, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";

interface Message {
  id: number;
  role: "user" | "assistant";
  content: string;
}

const suggestedQuestions = [
  "¿Cuánto gasté este mes en comida?",
  "¿Cómo puedo reducir mis deudas?",
  "Muéstrame un resumen de mis finanzas",
  "¿Cuánto debería ahorrar mensualmente?",
];

const ChatPage = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 1,
      role: "assistant",
      content:
        "¡Hola! Soy tu asistente financiero. Puedo ayudarte a analizar tus gastos, planificar tus ahorros y responder cualquier pregunta sobre tus finanzas personales. ¿En qué puedo ayudarte hoy?",
    },
  ]);
  const [input, setInput] = useState("");

  const handleSend = () => {
    if (!input.trim()) return;

    const userMessage: Message = {
      id: messages.length + 1,
      role: "user",
      content: input,
    };

    setMessages([...messages, userMessage]);
    setInput("");

    // Simulated response
    setTimeout(() => {
      const assistantMessage: Message = {
        id: messages.length + 2,
        role: "assistant",
        content:
          "Esta es una respuesta de demostración. Cuando conectes el backend con el MCP, podré acceder a tus datos financieros reales y darte respuestas personalizadas basadas en tu situación.",
      };
      setMessages((prev) => [...prev, assistantMessage]);
    }, 1000);
  };

  const handleSuggestionClick = (question: string) => {
    setInput(question);
  };

  return (
    <MainLayout>
      <div className="max-w-4xl mx-auto h-[calc(100vh-8rem)] flex flex-col">
        {/* Header */}
        <div className="flex items-center gap-4 mb-6 animate-fade-in">
          <div className="p-3 rounded-xl bg-primary/20">
            <Bot className="w-8 h-8 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-2xl font-bold">
              Asistente Financiero
            </h1>
            <p className="text-muted-foreground text-sm">
              Pregúntame sobre tus finanzas personales
            </p>
          </div>
        </div>

        {/* Chat Container */}
        <div className="flex-1 card-gradient rounded-xl p-5 flex flex-col overflow-hidden">
          {/* Messages */}
          <div className="flex-1 overflow-y-auto space-y-4 mb-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={cn(
                  "flex gap-3 animate-slide-up",
                  message.role === "user" && "flex-row-reverse"
                )}
              >
                <div
                  className={cn(
                    "p-2 rounded-xl shrink-0",
                    message.role === "assistant"
                      ? "bg-primary/20"
                      : "bg-secondary"
                  )}
                >
                  {message.role === "assistant" ? (
                    <Bot className="w-5 h-5 text-primary" />
                  ) : (
                    <User className="w-5 h-5 text-foreground" />
                  )}
                </div>
                <div
                  className={cn(
                    "max-w-[80%] p-4 rounded-xl",
                    message.role === "assistant"
                      ? "bg-secondary/70 text-foreground"
                      : "bg-primary text-primary-foreground"
                  )}
                >
                  <p className="text-sm leading-relaxed">{message.content}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Suggested Questions */}
          {messages.length === 1 && (
            <div className="mb-4">
              <p className="text-xs text-muted-foreground mb-2 flex items-center gap-1">
                <Sparkles className="w-3 h-3" />
                Sugerencias
              </p>
              <div className="flex flex-wrap gap-2">
                {suggestedQuestions.map((question, index) => (
                  <button
                    key={index}
                    onClick={() => handleSuggestionClick(question)}
                    className="px-3 py-1.5 text-xs bg-secondary/70 hover:bg-secondary rounded-full text-muted-foreground hover:text-foreground transition-colors"
                  >
                    {question}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input */}
          <div className="flex gap-3">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSend()}
              placeholder="Escribe tu pregunta..."
              className="bg-secondary/50 border-border/50 focus-visible:ring-primary"
            />
            <Button
              onClick={handleSend}
              disabled={!input.trim()}
              className="shrink-0"
            >
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default ChatPage;
