import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.models';

export function apiError(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'message' in err.error) {
    return err.error as ApiError;
  }
  return null;
}

/** Human-readable message for any failed request. */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (err instanceof HttpErrorResponse && err.status === 0) {
    return 'Can’t reach the restaurant right now. Check your connection and try again.';
  }
  const api = apiError(err);
  if (api?.fieldErrors && Object.keys(api.fieldErrors).length) {
    const [field, msg] = Object.entries(api.fieldErrors)[0];
    return `${humanize(field)} ${msg}`;
  }
  return api?.message ?? fallback;
}

function humanize(field: string): string {
  const last = field.split('.').pop() ?? field;
  const words = last.replace(/([A-Z])/g, ' $1').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}
