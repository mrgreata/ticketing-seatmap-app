import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { PasswordResetService } from '../../../services/password-reset.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';

@Component({
  selector: 'app-confirm',
  templateUrl: './confirm.component.html',
  styleUrls: ['./confirm.component.scss'],
  standalone: false
})
export class ConfirmComponent implements OnInit {

  resetForm!: FormGroup;
  loading = false;

  private token!: string;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private passwordResetService: PasswordResetService,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.toastr.error('Ungültiger oder fehlender Reset-Link', 'Fehler');
      this.router.navigate(['/login']);
      return;
    }

    this.resetForm = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
      },
      { validators: this.passwordsMatchValidator }
    );
  }

  onSubmit(): void {
    if (this.resetForm.invalid || this.loading) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const newPassword = this.resetForm.get('password')!.value;

    this.passwordResetService
    .confirmPasswordReset(this.token, newPassword)
    .subscribe({
      next: () => {
        this.toastr.success(
          'Dein Passwort wurde erfolgreich zurückgesetzt.',
          'Erfolg'
        );
        this.router.navigate(['/login']);
      },
      error: error => {
        this.toastr.error(
          this.errorFormatter.format(error),
          'Passwort zurücksetzen fehlgeschlagen'
        );
        this.loading = false;
      }
    });
  }

  private passwordsMatchValidator(form: FormGroup) {
    const password = form.get('password')?.value;
    const confirm = form.get('confirmPassword')?.value;

    return password === confirm ? null : { passwordMismatch: true };
  }
}
