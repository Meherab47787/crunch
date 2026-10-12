import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/api.models';
import { safeStorage } from '../utils/storage';

interface StoredSession {
  token: string;
  expiresAt: number;
  user: User;
}

const KEY = 'cc.session.v1';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<StoredSession | null>(this.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly isStaff = computed(() => {
    const r = this.user()?.role;
    return r === 'ADMIN' || r === 'KITCHEN_STAFF';
  });

  token(): string | null {
    const s = this.session();
    if (!s) return null;
    if (Date.now() >= s.expiresAt) {
      this.clear();
      return null;
    }
    return s.token;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(tap((r) => this.store(r)));
  }

  register(body: { email: string; password: string; fullName: string; phone: string | null }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', body).pipe(tap((r) => this.store(r)));
  }

  /** Re-fetches the profile (role/enabled may have changed server-side). */
  refreshMe(): void {
    if (!this.token()) return;
    this.http.get<User>('/api/auth/me').subscribe({
      next: (user) => this.updateUser(user),
      error: () => {
        /* interceptor handles 401 */
      },
    });
  }

  updateUser(user: User): void {
    const s = this.session();
    if (!s) return;
    const next = { ...s, user };
    this.session.set(next);
    safeStorage.set(KEY, next);
  }

  logout(redirect = true): void {
    this.clear();
    if (redirect) this.router.navigateByUrl('/');
  }

  /** Where to land after login, by role. */
  homeFor(user: User | null): string {
    if (user?.role === 'ADMIN') return '/admin';
    if (user?.role === 'KITCHEN_STAFF') return '/kitchen';
    return '/';
  }

  private store(r: AuthResponse): void {
    const s: StoredSession = { token: r.token, expiresAt: Date.now() + r.expiresInSeconds * 1000 - 30_000, user: r.user };
    this.session.set(s);
    safeStorage.set(KEY, s);
  }

  private clear(): void {
    this.session.set(null);
    safeStorage.remove(KEY);
  }

  private restore(): StoredSession | null {
    const s = safeStorage.get<StoredSession>(KEY);
    if (!s || !s.token || Date.now() >= s.expiresAt) {
      safeStorage.remove(KEY);
      return null;
    }
    return s;
  }
}
