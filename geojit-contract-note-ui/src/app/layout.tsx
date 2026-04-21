import type { Metadata } from "next";
import { Manrope, Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";

const manrope = Manrope({
  variable: "--font-mosl-headline",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"]
});

const inter = Inter({
  variable: "--font-mosl-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"]
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-mosl-mono",
  subsets: ["latin"],
  weight: ["400", "500", "600"]
});

export const metadata: Metadata = {
  title: { default: "MOSL Contract Note", template: "%s | MOSL CN" },
  description: "Back-office operations platform for contract note processing",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${manrope.variable} ${inter.variable} ${jetbrainsMono.variable} h-full`}>
      <head>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&display=swap"
        />
        <style>{`
          :root {
            --font-mosl-headline: ${manrope.style.fontFamily};
            --font-mosl-sans: ${inter.style.fontFamily};
            --font-mosl-mono: ${jetbrainsMono.style.fontFamily};
          }
        `}</style>
      </head>
      <body className="h-full antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
