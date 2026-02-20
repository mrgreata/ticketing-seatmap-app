import { Component, OnInit } from '@angular/core';
import { AdminUserService } from '../../services/admin-user.service';
import { DetailedUserDto } from '../../dtos/user/user.dto';
import { ToastrService } from 'ngx-toastr';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import * as bootstrap from 'bootstrap';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  standalone: false
})
export class UserManagementComponent implements OnInit {

  users: DetailedUserDto[] = [];

  page = 0;
  pageSize = 10;
  totalElements = 0;
  showLockedUsers = false;

  searchTerm = '';

  currentUserEmail!: string;
  loading = true;
  error: string | null = null;

  constructor(
    private adminUserService: AdminUserService,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUserEmail = this.authService.getCurrentUserEmail();
    this.loadUsers();
  }

  /**
   * Loads users from backend with pagination, lock filter and optional search.
   */
  loadUsers(): void {
    this.loading = true;

    this.adminUserService.getUsers({
      locked: this.showLockedUsers,
      page: this.page,
      size: this.pageSize,
      search: this.searchTerm.trim() || null
    }).subscribe({
      next: response => {
        this.users = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: err => {
        this.error = this.errorFormatter.format(err);
        this.loading = false;
      }
    });
  }

  /**
   * Called when the search input changes.
   * Resets to first page and reloads users.
   */
  onSearchChange(): void {
    this.page = 0;
    this.loadUsers();
  }

  /**
   * Toggles between active and locked users.
   */
  toggleLockedUsers(locked: boolean): void {
    this.showLockedUsers = locked;
    this.page = 0;
    this.loadUsers();
  }

  /**
   * Navigates to a different page.
   */
  goToPage(page: number): void {
    if (page < 0) {
      return;
    }
    this.page = page;
    this.loadUsers();
  }

  /**
   * Sorts users so that administrators are always listed first.
   */
  sortAdminsFirst(users: DetailedUserDto[]): DetailedUserDto[] {
    return [...users].sort((a, b) =>
      a.userRole === b.userRole
        ? 0
        : a.userRole === 'ROLE_ADMIN'
          ? -1
          : 1
    );
  }

  /**
   * Unlocks a locked user account.
   */
  unlock(id: number): void {
    this.adminUserService.updateLockState(id, false, false).subscribe({
      next: () => {
        this.toastr.success('Der Benutzer wurde erfolgreich entsperrt.');
        this.loadUsers();
      },
      error: err => {
        this.toastr.error(this.errorFormatter.format(err), 'Fehler');
      }
    });
  }

  /**
   * Locks an active user account.
   */
  lock(id: number): void {
    this.adminUserService.updateLockState(id, false,true).subscribe({
      next: () => {
        this.toastr.success('Benutzer wurde erfolgreich gesperrt');
        this.loadUsers();
      },
      error: err => {
        this.toastr.error(this.errorFormatter.format(err), 'Fehler');
      }
    });
  }

  /**
   * Triggers a password reset for the given user.
   */
  triggerPasswordReset(id: number): void {
    this.adminUserService.triggerPasswordReset(id).subscribe({
      next: () => {
        this.toastr.success(
          'Die Passwort-Zurücksetzungs-Mail wurde versendet.',
          'Erfolg'
        );
      },
      error: err => {
        this.toastr.error(this.errorFormatter.format(err), 'Fehler');
      }
    });
  }

  /**
   * Toggles the role of a user between ROLE_USER and ROLE_ADMIN.
   */
  toggleRole(user: DetailedUserDto): void {
    const newRole =
      user.userRole === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';

    this.adminUserService.updateUserRole(user.id, newRole).subscribe({
      next: () => {
        if (this.isCurrentUser(user)) {
          this.toastr.info(
            'Ihre Rolle wurde geändert. Bitte melden Sie sich erneut an.'
          );
          this.logoutAndRedirect();
        } else {
          this.toastr.success('Die Benutzerrolle wurde erfolgreich geändert.');
          this.loadUsers();
        }
      },
      error: err => {
        this.toastr.error(this.errorFormatter.format(err), 'Fehler');
      }
    });
  }

  /**
   * Checks whether the given user is the last remaining ACTIVE administrator.
   * Locked administrators do NOT count as active.
   */
  isLastActiveAdmin(user: DetailedUserDto): boolean {
    if (user.userRole !== 'ROLE_ADMIN') {
      return false;
    }

    const adminCountOnPage =
      this.users.filter(u => u.userRole === 'ROLE_ADMIN').length;

    return adminCountOnPage === 1 && this.totalElements === 1;
  }

  isCurrentUser(user: DetailedUserDto): boolean {
    return user.email === this.currentUserEmail;
  }

  openFinalSelfDowngrade(userId: number): void {
    const modalId = `self-role-final-${userId}`;
    const modalElement = document.getElementById(modalId);

    if (modalElement) {
      const modal = new bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  get totalPages(): number {
    return Math.ceil(this.totalElements / this.pageSize);
  }

  private logoutAndRedirect(): void {
    this.authService.logoutUser();
    this.router.navigate(['/login']);
  }
}
