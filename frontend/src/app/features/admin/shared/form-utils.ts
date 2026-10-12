import { AbstractControl, FormArray, FormGroup } from '@angular/forms';
import { apiError, errorMessage } from '../../../core/utils/errors';

/**
 * Copies server-side `fieldErrors` (e.g. `variants[0].name`) onto matching form controls as a
 * `server` error. Returns a general message for anything that could not be attached to a field.
 */
export function applyServerErrors(form: FormGroup, err: unknown): string | null {
  const api = apiError(err);
  const fields = api?.fieldErrors ?? {};
  const unmatched: string[] = [];
  for (const [path, msg] of Object.entries(fields)) {
    const ctrl = findControl(form, path);
    if (ctrl) {
      ctrl.setErrors({ ...(ctrl.errors ?? {}), server: msg });
      ctrl.markAsTouched();
    } else {
      // class-level errors (e.g. key "couponRequest") carry a full sentence already
      unmatched.push(path.includes('.') || path.includes('[') ? `${path}: ${msg}` : msg);
    }
  }
  if (Object.keys(fields).length === 0) return errorMessage(err);
  return unmatched.length ? unmatched.join(' · ') : null;
}

function findControl(form: FormGroup, path: string): AbstractControl | null {
  // "variants[0].name" -> ["variants", "0", "name"]; "comboComponents[1].choices[0].productId"
  const parts = path.replace(/\[(\d+)\]/g, '.$1').split('.').filter(Boolean);
  let ctrl: AbstractControl | null = form;
  for (const p of parts) {
    if (ctrl instanceof FormGroup) ctrl = ctrl.get(p);
    else if (ctrl instanceof FormArray) ctrl = ctrl.at(Number(p)) ?? null;
    else return null;
    if (!ctrl) return null;
  }
  return ctrl === form ? null : ctrl;
}

/** First human-readable error for a control, or null. Shown only after the user touched it. */
export function fieldError(ctrl: AbstractControl | null, label = 'This field'): string | null {
  if (!ctrl || !ctrl.errors || !(ctrl.touched || ctrl.dirty)) return null;
  const e = ctrl.errors;
  if (e['server']) return String(e['server']);
  if (e['required']) return `${label} is required.`;
  if (e['min']) return `${label} must be at least ${e['min'].min}.`;
  if (e['max']) return `${label} must be at most ${e['max'].max}.`;
  if (e['maxlength']) return `${label} must be ${e['maxlength'].requiredLength} characters or fewer.`;
  if (e['minlength']) return `${label} must be at least ${e['minlength'].requiredLength} characters.`;
  if (e['email']) return 'Enter a valid email address.';
  if (e['pattern']) return `${label} has an invalid format.`;
  return `${label} is invalid.`;
}

/** ISO instant -> value for <input type="datetime-local"> in the browser's timezone. */
export function toLocalInput(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** <input type="datetime-local"> value -> ISO instant (or null when empty). */
export function fromLocalInput(value: string | null | undefined): string | null {
  if (!value) return null;
  const d = new Date(value);
  return isNaN(d.getTime()) ? null : d.toISOString();
}

export function blankToNull(v: string | null | undefined): string | null {
  const t = (v ?? '').trim();
  return t ? t : null;
}

export function slugify(v: string): string {
  return v
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
