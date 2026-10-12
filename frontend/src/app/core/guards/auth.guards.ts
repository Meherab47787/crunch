import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/api.models';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.token()) return true;
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export function roleGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.token()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    const role = auth.user()?.role;
    return role && roles.includes(role) ? true : router.createUrlTree(['/']);
  };
}

/** Keeps signed-in users away from the login/register pages. */
export const guestOnlyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? inject(Router).createUrlTree([auth.homeFor(auth.user())]) : true;
};
