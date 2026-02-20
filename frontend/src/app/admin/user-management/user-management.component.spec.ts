import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserManagementComponent } from './user-management.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ToastrService } from 'ngx-toastr';
import { AdminUserService } from '../../services/admin-user.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { ConfirmDialogComponent } from '../../components/confirm-dialog/confirm-dialog.component';
import { DetailedUserDto } from '../../dtos/user/user.dto';
import { Page } from '../../dtos/page.dto';

/**
 * Helper to create a valid Page<T> mock
 */
function pageOf<T>(
  content: T[],
  page = 0,
  size = 10
): Page<T> {
  const totalElements = content.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));

  return {
    content,
    number: page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page === totalPages - 1,
    empty: content.length === 0
  } as Page<T>;
}

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;

  let adminUserServiceSpy: jasmine.SpyObj<AdminUserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const lockedUsersMock: DetailedUserDto[] = [
    {
      id: 1,
      email: 'locked@test.com',
      firstName: 'Max',
      lastName: 'Mustermann',
      locked: true,
      userRole: 'ROLE_USER'
    }
  ];

  beforeEach(async () => {
    adminUserServiceSpy = jasmine.createSpyObj<AdminUserService>(
      'AdminUserService',
      [
        'getUsers',
        'updateLockState',
        'triggerPasswordReset',
        'updateUserRole'
      ]
    );

    adminUserServiceSpy.getUsers.and.returnValue(
      of(pageOf<DetailedUserDto>(lockedUsersMock))
    );

    adminUserServiceSpy.updateLockState.and.returnValue(of(void 0));
    adminUserServiceSpy.triggerPasswordReset.and.returnValue(of(void 0));
    adminUserServiceSpy.updateUserRole.and.returnValue(of(void 0));

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', [
      'success',
      'error'
    ]);

    await TestBed.configureTestingModule({
      declarations: [
        UserManagementComponent,
        ConfirmDialogComponent
      ],
      imports: [
        HttpClientTestingModule
      ],
      providers: [
        { provide: AdminUserService, useValue: adminUserServiceSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ------------------------------------------------------------
  // BASIC
  // ------------------------------------------------------------

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load users on init', () => {
    component.ngOnInit();

    expect(adminUserServiceSpy.getUsers).toHaveBeenCalled();
  });

  // ------------------------------------------------------------
  // LOCKED USERS
  // ------------------------------------------------------------

  it('should render locked users table when users exist', () => {
    component.loading = false;
    fixture.detectChanges();

    const table = fixture.debugElement.query(By.css('table'));
    expect(table).toBeTruthy();
  });

  it('should unlock user and reload users on success', () => {
    component.unlock(1);

    expect(adminUserServiceSpy.updateLockState)
    .toHaveBeenCalledWith(1, false, false);

    expect(toastrSpy.success).toHaveBeenCalled();
    expect(adminUserServiceSpy.getUsers).toHaveBeenCalled();
  });

  it('should show error toast when unlock fails', () => {
    adminUserServiceSpy.updateLockState.and.returnValue(
      throwError(() => new Error('Unlock failed'))
    );

    component.unlock(1);

    expect(toastrSpy.error).toHaveBeenCalled();
  });

  // ------------------------------------------------------------
  // PASSWORD RESET
  // ------------------------------------------------------------

  it('should trigger password reset and show success toast', () => {
    component.triggerPasswordReset(1);

    expect(adminUserServiceSpy.triggerPasswordReset)
    .toHaveBeenCalledWith(1);

    expect(toastrSpy.success).toHaveBeenCalledWith(
      'Die Passwort-Zurücksetzungs-Mail wurde versendet.',
      'Erfolg'
    );
  });

  it('should show error toast when password reset fails', () => {
    adminUserServiceSpy.triggerPasswordReset.and.returnValue(
      throwError(() => new Error('Reset failed'))
    );

    component.triggerPasswordReset(1);

    expect(toastrSpy.error).toHaveBeenCalledWith(
      'Ein unerwarteter Fehler ist aufgetreten.',
      'Fehler'
    );
  });

  // ------------------------------------------------------------
  // UI
  // ------------------------------------------------------------

  it('should render password reset button for locked user', () => {
    component.loading = false;
    fixture.detectChanges();

    const resetButton = fixture.debugElement
    .queryAll(By.css('button'))
    .find(btn =>
      btn.nativeElement.textContent.includes('Passwort')
    );

    expect(resetButton).toBeTruthy();
  });
});
