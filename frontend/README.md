# Frontend (EasyCloudPan)

## Goals / Budgets (Production Build)

These budgets are enforced by review + verification commands, not by CI hard limits yet.

- PWA precache size: target `< 2MB` (prefer `< 1MB`)
- Avoid shipping preview-heavy dependencies on first load (PDF/XLSX/DOCX/media/highlight)
- Keep authentication data out of long-lived storage unless explicitly needed

## Verify (Local)

```powershell
Set-Location frontend

# Type safety
npm run type-check

# Lint (no auto-fix)
npm run lint:check

# Formatting (no writes)
npm run format:check

# Production build output (sizes + PWA precache summary)
npm run build

# E2E (requires backend + infra to be reachable via /api proxy)
npm run test:e2e
```

