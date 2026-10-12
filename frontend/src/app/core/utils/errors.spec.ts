import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './errors';

describe('errorMessage', () => {
  it('prefers the first field error, humanised', () => {
    const err = new HttpErrorResponse({ status: 400, error: { message: 'Validation failed', fieldErrors: { 'contact.phone': 'is not a valid phone number' } } });
    expect(errorMessage(err)).toBe('Phone is not a valid phone number');
  });

  it('uses the API message', () => {
    const err = new HttpErrorResponse({ status: 422, error: { message: 'Margherita is sold out right now' } });
    expect(errorMessage(err)).toBe('Margherita is sold out right now');
  });

  it('explains network failures', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('connection');
  });
});
