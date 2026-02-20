import { Injectable } from '@angular/core';
import { AuthRequest } from '../dtos/auth-request';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { jwtDecode } from 'jwt-decode';
import { Globals } from '../global/globals';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authBaseUri: string = this.globals.backendUri + '/authentication';

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) {}

  /**
   * Login in the user. If it was successful, a valid JWT token will be stored
   */
  loginUser(authRequest: AuthRequest): Observable<string> {
    return this.httpClient
    .post(this.authBaseUri, authRequest, { responseType: 'text' })
    .pipe(
      tap((authResponse: string) => this.setToken(authResponse))
    );
  }

  /**
   * Check if a valid JWT token is saved in the localStorage
   */
  isLoggedIn(): boolean {
    return (
      !!this.getToken() &&
      this.getTokenExpirationDate(this.getToken()).valueOf() > new Date().valueOf()
    );
  }

  logoutUser(): void {
    localStorage.removeItem('authToken');
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  public loginWithToken(token: string): void {
    if (token.startsWith('Bearer ')) {
      token = token.substring(7);
    }
    localStorage.setItem('authToken', token);
  }

  /**
   * Returns the user role based on the current token
   */
  getUserRole(): string {
    if (this.getToken() != null) {
      const decoded: any = jwtDecode(this.getToken());
      const authInfo: string[] = decoded.rol;

      if (authInfo.includes('ROLE_ADMIN')) {
        return 'ADMIN';
      } else if (authInfo.includes('ROLE_USER')) {
        return 'USER';
      }
    }
    return 'UNDEFINED';
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isUser(): boolean {
    return this.getUserRole() === 'USER';
  }

  private setToken(authResponse: string): void {
    if (authResponse.startsWith('Bearer ')) {
      authResponse = authResponse.substring(7);
    }
    localStorage.setItem('authToken', authResponse);
  }

  private getTokenExpirationDate(token: string): Date {
    const decoded: any = jwtDecode(token);
    const date = new Date(0);
    date.setUTCSeconds(decoded.exp);
    return date;
  }

  getCurrentUserEmail(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const decoded: any = jwtDecode(token);
      return decoded.sub ?? null;
    } catch {
      return null;
    }
  }
}
