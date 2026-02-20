import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { PasswordResetService } from '../../../services/password-reset.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';

@Component({
  selector: 'app-request',
  templateUrl: './request.component.html',
  styleUrl: './request.component.scss',
  standalone: false
})
export class RequestComponent {

  resetForm: FormGroup;
  loading = false;
  success = false;

  constructor(
    private fb: FormBuilder,
    private passwordResetService: PasswordResetService,
    private notification: ToastrService
  ) {
    this.resetForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  requestReset(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.passwordResetService
    .requestPasswordReset(this.resetForm.value.email)
    .subscribe({
      next: () => this.handleSuccess(),
      error: () => this.handleSuccess()
    });
  }

  private handleSuccess(): void {
    this.loading = false;
    this.success = true;

    this.notification.success(
      'Falls ein Konto existiert, wurde eine E-Mail versendet.',
      'Passwort zurücksetzen'
    );
  }
}
