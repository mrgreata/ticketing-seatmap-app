import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { AdminUserService } from '../../services/admin-user.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';

@Component({
  selector: 'app-create-user',
  templateUrl: './create-user.component.html',
  styleUrl: './create-user.component.scss',
  standalone: false
})
export class CreateUserComponent {

  createUserForm: FormGroup;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private adminUserService: AdminUserService,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {
    this.createUserForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      userRole: [null, Validators.required]
    });
  }

  onSubmit(): void {
    if (this.createUserForm.invalid) {
      this.createUserForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    this.adminUserService.createUser(this.createUserForm.value).subscribe({
      next: () => {
        this.toastr.success('Benutzer wurde erfolgreich angelegt.');
        this.createUserForm.reset();
        this.submitting = false;
      },
      error: (error) => {
        this.toastr.error(this.errorFormatter.format(error), 'Benutzer konnte nicht angelegt werden.');
        this.submitting = false;
      }
    });
  }
}
