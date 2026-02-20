import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Globals } from '../global/globals';

@Injectable({
  providedIn: 'root',
})
export class PasswordResetService {

  private usersBaseUri: string = this.globals.backendUri + '/users';

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) {}

  /**
   * Request a password reset link via email.
   * The response does not reveal whether the email exists.
   */
  requestPasswordReset(email: string): Observable<void> {
    return this.httpClient.post<void>(
      `${this.usersBaseUri}/password-reset/request`,
      { email }
    );
  }

  /**
   * Confirm the password reset using the token sent via email.
   */
  confirmPasswordReset(token: string, newPassword: string): Observable<void> {
    return this.httpClient.post<void>(
      `${this.usersBaseUri}/password-reset/confirmation`,
      {
        token,
        newPassword
      }
    );
  }
}
