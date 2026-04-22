"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { usersApi } from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import type { AppUser, UserOrganisation } from "@/types";
import { Loader2 } from "lucide-react";
import { cn, fmtDate } from "@/lib/utils";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";

const avatarColors = ["bg-[#00174b]", "bg-emerald-600", "bg-purple-600", "bg-rose-600", "bg-amber-600", "bg-sky-600"];
const rolePill: Record<string, string> = { ADMIN: "pill-info", OPS_MANAGER: "pill-ok", VIEWER: "pill-neu" };
const orgPill: Record<string, string> = { ACC: "pill-info", GEOJIT: "pill-ok" };
const permissions: Record<string, string> = {
  ADMIN: "Upload · Override · Bulk resend · Template · PFX · Users",
  OPS_MANAGER: "Upload · Override · Bulk resend · Template · PFX",
  VIEWER: "Read-only · Reports · Audit",
};

type FormState = { name: string; email: string; role: string; organisation: UserOrganisation; password: string };
const defaultForm = (): FormState => ({ name: "", email: "", role: "OPS_MANAGER", organisation: "GEOJIT", password: "" });

export default function UsersPage() {
  const qc = useQueryClient();
  const authUser = useAuthStore(s => s.user);
  const isAccAdmin = authUser?.organisation === "ACC" || !authUser?.organisation; // treat unknown as ACC (backwards compat)

  const [showInvite, setShowInvite] = useState(false);
  const [editUser, setEditUser] = useState<AppUser | null>(null);
  const [deactivateUser, setDeactivateUser] = useState<AppUser | null>(null);
  const [form, setForm] = useState<FormState>(defaultForm());

  const { data, isLoading } = useQuery({ queryKey: ["users"], queryFn: () => usersApi.list() });
  const _raw = data?.data?.data;
  const users: AppUser[] = Array.isArray(_raw) ? _raw : (_raw?.content ?? []);

  const { mutate: create, isPending: creating } = useMutation({
    mutationFn: () => usersApi.create(form),
    onSuccess: () => { toast.success("User created successfully"); setShowInvite(false); setForm(defaultForm()); qc.invalidateQueries({ queryKey: ["users"] }); },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? "Failed to create user";
      toast.error(msg);
    },
  });

  const { mutate: update, isPending: updating } = useMutation({
    mutationFn: () => usersApi.update(editUser!.userId, { name: form.name, role: form.role, organisation: form.organisation }),
    onSuccess: () => { toast.success("User updated"); setEditUser(null); qc.invalidateQueries({ queryKey: ["users"] }); },
  });

  const { mutate: deactivate } = useMutation({
    mutationFn: (id: string) => usersApi.deactivate(id),
    onSuccess: () => { toast.success("User deactivated — access revoked immediately"); setDeactivateUser(null); qc.invalidateQueries({ queryKey: ["users"] }); },
  });

  const openEdit = (u: AppUser) => {
    setForm({ name: u.name, email: u.email, role: u.role, organisation: u.organisation ?? "GEOJIT", password: "" });
    setEditUser(u);
  };

  return (
    <div className="p-6 space-y-5 max-w-[1600px] mx-auto w-full fade-up">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-slate-900 headline">Users &amp; Roles</h2>
          <p className="text-slate-500 text-sm mt-1">Manage operator access to the contract note pipeline. Admins control uploads, config, and certificate management.</p>
        </div>
        <button onClick={() => { setForm(defaultForm()); setShowInvite(true); }} className="px-4 py-2 bg-[#00174b] text-white rounded-xl text-sm font-bold hover:bg-[#003ea8] flex items-center gap-1.5">
          <span className="material-symbols-outlined text-base">person_add</span>Add user
        </button>
      </div>

      <div className="card overflow-hidden">
        <table className="w-full text-left">
          <thead>
            <tr className="t-hd">
              <th className="px-5 py-3.5">User</th>
              <th className="px-5 py-3.5">Organisation</th>
              <th className="px-5 py-3.5">Role</th>
              <th className="px-5 py-3.5">Permissions</th>
              <th className="px-5 py-3.5">Last active</th>
              <th className="px-5 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={6} className="text-center py-12"><Loader2 className="animate-spin text-[#00174b] inline" /></td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-12 text-slate-400 text-sm">No users found</td></tr>
            ) : users.map(u => {
              const initials = u.name?.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) ?? "?";
              const colorIdx = (u.name?.charCodeAt(0) ?? 0) % avatarColors.length;
              return (
                <tr key={u.userId} className={cn("t-row", !u.isActive && "opacity-50")}>
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <div className={cn("w-9 h-9 rounded-full text-white text-[11px] font-bold flex items-center justify-center flex-shrink-0", avatarColors[colorIdx])}>{initials}</div>
                      <div>
                        <div className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                          {u.name}
                          {!u.isActive && <span className="pill pill-err text-[9px]">Deactivated</span>}
                        </div>
                        <div className="text-[11px] text-slate-500 mono">{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <span className={cn("pill", orgPill[u.organisation ?? "GEOJIT"] ?? "pill-neu")}>
                      {u.organisation ?? "GEOJIT"}
                    </span>
                  </td>
                  <td className="px-5 py-4"><span className={cn("pill", rolePill[u.role] ?? "pill-neu")}>{u.role === "OPS_MANAGER" ? "Ops Manager" : u.role}</span></td>
                  <td className="px-5 py-4 text-[11px] text-slate-500">{permissions[u.role] ?? "—"}</td>
                  <td className="px-5 py-4 text-[11px] text-slate-500">{u.lastLogin ? fmtDate(u.lastLogin) : "Never"}</td>
                  <td className="px-5 py-4 text-right flex justify-end gap-3">
                    <button onClick={() => openEdit(u)} className="text-[11px] font-bold text-[#003ea8] hover:underline">Edit</button>
                    {u.isActive && (
                      <button onClick={() => setDeactivateUser(u)} className="text-[11px] font-bold text-rose-600 hover:underline">Deactivate</button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Create Dialog */}
      <Dialog open={showInvite} onOpenChange={setShowInvite}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add operator</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Full name</label>
              <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm" placeholder="e.g. Priya Nair" />
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Corporate email</label>
              <input value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm" placeholder="user@geojit.co.in" />
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Organisation</label>
              <select
                value={form.organisation}
                onChange={e => setForm(f => ({ ...f, organisation: e.target.value as UserOrganisation }))}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm"
                disabled={!isAccAdmin}
              >
                <option value="GEOJIT">Geojit</option>
                {isAccAdmin && <option value="ACC">ACC</option>}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Role</label>
              <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm">
                <option value="ADMIN">Admin</option>
                <option value="OPS_MANAGER">Ops Manager</option>
                <option value="VIEWER">Viewer</option>
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Temporary password</label>
              <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm" placeholder="Set a temporary password" />
              <p className="text-[11px] text-slate-400 mt-1">The user should change this on first login.</p>
            </div>
          </div>
          <DialogFooter>
            <button onClick={() => setShowInvite(false)} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-semibold">Cancel</button>
            <button onClick={() => create()} disabled={creating || !form.name || !form.email} className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50">
              {creating ? "Creating..." : "Create user"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={!!editUser} onOpenChange={() => setEditUser(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit user</DialogTitle></DialogHeader>
          {editUser && (
            <div className="space-y-3">
              <div>
                <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Name</label>
                <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm" />
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Email</label>
                <input value={form.email} disabled className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 text-slate-400 cursor-not-allowed" />
              </div>
              {isAccAdmin && (
                <div>
                  <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Organisation</label>
                  <select
                    value={form.organisation}
                    onChange={e => setForm(f => ({ ...f, organisation: e.target.value as UserOrganisation }))}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm"
                  >
                    <option value="GEOJIT">Geojit</option>
                    <option value="ACC">ACC</option>
                  </select>
                </div>
              )}
              <div>
                <label className="text-[10px] font-bold uppercase tracking-widest text-slate-500 block mb-1">Role</label>
                <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))} className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm">
                  <option value="ADMIN">Admin</option>
                  <option value="OPS_MANAGER">Ops Manager</option>
                  <option value="VIEWER">Viewer</option>
                </select>
              </div>
            </div>
          )}
          <DialogFooter>
            <button onClick={() => setEditUser(null)} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-semibold">Cancel</button>
            <button onClick={() => update()} disabled={updating} className="px-4 py-2 bg-[#00174b] text-white rounded-lg text-sm font-bold hover:bg-[#003ea8] disabled:opacity-50">
              {updating ? "Saving..." : "Save changes"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Deactivate Dialog */}
      <AlertDialog open={!!deactivateUser} onOpenChange={() => setDeactivateUser(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Deactivate {deactivateUser?.name}?</AlertDialogTitle>
            <AlertDialogDescription>This revokes their access immediately. They will be unable to log in. This action can be reversed by re-creating the user.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={() => deactivateUser && deactivate(deactivateUser.userId)} className="bg-rose-600 hover:bg-rose-700">
              Deactivate
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
