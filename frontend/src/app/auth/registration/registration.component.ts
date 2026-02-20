import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { UserRegisterDto } from '../../dtos/user/user-registration.dto';
import { UserService } from '../../services/user.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.scss'],
  standalone: false
})
export class RegistrationComponent {

  registerForm: FormGroup;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(255)]],
      lastName: ['', [Validators.required, Validators.maxLength(255)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const dto: UserRegisterDto = this.registerForm.value;

    this.userService.registerUser(dto).subscribe({
      next: response => {
        this.authService.loginWithToken(response.token);

        this.toastr.success(
          'Registrierung erfolgreich. Sie sind jetzt angemeldet.',
          'Willkommen'
        );

        this.router.navigate(['']);
      },
      error: error => {
        this.toastr.error(
          this.errorFormatter.format(error),
          'Registrierung fehlgeschlagen',
          { enableHtml: true, timeOut: 10000 }
        );
        this.submitting = false;
      }
    });
  }
}
