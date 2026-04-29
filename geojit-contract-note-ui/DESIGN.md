---
name: MOSL Contract Note UI
colors:
  primary: "#00174b"
  on-primary: "#ffffff"
  secondary: "#497cff"
  on-secondary: "#ffffff"
  tertiary: "#003ea8"
  on-tertiary: "#ffffff"
  background: "#ffffff"
  on-background: "#0f172a"
  surface: "#ffffff"
  surface-dim: "#f7f9fb"
  on-surface: "#0f172a"
  on-surface-variant: "#64748b"
  outline: "#eef2f7"
  error: "#ef4444"
  on-error: "#ffffff"
  success: "#10b981"
  warning: "#f59e0b"
  info: "#3b82f6"
typography:
  font-families:
    headline: "Manrope"
    sans: "Inter"
    mono: "JetBrains Mono"
radii:
  sm: "0.375rem"
  base: "0.625rem"
  lg: "0.85rem"
  xl: "1rem"
  pill: "9999px"
shadows:
  card: "0 1px 2px rgba(15, 23, 42, 0.04)"
  hover: "0 4px 16px rgba(15, 23, 42, 0.06)"
---

# Design Rationale

**MOSL Contract Note UI** is a back-office operations platform tailored for processing and monitoring financial contract notes. The design system is crafted to convey trust, stability, and high functionality, ensuring that operators can efficiently parse dense information without fatigue.

## 1. Aesthetic & Brand Identity
The UI embodies a **Modern Financial / Corporate** aesthetic. It avoids overly sterile minimalism by introducing generous rounding and soft, subtle contrast, making the dense, data-heavy environment feel approachable and modern.

## 2. Color Philosophy
- **Authority & Trust:** The brand is anchored by a deep "MOSL Navy" (`#00174b`), which is used for sidebars, primary actions, and active states to ground the user in a secure, authoritative environment.
- **Action & Focus:** A vibrant, energetic blue (`#497cff`) serves as the primary accent and secondary color, drawing the eye to progress rings, active pipeline nodes, and interactive elements.
- **Low-Fatigue Surfaces:** To prevent eye strain during long operational sessions, the application relies on a soft slate/gray surface (`#f7f9fb` and borders `#eef2f7`) against pure white (`#ffffff`) cards, ensuring clear visual hierarchy with minimal stark contrast.
- **Semantic Status:** Heavy reliance on semantic colors (Emerald for Success, Amber for Warning, Red for Error, Blue for Info) to immediately communicate the health of automated jobs and pipelines.

## 3. Typography
The system utilizes a tri-font architecture to balance branding with extreme legibility:
- **Manrope (`headline`):** Used for page titles and major headings. Its slight geometric nature provides a modern, polished corporate feel.
- **Inter (`sans`):** The primary workhorse font for body text, form fields, and data tables. It offers exceptional legibility in dense UI layouts.
- **JetBrains Mono (`mono`):** Applied to technical data, unique identifiers, status logs, and tabular numeric data. It ensures character precision and alignment where data accuracy is critical.

## 4. Shape & Elevation
- **Soft Geometry:** The system uses relatively generous corner radii (e.g., `1rem` for major cards and `0.625rem` for base elements). This "softens" the rigid structure typical of financial dashboards.
- **Subtle Depth:** The interface is largely flat, utilizing extremely subtle borders (`#eef2f7`) and minimal drop shadows (`0 1px 2px`) to define boundaries. Interactive elements, such as metric KPI tiles, slightly elevate on hover (`0 4px 16px` with a `-2px` Y-translation) to provide tactile feedback without cluttering the screen.

## 5. Motion, Feedback & Pipelines
Motion is used purposefully to communicate system state, creating a "live" feel for ongoing automated processes:
- **Dash Flow:** Pipeline connectors (`.pipe-active`) utilize a scrolling dashed linear gradient to visualize data moving through the system.
- **Pulse Indicators:** Running states (`.dot-run`, `.ring-active`) use gentle, infinite pulse and scale animations to indicate that an operation is currently processing.
- **Micro-Interactions:** Quick fade-ups and scale transitions are used on tooltips, dialogs, and toast notifications to ensure feedback feels responsive and snappy.

## 6. Component Primitives
- **Pills & Dots:** Status indicators are heavily condensed into pill badges (`.pill-ok`, `.pill-warn`) and small dots to allow high-density status tracking in data tables and logs.
- **Metric Cards:** Clean, white tiles that house key performance indicators, designed to be the primary visual anchors on the dashboard.
- **Wizard Steppers & Nodes:** Complex multi-step processes use clear, rounded nodes (`.pnode`, `.step-dot`) with distinctive "done", "current", and "pending" states to guide the operator through operational flows.