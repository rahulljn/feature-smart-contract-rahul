"use client";

import { useQuery } from "@tanstack/react-query";
import { configApi } from "@/lib/api";
import type { CmsPage } from "@/types";
import { Loader2 } from "lucide-react";
import Link from "next/link";

const USE_MOCK = true;
const MOCK_PAGE: CmsPage = {
  id: "1", slug: "privacy-policy", title: "Privacy Policy",
  content: `Privacy Policy

This Privacy Policy describes how Geojit Financial Services Ltd ("Geojit", "we", "us", or "our") collects, uses, and shares information about you when you use our contract note delivery system.

Information We Collect

We collect information you provide directly to us, including your name, email address, and client code when you register or interact with our services.

How We Use Your Information

We use the information we collect to deliver contract notes and trade confirmations to your registered email address, maintain accurate records of your transactions, and comply with regulatory requirements.

Data Security

We implement appropriate technical and organisational measures to protect your personal information against unauthorised access, loss, or alteration. All contract note PDFs are encrypted and digitally signed.

Contact Us

For privacy-related queries, contact us at customercare@geojit.com or call Toll Free: 1800-571-5501.`,
  lastUpdatedAt: new Date().toISOString(), lastUpdatedBy: "Admin",
};

export default function PrivacyPolicyPage() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const { data: pages, isLoading } = useQuery<any>({
    queryKey: ["cms-privacy-policy"],
    queryFn: async () => USE_MOCK
      ? Promise.resolve({ data: { data: [MOCK_PAGE] } })
      : configApi.getCmsPages(),
    select: (res) => (res?.data?.data as CmsPage[])?.find(p => p.slug === "privacy-policy"),
  });

  const page = pages as CmsPage | undefined;

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-3xl mx-auto px-6 py-12">
        <Link href="/" className="inline-flex items-center gap-1 text-[#003ea8] text-sm font-semibold hover:underline mb-8">
          <span className="material-symbols-outlined text-base">arrow_back</span>
          Back
        </Link>

        {isLoading ? (
          <div className="flex justify-center py-16"><Loader2 className="animate-spin text-[#00174b]" /></div>
        ) : page ? (
          <>
            <h1 className="text-3xl font-extrabold text-slate-900 mb-2">{page.title}</h1>
            <p className="text-sm text-slate-400 mb-8">
              Last updated by {page.lastUpdatedBy} · {new Date(page.lastUpdatedAt).toLocaleDateString("en-IN", { day: "2-digit", month: "long", year: "numeric" })}
            </p>
            <div className="prose max-w-none text-slate-700 space-y-4">
              {page.content.split("\n").map((line, i) => (
                line.trim() === "" ? <div key={i} className="h-2" /> :
                line.startsWith("##") ? <h2 key={i} className="text-xl font-bold text-slate-900 mt-6">{line.replace(/^#+\s*/, "")}</h2> :
                line.startsWith("#") ? <h1 key={i} className="text-2xl font-extrabold text-slate-900 mt-8">{line.replace(/^#+\s*/, "")}</h1> :
                <p key={i} className="text-[15px] leading-relaxed">{line}</p>
              ))}
            </div>
          </>
        ) : (
          <div className="text-center py-16 text-slate-400">Page not found</div>
        )}

        <div className="mt-12 pt-6 border-t border-slate-100 text-center text-[12px] text-slate-400">
          © {new Date().getFullYear()} Geojit Financial Services Ltd. All rights reserved.
        </div>
      </div>
    </div>
  );
}
