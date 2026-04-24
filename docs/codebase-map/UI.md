# UI Documentation - geojit-contract-note-ui

## Overview

**Framework**: Next.js 15 with App Router
**UI Library**: React 19
**Language**: TypeScript
**Styling**: Tailwind CSS
**Component Library**: shadcn/ui

## Entry Point

- **Root**: `src/app/page.tsx` - Redirects to `/login` or `/dashboard`
- **Login**: `src/app/login/page.tsx` - Authentication page
- **Root Layout**: `src/app/layout.tsx` - Global layout and metadata
- **Dashboard Layout**: `src/app/(dashboard)/layout.tsx` - Protected dashboard layout

## State Management

- **Auth**: Local state + session storage for JWT token
- **UI State**: React `useState` and `useEffect` hooks
- **Toast Notifications**: `sonner` library
- **No global state library** (Redux/Zustand) - using component-level state

## Key Pages and Routes

### Public Routes
| Route | File | Purpose |
|-------|------|---------|
| `/` | `src/app/page.tsx` | Home (redirects to dashboard or login) |
| `/login` | `src/app/login/page.tsx` | User authentication |

### Protected Routes (Dashboard)
All routes under `src/app/(dashboard)/`:

| Route | File | Purpose | API Calls |
|-------|------|---------|-----------|
| `/dashboard` | `dashboard/page.tsx` | Main dashboard with metrics | `GET /api/dashboard` |
| `/jobs` | `jobs/page.tsx` | List all jobs | `GET /api/jobs` |
| `/jobs/[jobId]` | `jobs/[jobId]/page.tsx` | Job detail with customers | `GET /api/jobs/{id}`, `GET /api/jobs/{id}/customers` |
| `/clients` | `clients/page.tsx` | Search clients | `GET /api/clients/search` |
| `/clients/[partyCode]` | `clients/[partyCode]/page.tsx` | Client detail with PDFs and timeline | `GET /api/clients/{code}/pdfs`, `GET /api/clients/{code}/timeline` |
| `/users` | `users/page.tsx` | User management (admin) | `GET /api/users`, `POST /api/users` |
| `/templates` | `templates/page.tsx` | Email template editor | `GET /api/templates`, `POST /api/templates` |
| `/certificates` | `certificates/page.tsx` | Certificate management | `GET /api/config/certificates` |
| `/ses-config` | `ses-config/page.tsx` | SES configuration | `GET /api/config/ses` |
| `/audit` | `audit/page.tsx` | Audit log viewer | `GET /api/audit` |
| `/process` | `process/page.tsx` | File upload and processing | `POST /api/jobs/upload`, `POST /api/jobs/validate` |
| `/resend` | `resend/page.tsx` | Bulk resend failed emails | `POST /api/jobs/{id}/resend` |
| `/exceptions` | `exceptions/page.tsx` | Email suppression list | `GET /api/suppression` |

## Layout Components

### `src/components/layout/`

| Component | Responsibility |
|-----------|---------------|
| **Header.tsx** | Top navigation bar with user menu, logout |
| **Sidebar.tsx** | Left navigation menu with route links |

## UI Components

### `src/components/ui/` (shadcn/ui)

Pre-built, customizable components:

- **alert-dialog.tsx** - Confirmation dialogs
- **alert.tsx** - Alert/notification banners
- **avatar.tsx** - User avatar display
- **badge.tsx** - Status badges (success, error, pending)
- **button.tsx** - Primary, secondary, outline buttons
- **card.tsx** - Content containers
- **dialog.tsx** - Modal dialogs
- **dropdown-menu.tsx** - Dropdown menus
- **input.tsx** - Form inputs
- **label.tsx** - Form labels
- **progress.tsx** - Progress bars
- **select.tsx** - Dropdown selects
- **separator.tsx** - Horizontal dividers
- **sonner.tsx** - Toast notifications
- **table.tsx** - Data tables
- **tabs.tsx** - Tab navigation
- **textarea.tsx** - Multi-line text inputs

## API Integration Pattern

All pages follow this pattern:

```typescript
// Fetch data on mount
useEffect(() => {
  fetch('/api/endpoint', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
  .then(res => res.json())
  .then(data => setState(data))
}, [])
```

### API Endpoints Called by UI

| UI Page | API Endpoint | Method | Purpose |
|---------|--------------|--------|---------|
| Dashboard | `/api/dashboard` | GET | Metrics (jobs, emails, hourly activity) |
| Jobs List | `/api/jobs` | GET | All jobs with pagination |
| Job Detail | `/api/jobs/{id}` | GET | Single job details |
| Job Customers | `/api/jobs/{id}/customers` | GET | Customers in a job |
| Upload | `/api/jobs/upload` | POST | Upload CSV file |
| Validate | `/api/jobs/validate` | POST | Validate CSV structure |
| Resend | `/api/jobs/{id}/resend/{partyCode}` | POST | Resend email to customer |
| Bulk Resend | `/api/jobs/{id}/bulk-resend` | POST | Resend to all failed |
| Client Search | `/api/clients/search?q={query}` | GET | Search by party code/email |
| Client PDFs | `/api/clients/{code}/pdfs` | GET | List PDFs for client |
| Client Timeline | `/api/clients/{code}/timeline` | GET | Email event history |
| Users | `/api/users` | GET | All users |
| Create User | `/api/users` | POST | Create new user |
| Update User | `/api/users/{id}` | PUT | Update user details |
| Templates | `/api/templates` | GET | All email templates |
| Create Template | `/api/templates` | POST | Create new template |
| Activate Template | `/api/templates/{id}/activate` | POST | Set active template |
| Certificates | `/api/config/certificates` | GET | All certificates |
| Upload Cert | `/api/config/certificates` | POST | Upload new certificate |
| SES Config | `/api/config/ses` | GET | SES configurations |
| Activate SES | `/api/config/ses/{id}/activate` | POST | Activate SES config |
| Audit Log | `/api/audit` | GET | Audit events |
| Suppression List | `/api/suppression` | GET | Blocked emails |
| Add Suppression | `/api/suppression` | POST | Block an email |
| Remove Suppression | `/api/suppression/{id}` | DELETE | Unblock an email |

## Real-Time Features

### Server-Sent Events (SSE)

Used in **Job Detail** page for real-time updates:

```typescript
const eventSource = new EventSource(`/api/jobs/${jobId}/stream`)
eventSource.onmessage = (event) => {
  // Update UI with pipeline events
}
```

## File Structure

```
geojit-contract-note-ui/
├── src/
│   ├── app/
│   │   ├── (dashboard)/          # Protected routes group
│   │   │   ├── audit/
│   │   │   │   └── page.tsx      # Audit log viewer
│   │   │   ├── certificates/
│   │   │   │   └── page.tsx      # Certificate management
│   │   │   ├── clients/
│   │   │   │   ├── [partyCode]/
│   │   │   │   │   └── page.tsx  # Client detail (dynamic route)
│   │   │   │   └── page.tsx      # Client search
│   │   │   ├── dashboard/
│   │   │   │   └── page.tsx      # Main dashboard
│   │   │   ├── exceptions/
│   │   │   │   └── page.tsx      # Suppression list
│   │   │   ├── jobs/
│   │   │   │   ├── [jobId]/
│   │   │   │   │   └── page.tsx  # Job detail (dynamic route)
│   │   │   │   └── page.tsx      # Jobs list
│   │   │   ├── process/
│   │   │   │   └── page.tsx      # File upload
│   │   │   ├── resend/
│   │   │   │   └── page.tsx      # Bulk resend
│   │   │   ├── ses-config/
│   │   │   │   └── page.tsx      # SES configuration
│   │   │   ├── templates/
│   │   │   │   └── page.tsx      # Email templates
│   │   │   ├── users/
│   │   │   │   └── page.tsx      # User management
│   │   │   └── layout.tsx        # Dashboard layout (Sidebar + Header)
│   │   ├── login/
│   │   │   └── page.tsx          # Login page
│   │   ├── layout.tsx            # Root layout
│   │   ├── page.tsx              # Home redirect
│   │   └── providers.tsx         # Context providers
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Header.tsx        # Top navigation
│   │   │   └── Sidebar.tsx       # Left menu
│   │   └── ui/                   # shadcn/ui components
│   │       ├── button.tsx
│   │       ├── card.tsx
│   │       ├── dialog.tsx
│   │       ├── dropdown-menu.tsx
│   │       ├── input.tsx
│   │       ├── table.tsx
│   │       └── ... (other components)
│   ├── lib/
│   │   └── utils.ts              # Utility functions
│   └── styles/
│       └── globals.css           # Global styles + Tailwind
├── public/                       # Static assets
├── package.json
├── tsconfig.json
├── tailwind.config.ts
└── next.config.js
```

## Key User Flows

### 1. Upload and Process Job
1. Navigate to `/process`
2. Select CSV file
3. Click "Validate" → `POST /api/jobs/validate`
4. If valid, click "Upload" → `POST /api/jobs/upload`
5. Redirected to `/jobs/[jobId]` with SSE updates

### 2. Monitor Job Status
1. Navigate to `/jobs`
2. Click on a job → `/jobs/[jobId]`
3. View customers in table
4. Watch SSE updates for PDF generation and email delivery

### 3. Resend Failed Emails
1. Navigate to `/jobs/[jobId]`
2. Filter customers by "Failed" status
3. Click "Resend" on individual customer
4. Or navigate to `/resend` for bulk operations

### 4. Search Client
1. Navigate to `/clients`
2. Enter party code or email
3. Click on result → `/clients/[partyCode]`
4. View PDFs and email timeline

### 5. Manage Templates
1. Navigate to `/templates`
2. Create/edit HTML email template
3. Validate template → `POST /api/templates/validate`
4. Activate template → `POST /api/templates/{id}/activate`

## Authentication Flow

1. User enters credentials on `/login`
2. `POST /api/auth/login` with `{ email, password }`
3. Receive JWT token in response
4. Store token in localStorage
5. Include token in all subsequent requests:
   ```
   Authorization: Bearer <token>
   ```
6. On `/dashboard` pages, check for token
7. If missing, redirect to `/login`
8. Logout via Header menu → `POST /api/auth/logout`

## Critical Flows (From Graph)

Top execution flows:

1. **UsersPage** (criticality: 0.67) - 12 nodes
2. **LoginPage** (criticality: 0.62) - 5 nodes
3. **CertificatesPage** (criticality: 0.595) - 10 nodes
4. **DashboardLayout** (criticality: 0.595) - 5 nodes
5. **JobDetailPage** (criticality: 0.51) - 5 nodes

## Styling Approach

- **Tailwind CSS** for utility-first styling
- **shadcn/ui** for pre-built component primitives
- **Custom theme** via `tailwind.config.ts`
- **Responsive design** with mobile-first breakpoints
- **Dark mode** support (if configured)

## Dependencies

Key npm packages:

- `next` - React framework
- `react` - UI library
- `typescript` - Type safety
- `tailwindcss` - Styling
- `@radix-ui/*` - Headless UI primitives (used by shadcn)
- `lucide-react` / `@heroicons/react` - Icons
- `sonner` - Toast notifications
- `class-variance-authority` - Component variants
- `clsx` / `tailwind-merge` - Class name utilities

## Error Handling

- **API errors**: Display toast notifications
- **Validation errors**: Show inline form errors
- **Network errors**: Catch and display user-friendly messages
- **Auth errors**: Redirect to `/login`

## Testing

- No test files detected in graph (Test nodes: 15 total, but may be in API)
- UI tests likely in `__tests__/` or `.test.tsx` files (if present)
