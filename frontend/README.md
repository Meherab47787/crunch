# Crust & Crunch — Angular frontend

Angular 21 · standalone components · signals (zoneless) · Reactive Forms · Tailwind CSS v4 · Lucide icons.
The full project overview (backend, Docker, API) is in [`../README.md`](../README.md).

```bash
npm install
npm start               # http://localhost:4200 — proxies /api and /uploads to localhost:8080
npm test                # unit tests (Vitest)
npm run e2e             # Playwright; see ../README.md for the required env vars
npx ng build            # production build into dist/
```

## Where things live

```
src/
├── styles.css            entry point: just @imports the partials below
├── styles/               global design system only
│   ├── theme.css           design tokens (colours, fonts, shadows, animations)
│   ├── base.css            element defaults, focus ring, reduced-motion
│   ├── layout.css          .container-page
│   ├── buttons.css         .btn + variants (btn-primary, btn-outline, btn-sm …)
│   ├── forms.css           .input, .label, .field-error
│   ├── surfaces.css        .card, .chip, .skeleton, admin tables
│   └── print.css           receipt / kitchen-ticket printing
└── app/
    ├── app.ts / app.html / app.config.ts / app.routes.ts
    ├── core/               app-wide, UI-free code
    │   ├── models/           TypeScript mirrors of the backend DTOs
    │   ├── services/         API + state (auth, cart, orders, admin, toast, confirm …)
    │   ├── guards/ interceptors/ utils/
    ├── shared/             reusable UI used by several features (product card, modal, toasts …)
    ├── layout/             storefront shell and cart drawer
    └── features/           one folder per route area
        ├── home  menu  product  cart  checkout  orders  auth  account
        ├── admin/            lazy-loaded back office (dashboard, orders, products, promotions …)
        └── kitchen/          full-screen kitchen display
```

## Conventions

- **Three files per component, side by side**: `name.component.ts` (logic), `name.component.html`
  (markup), and `name.component.css` only when the component needs its own CSS. No inline templates.
- **Styling is Tailwind utility classes in the HTML.** Reusable patterns (`btn-primary`, `card`, `input` …)
  are defined once in `src/styles/`. Put CSS in a component's own `.css` file only for something Tailwind
  can't express; reference tokens with `var(--color-tomato-500)` rather than hard-coding colours.
- **Change a colour or font in one place**: `src/styles/theme.css`.
- **Prices never come from the browser.** Cart lines hold IDs and quantities; `CartService` asks
  `POST /api/cart/quote` for every figure shown. Don't add client-side price maths beyond previews.
- **Services hold state, components render it.** Services expose signals; components read them and call
  methods. Use the `remote()` helper (`core/utils/remote.ts`) for loading / error / data state.
- **Specs sit next to the code they test** (`*.spec.ts`); end-to-end tests are in `e2e/`.
