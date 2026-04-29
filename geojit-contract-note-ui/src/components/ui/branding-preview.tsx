"use client";

interface BrandingPreviewProps {
  primaryColor: string;
  accentColor: string;
  appName: string;
}

export function BrandingPreview({ primaryColor, accentColor, appName }: BrandingPreviewProps) {
  return (
    <div className="rounded-xl overflow-hidden shadow-md border border-gray-200" style={{ width: 360, height: 220 }}>
      <div className="flex h-full">
        {/* Mini sidebar */}
        <div
          className="w-20 flex flex-col p-2 gap-2"
          style={{ backgroundColor: primaryColor }}
        >
          <div className="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center">
            <span className="text-white text-xs font-bold">
              {appName.slice(0, 2).toUpperCase()}
            </span>
          </div>
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-6 rounded-md"
              style={{ backgroundColor: i === 1 ? "rgba(255,255,255,0.2)" : "rgba(255,255,255,0.08)" }}
            />
          ))}
        </div>

        {/* Mini content */}
        <div className="flex-1 bg-gray-50 p-3 flex flex-col gap-2">
          <div className="h-4 bg-gray-200 rounded w-24" />
          <div className="bg-white rounded-lg p-2.5 border border-gray-100 flex items-center gap-2">
            <div>
              <div
                className="text-lg font-extrabold leading-none"
                style={{ color: accentColor }}
              >
                28,200
              </div>
              <div className="text-[9px] text-gray-400 uppercase tracking-wide mt-0.5">
                Total Records
              </div>
            </div>
          </div>
          <div className="flex gap-1.5">
            <span
              className="px-2 py-0.5 rounded-full text-[9px] font-bold text-white"
              style={{ backgroundColor: accentColor }}
            >
              Completed
            </span>
            <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-green-100 text-green-700">
              Online
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
