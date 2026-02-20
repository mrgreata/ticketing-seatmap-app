import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';
import { AuthRequest } from '../../dtos/auth-request';
import { ErrorFormatterService } from '../../services/error-formatter.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: false
})
export class LoginComponent {

  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notification: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  loginUser(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const authRequest = new AuthRequest(
      this.loginForm.value.email,
      this.loginForm.value.password
    );

    this.authenticateUser(authRequest);
  }

  private authenticateUser(authRequest: AuthRequest): void {
    this.authService.loginUser(authRequest).subscribe({
      next: () => {
        this.notification.success(
          'Anmeldung erfolgreich.',
          'Willkommen zurück'
        );
        this.router.navigate(['']);
      },
      error: error => {
        this.notification.error(
          this.errorFormatter.format(error),
          'Anmeldung fehlgeschlagen',
          { enableHtml: true, timeOut: 10000 }
        );
      }
    });
  }
}
