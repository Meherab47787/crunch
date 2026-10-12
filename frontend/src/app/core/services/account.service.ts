import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Address, AddressRequest, User } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  profile(): Observable<User> {
    return this.http.get<User>('/api/me');
  }

  updateProfile(body: { fullName: string; phone: string | null }): Observable<User> {
    return this.http.put<User>('/api/me', body);
  }

  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.post<void>('/api/me/password', body);
  }

  addresses(): Observable<Address[]> {
    return this.http.get<Address[]>('/api/me/addresses');
  }

  createAddress(body: AddressRequest): Observable<Address> {
    return this.http.post<Address>('/api/me/addresses', body);
  }

  updateAddress(id: number, body: AddressRequest): Observable<Address> {
    return this.http.put<Address>(`/api/me/addresses/${id}`, body);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`/api/me/addresses/${id}`);
  }
}
