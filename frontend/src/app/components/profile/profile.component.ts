import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service';
import {DetailedUserDto, RewardPointsDto} from '../../dtos/user/user.dto';
import {Router, RouterModule} from "@angular/router";
import { AuthService } from '../../services/auth.service';
import {ToastrService} from "ngx-toastr";
import {FormsModule} from "@angular/forms";
import { ErrorFormatterService } from '../../services/error-formatter.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  me?: DetailedUserDto;
  points?: RewardPointsDto;

  loading = false;
  error?: string;
  deleteLoading = false;

  isAdmin = false;
  isAuthenticated = false;
  userRole: string | null = null;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {}

  ngOnInit() {
    this.checkAuthStatus();

    if (!this.isAuthenticated) {
      this.error = 'Profil konnte nicht geladen werden';
      return;
    }

    this.loadProfile();
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isAdmin = this.userRole === 'ADMIN';
  }

  private loadProfile(): void {
    this.loading = true;
    this.error = undefined;

    let pending = 2;

    this.userService.getMeDetailed().subscribe({
      next: (me)=> {
        this.me = me;
        if (--pending === 0) {
          this.loading = false;
        }
      },
      error: (err)=> {
        console.error(err);
        const errorMessage = this.errorFormatter.format(err);
        this.toastr.error(errorMessage, 'Fehler', { timeOut: 5000 });
        this.error = 'Profil konnte nicht geladen werden.';
        this.loading = false;
      }
    });

    this.userService.getMyRewardPoints().subscribe({
      next: (points) => {
        this.points = points;
        if (--pending === 0) {
          this.loading = false;
        }
      },
      error: (err) => {
        console.error(err);
        const errorMessage = this.errorFormatter.format(err);
        this.toastr.error(errorMessage, 'Fehler', { timeOut: 5000 });
        this.error = 'Prämienpunkte konnten nicht geladen werden.';
        if (--pending === 0) {
          this.loading = false;
        }
      }
    });
  }

  confirmDelete(): void {
    if (!confirm('Sind Sie sicher, dass Sie Ihr Konto endgültig löschen möchten?')) {
      return;
    }

    this.deleteLoading = true;

    this.userService.deleteAccount().subscribe({
      next: () => {
        this.toastr.success('Ihr Konto wurde erfolgreich gelöscht.');
        this.authService.logoutUser();
        this.router.navigate(['/']);
        this.deleteLoading = false;
      },
      error: (err) => {
        console.error('Error deleting account:', err);

        if (err.status === 409) {
          this.toastr.error('Konto kann nicht gelöscht werden');
        } else {
          this.toastr.error('Konto konnte nicht gelöscht werden.');
        }

        this.deleteLoading = false;
      }
    });
  }

  deleteAccount(): void {
    if (this.isAdmin) {
      this.toastr.info('Als Admin können Sie Ihr Benutzerprofil nich löschen.', 'Info');
      this.router.navigate(['/admin/user-management']);
      return;
    }
    this.deleteLoading = true;

    this.userService.deleteAccount().subscribe({
      next: () => {
        this.toastr.success(
          'Ihr Konto wurde erfolgreich gelöscht.',
          'Erfolg',
          { timeOut: 2000 }
        );
        this.authService.logoutUser();
        this.router.navigate(['/']);
        this.deleteLoading = false;
      },
      error: (err) => {
        console.error('Error deleting account:', err);
        const errorMessage = this.errorFormatter.format(err);

        this.toastr.error(
          errorMessage,
          'Fehler',
          { timeOut: 5000 }
        );

        this.deleteLoading = false;
      }
    });
  }

  formatAddress(address: string): string {
    if (!address || address === '-') return '-';
    return address;
  }
}
