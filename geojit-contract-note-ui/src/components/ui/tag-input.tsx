"use client";

import { useState, KeyboardEvent } from "react";
import { cn } from "@/lib/utils";

interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  className?: string;
}

export function TagInput({ value, onChange, placeholder, className }: TagInputProps) {
  const [input, setInput] = useState("");

  function addTag(raw: string) {
    const tags = raw
      .split(/[,\n]+/)
      .map((t) => t.trim())
      .filter(Boolean);
    if (tags.length === 0) return;
    const next = [...value, ...tags.filter((t) => !value.includes(t))];
    onChange(next);
    setInput("");
  }

  function handleKey(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      addTag(input);
    } else if (e.key === "Backspace" && input === "" && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  function remove(tag: string) {
    onChange(value.filter((t) => t !== tag));
  }

  return (
    <div
      className={cn(
        "flex flex-wrap gap-1.5 items-center border border-gray-200 rounded-lg px-2 py-1.5 bg-white min-h-[38px] focus-within:ring-2 focus-within:ring-[#00174b]/20",
        className,
      )}
    >
      {value.map((tag) => (
        <span
          key={tag}
          className="flex items-center gap-1 bg-blue-50 text-blue-700 text-xs font-medium px-2 py-0.5 rounded-full"
        >
          {tag}
          <button
            type="button"
            onClick={() => remove(tag)}
            className="hover:text-blue-900 leading-none"
          >
            ×
          </button>
        </span>
      ))}
      <input
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={handleKey}
        onBlur={() => input && addTag(input)}
        placeholder={value.length === 0 ? placeholder : ""}
        className="flex-1 min-w-[120px] text-sm outline-none bg-transparent text-gray-700 placeholder-gray-400"
      />
    </div>
  );
}
