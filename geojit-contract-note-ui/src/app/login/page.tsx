"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import type { AuthUser } from "@/types";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

const schema = z.object({
  email:    z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});
type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router   = useRouter();
  const setUser  = useAuthStore(s => s.setUser);
  const [showPw, setShowPw] = useState(false);
  const [clock,  setClock]  = useState("");

  useEffect(() => {
    const tick = () => setClock(new Date().toLocaleTimeString("en-GB", { timeZone: "Asia/Kolkata", hour12: false }));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const { mutate, isPending, error } = useMutation({
    mutationFn: ({ email, password }: FormData) => authApi.login(email, password),
    onSuccess: (res) => {
      const user: AuthUser = res.data.data;
      setUser(user);
      document.cookie = `auth-token=${user.accessToken}; path=/; max-age=${user.expiresIn}; SameSite=Strict`;
      toast.success(`Welcome back, ${user.name}`);
      router.replace("/dashboard");
    },
  });

  return (
    <div className="min-h-screen flex login-grad">
      {/* ── Left panel ────────────────────────────────── */}
      <div className="hidden lg:flex w-[42%] bg-[#00174b] text-white p-12 flex-col justify-between relative overflow-hidden">
        <div className="absolute -right-20 -top-20 w-96 h-96 rounded-full bg-[#497cff]/20 blur-3xl pointer-events-none" />
        <div className="absolute -left-10 bottom-10 w-72 h-72 rounded-full bg-[#003ea8]/30 blur-3xl pointer-events-none" />

        <div className="relative z-10">
          {/* Brand */}
          <div className="flex items-center gap-3 mb-16">
            <div className="w-11 h-11 rounded-xl bg-white/10 backdrop-blur flex items-center justify-center border border-white/20">
              <span className="material-symbols-outlined text-blue-300 text-2xl">account_balance</span>
            </div>
            <div>
              <div className="font-extrabold text-sm tracking-wide">GEOJIT FINANCIAL SERVICES</div>
              <div className="text-[10px] text-blue-300/80 tracking-widest uppercase">Contract Note Operations</div>
            </div>
          </div>

          {/* Headline */}
          <div className="max-w-md">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 border border-white/20 text-[10px] font-bold tracking-widest uppercase mb-6">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Live · Mumbai
            </div>
            <h2 className="font-bold text-4xl tracking-tight mb-5 leading-tight" style={{ fontFamily: "Manrope, sans-serif" }}>
              Smart Contract Note <span className="text-blue-300">Console</span>
            </h2>
            <p className="text-blue-100/70 text-sm leading-relaxed mb-8">
              Upload raw trade files, generate digitally-signed PDFs, dispatch to clients, and resolve bounces — the complete contract note pipeline for Geojit operations.
            </p>
            <ul className="space-y-5">
              <li className="flex items-start gap-4">
                <span className="material-symbols-outlined text-blue-300">fact_check</span>
                <div>
                  <p className="font-bold text-sm">Pre-flight file validation</p>
                  <p className="text-white/55 text-xs mt-1 leading-relaxed">Validate every row before submission. Catch bad client codes, missing PANs, or malformed fields before a single PDF is generated.</p>
                </div>
              </li>
              <li className="flex items-start gap-4">
                <span className="material-symbols-outlined text-blue-300">timeline</span>
                <div>
                  <p className="font-bold text-sm">Live pipeline visibility</p>
                  <p className="text-white/55 text-xs mt-1 leading-relaxed">Track every run from file upload through PDF generation to email delivery — with live progress per customer.</p>
                </div>
              </li>
              <li className="flex items-start gap-4">
                <span className="material-symbols-outlined text-blue-300">mark_email_unread</span>
                <div>
                  <p className="font-bold text-sm">Bounce &amp; delivery reconciliation</p>
                  <p className="text-white/55 text-xs mt-1 leading-relaxed">Bounces and delivery confirmations matched to client codes in real time. One-click resend for failed or bounced records.</p>
                </div>
              </li>
            </ul>
          </div>
        </div>

        {/* Footer stats */}
        <div className="relative z-10 border-t border-white/10 pt-6 flex items-center gap-8">
          <div>
            <p className="text-[9px] font-bold uppercase tracking-widest text-white/40 mb-1">Status</p>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span className="text-xs font-semibold">All systems operational</span>
            </div>
          </div>
          <div>
            <p className="text-[9px] font-bold uppercase tracking-widest text-white/40 mb-1">Region</p>
            <p className="text-xs font-semibold">Mumbai · ap-south-1</p>
          </div>
          <div>
            <p className="text-[9px] font-bold uppercase tracking-widest text-white/40 mb-1">Compliance</p>
            <p className="text-xs font-semibold">SEBI · 7-yr audit trail</p>
          </div>
        </div>
      </div>

      {/* ── Right panel — login form ───────────────────── */}
      <div className="flex-1 flex items-center justify-center p-8 bg-[#f7f9fb]">
        <div className="w-full max-w-[440px] bg-white p-10 rounded-3xl shadow-2xl border border-slate-100">
          {/* Mobile brand */}
          <div className="lg:hidden flex items-center gap-2 mb-6">
            <div className="w-10 h-10 rounded-xl bg-[#00174b] flex items-center justify-center">
              <span className="material-symbols-outlined text-blue-300">account_balance</span>
            </div>
            <div className="font-extrabold text-sm">GEOJIT FINANCIAL SERVICES</div>
          </div>

          <div className="mb-8">
            <h1 className="text-[1.7rem] font-extrabold tracking-tight mb-1.5" style={{ fontFamily: "Manrope, sans-serif" }}>Sign in</h1>
            <p className="text-slate-500 text-sm">Authorised ops personnel only</p>
          </div>

          <form onSubmit={handleSubmit(d => mutate(d))} className="space-y-5">
            {/* Email */}
            <div className="space-y-2">
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 px-1">Corporate email</label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">mail</span>
                <input
                  type="email"
                  autoComplete="email"
                  placeholder="you@geojit.co.in"
                  {...register("email")}
                  className="w-full pl-11 pr-3 py-3.5 bg-slate-50 border border-slate-100 rounded-xl text-sm transition-all focus:outline-none focus:shadow-[0_0_0_3px_rgba(73,124,255,0.18)] focus:border-[#497cff]"
                />
              </div>
              {errors.email && <p className="text-xs text-red-500 px-1">{errors.email.message}</p>}
            </div>

            {/* Password */}
            <div className="space-y-2">
              <div className="flex justify-between items-center px-1">
                <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Password</label>
              </div>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">lock</span>
                <input
                  type={showPw ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  {...register("password")}
                  className="w-full pl-11 pr-11 py-3.5 bg-slate-50 border border-slate-100 rounded-xl text-sm transition-all focus:outline-none focus:shadow-[0_0_0_3px_rgba(73,124,255,0.18)] focus:border-[#497cff]"
                />
                <button
                  type="button"
                  onClick={() => setShowPw(v => !v)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-[#00174b]"
                >
                  <span className="material-symbols-outlined text-[20px]">
                    {showPw ? "visibility_off" : "visibility"}
                  </span>
                </button>
              </div>
              {errors.password && <p className="text-xs text-red-500 px-1">{errors.password.message}</p>}
            </div>

            {/* Compliance note */}
            <div className="flex items-start gap-3 p-3.5 bg-blue-50/60 border border-blue-100 rounded-xl">
              <span className="material-symbols-outlined text-[#003ea8] text-[18px] mt-0.5">verified_user</span>
              <p className="text-[11px] leading-relaxed text-slate-600">
                All operator actions — uploads, resends, template edits, certificate changes — are recorded in the audit log per SEBI record-keeping requirements.
              </p>
            </div>

            {error && (
              <Alert variant="destructive" className="py-2">
                <AlertDescription className="text-sm">Invalid email or password. Please try again.</AlertDescription>
              </Alert>
            )}

            <button
              type="submit"
              disabled={isPending}
              className="w-full bg-[#00174b] text-white py-3.5 rounded-xl font-bold text-sm tracking-wide shadow-lg hover:bg-[#003ea8] transition-all flex items-center justify-center gap-2 disabled:opacity-60"
            >
              {isPending
                ? <><Loader2 size={16} className="animate-spin" /> Signing in…</>
                : <>Sign in <span className="material-symbols-outlined text-base">arrow_forward</span></>
              }
            </button>
          </form>

          {/* Footer */}
          <div className="mt-8 pt-6 border-t border-slate-100 flex justify-between items-center">
            <div className="text-[10px] text-slate-400">
              © {new Date().getFullYear()} Geojit Financial Services Ltd.
            </div>
            <div className="text-[9px] text-slate-400 font-mono">IST {clock}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
