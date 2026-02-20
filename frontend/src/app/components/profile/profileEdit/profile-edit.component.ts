import {Component, OnInit} from "@angular/core";
import {CommonModule} from "@angular/common";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors, ValidatorFn,
  Validators
} from "@angular/forms";
import {Router, RouterModule} from "@angular/router";
import {DetailedUserDto, UserUpdateDto} from "../../../dtos/user/user.dto";
import {UserService} from "../../../services/user.service";
import {ToastrService} from "ngx-toastr";
import { ErrorFormatterService } from "../../../services/error-formatter.service";
import { AuthService } from "../../../services/auth.service"; // Import hinzufügen

@Component({
  selector: 'app-profile-update',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile-edit.component.html',
  styleUrls: ['./profile-edit.component.scss']
})
export class ProfileEditComponent implements OnInit {
  profileForm: FormGroup;
  user?: DetailedUserDto;
  loading = false;
  profileLoading = false;
  isSubmitting = false;

  oldEmail = '';
  newEmail = '';
  pendingUpdateData?: UserUpdateDto;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    private notification: ToastrService,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService,
    private authService: AuthService // AuthService hinzufügen
  ) {
    this.profileForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', [Validators.required, Validators.maxLength(255)]],
      lastName: ['', [Validators.required, Validators.maxLength(255)]],

      street: ['', [Validators.required, Validators.maxLength(255)]],
      houseNumber: ['', [Validators.required, Validators.pattern(/^\d{1,3}$/)]],
      stair: ['', [Validators.pattern(/^\d{1,3}$/)]],
      door: ['', [Validators.pattern(/^\d{1,3}$/)]],
      postalCode: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]],
      city: ['', [Validators.required, Validators.maxLength(255)]]
    },
      { validators: maxCombinedAddressLength(255) }
    );
  }

  ngOnInit() {
    this.loadUserProfile();
  }

  private loadUserProfile() {
    this.loading = true;
    this.userService.getMeDetailed().subscribe({
      next: (user) => {
        this.user = user;
        this.profileForm.patchValue({
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName
        });
        this.splitAddress(user.address);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading profile', err);
        const errorMessage = this.errorFormatter.format(err);
        this.toastr.error(errorMessage, 'Fehler', { timeOut: 5000 });
        this.router.navigate(['/profile']);
        this.loading = false;
      }
    });
  }

  onProfileSubmit() {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const emailChanged = this.user && this.profileForm.value.email !== this.user.email;

    if (emailChanged) {
      this.oldEmail = this.user!.email;
      this.newEmail = this.profileForm.value.email;
      this.pendingUpdateData = {
        email: this.profileForm.value.email,
        firstName: this.profileForm.value.firstName,
        lastName: this.profileForm.value.lastName,
        address: this.buildAddress()
      };

      const modal = document.getElementById('emailChangeModal');
      if (modal) {
        const modalInstance = new (window as any).bootstrap.Modal(modal);
        modalInstance.show();
      }
    } else {
      this.submitProfileUpdate({
        email: this.profileForm.value.email,
        firstName: this.profileForm.value.firstName,
        lastName: this.profileForm.value.lastName,
        address: this.buildAddress()
      });
    }
  }

  confirmEmailChange() {
    if (this.pendingUpdateData) {
      this.submitProfileUpdate(this.pendingUpdateData);
    }
  }

  private submitProfileUpdate(updateData: UserUpdateDto) {
    this.profileLoading = true;
    this.isSubmitting = true;

    this.userService.updateProfile(updateData).subscribe({
      next: (updatedUser) => {
        const emailChanged = updateData.email !== this.user?.email;

        this.user = updatedUser;
        this.toastr.success(
          'Profil erfolgreich aktualisiert.',
          'Erfolg',
          { timeOut: 2000 }
        );
        this.profileLoading = false;
        this.isSubmitting = false;

        if (emailChanged) {
          this.toastr.info(
            'E-Mail wurde geändert. Sie werden abgemeldet und müssen sich mit der neuen E-Mail anmelden.',
            'E-Mail geändert',
            { timeOut: 5000 }
          );

          this.authService.logoutUser();
          setTimeout(() => {
            this.router.navigate(['/login'], {
              queryParams: {
                emailChanged: 'true',
                email: updateData.email
              }
            });
          }, 1500);

        } else {
          this.router.navigate(['/profile']);
        }
      },
      error: (err) => {
        console.error('Error updating profile:', err);
        const errorMessage = this.errorFormatter.format(err);

        this.toastr.error(
          errorMessage,
          'Fehler',
          { timeOut: 5000 }
        );

        this.profileLoading = false;
        this.isSubmitting = false;
      }
    });
  }

  cancel() {
    this.router.navigate(['/profile']);
  }

  buildAddress(): string {
    const {
      street,
      houseNumber,
      stair,
      door,
      postalCode,
      city
    } = this.profileForm.value;

    let numberPart = houseNumber;

    if (stair) {
      numberPart += `/${stair}`;
    }
    if (door) {
      numberPart += `/${door}`;
    }

    return `${street} ${numberPart}, ${postalCode} ${city}`;
  }

  private splitAddress(address?: string) {
    if (!address) return;

    const regex = /^(.+)\s(\d{1,3})(?:\/(\d{1,3}))?(?:\/(\d{1,3}))?,\s(\d{4})\s(.+)$/;
    const match = address.match(regex);

    if (!match) return;

    const [, street, houseNumber, stair, door, postalCode, city] = match;

    this.profileForm.patchValue({
      street,
      houseNumber,
      stair: stair ?? '',
      door: door ?? '',
      postalCode,
      city
    });
  }

  getAddressLength(): number {
    const address = this.buildAddress();
    return address.replace(/\s+/g, ' ').trim().length;
  }

}
function maxCombinedAddressLength(maxLength: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const street = control.get('street')?.value || '';
    const houseNumber = control.get('houseNumber')?.value || '';
    const stair = control.get('stair')?.value || '';
    const door = control.get('door')?.value || '';
    const postalCode = control.get('postalCode')?.value || '';
    const city = control.get('city')?.value || '';

    let numberPart = houseNumber;
    if (stair) numberPart += `/${stair}`;
    if (door) numberPart += `/${door}`;

    const fullAddress = `${street} ${numberPart}, ${postalCode} ${city}`.trim();

    const normalizedAddress = fullAddress.replace(/\s+/g, ' ');

    const addressLength = normalizedAddress.length;

    if (addressLength > maxLength) {
      return {
        addressTooLong: {
          length: addressLength,
          max: maxLength,
          address: normalizedAddress
        }
      };
    }

    return null;
  };
}
