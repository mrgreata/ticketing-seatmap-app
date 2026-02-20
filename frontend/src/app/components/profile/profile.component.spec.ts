import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { ProfileComponent } from './profile.component';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { ErrorFormatterService } from '../../services/error-formatter.service';

import { DetailedUserDto, RewardPointsDto } from '../../dtos/user/user.dto';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  let userServiceSpy: jasmine.SpyObj<UserService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;
  let router: Router;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', [
      'getMeDetailed',
      'getMyRewardPoints',
      'deleteAccount'
    ]);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn',
      'getUserRole',
      'logoutUser'
    ]);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', [
      'success',
      'error',
      'info'
    ]);

    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>('ErrorFormatterService', [
      'format'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ProfileComponent
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if ((window.confirm as unknown as jasmine.Spy)?.and) {
      (window.confirm as unknown as jasmine.Spy).and.stub();
    }
  });

  it('should create', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.getUserRole.and.returnValue(null);

    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit should set error and not load profile when not authenticated', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.getUserRole.and.returnValue(null);

    fixture.detectChanges();

    expect(component.isAuthenticated).toBeFalse();
    expect(component.error).toBe('Profil konnte nicht geladen werden');
    expect(userServiceSpy.getMeDetailed).not.toHaveBeenCalled();
    expect(userServiceSpy.getMyRewardPoints).not.toHaveBeenCalled();
  });

  it('ngOnInit should load profile when authenticated', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');

    const me = { email: 'a@b.c' } as unknown as DetailedUserDto;
    const points = { rewardPoints: 10 } as unknown as RewardPointsDto;

    userServiceSpy.getMeDetailed.and.returnValue(of(me));
    userServiceSpy.getMyRewardPoints.and.returnValue(of(points));

    fixture.detectChanges();

    expect(component.isAuthenticated).toBeTrue();
    expect(component.isAdmin).toBeFalse();

    expect(userServiceSpy.getMeDetailed).toHaveBeenCalledTimes(1);
    expect(userServiceSpy.getMyRewardPoints).toHaveBeenCalledTimes(1);

    expect(component.me).toEqual(me);
    expect(component.points).toEqual(points);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeUndefined();
  });

  it('should set isAdmin=true when role is ADMIN', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('ADMIN');

    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));

    fixture.detectChanges();

    expect(component.isAdmin).toBeTrue();
  });

  it('loadProfile: if getMeDetailed fails, should show formatted error toast and set error + loading false', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');

    errorFormatterSpy.format.and.returnValue('Formatted ME error');

    userServiceSpy.getMeDetailed.and.returnValue(throwError(() => ({ status: 500 })));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));

    fixture.detectChanges();

    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalledWith('Formatted ME error', 'Fehler', { timeOut: 5000 });

    expect(component.error).toBe('Profil konnte nicht geladen werden.');
    expect(component.loading).toBeFalse();
  });

  it('loadProfile: if getMyRewardPoints fails, should show formatted error toast and set points error + loading false', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');

    errorFormatterSpy.format.and.returnValue('Formatted Points error');

    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();

    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalledWith('Formatted Points error', 'Fehler', { timeOut: 5000 });

    expect(component.error).toBe('Prämienpunkte konnten nicht geladen werden.');
    expect(component.loading).toBeFalse();
  });

  it('confirmDelete: should do nothing when user cancels confirm()', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');
    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(false);

    component.confirmDelete();

    expect(userServiceSpy.deleteAccount).not.toHaveBeenCalled();
    expect(component.deleteLoading).toBeFalse();
  });

  it('confirmDelete: should delete account, logout, navigate, and reset loading on success', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');
    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(true);
    userServiceSpy.deleteAccount.and.returnValue(of(void 0));

    component.confirmDelete();

    expect(component.deleteLoading).toBeFalse();
    expect(userServiceSpy.deleteAccount).toHaveBeenCalledTimes(1);

    expect(toastrSpy.success).toHaveBeenCalledWith('Ihr Konto wurde erfolgreich gelöscht.');
    expect(authServiceSpy.logoutUser).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('confirmDelete: should show 409 specific error message', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');
    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(true);
    userServiceSpy.deleteAccount.and.returnValue(throwError(() => ({ status: 409 })));

    component.confirmDelete();

    expect(component.deleteLoading).toBeFalse();
    expect(toastrSpy.error).toHaveBeenCalledWith('Konto kann nicht gelöscht werden');
  });

  it('confirmDelete: should show generic error message for non-409', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('USER');
    userServiceSpy.getMeDetailed.and.returnValue(of({} as DetailedUserDto));
    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 } as RewardPointsDto));
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(true);
    userServiceSpy.deleteAccount.and.returnValue(throwError(() => ({ status: 500 })));

    component.confirmDelete();

    expect(component.deleteLoading).toBeFalse();
    expect(toastrSpy.error).toHaveBeenCalledWith('Konto konnte nicht gelöscht werden.');
  });

  it('deleteAccount: when admin should show info and navigate to /admin/user-management (no delete call)', () => {
    component.isAdmin = true;

    component.deleteAccount();

    expect(toastrSpy.info).toHaveBeenCalledWith(
      'Als Admin können Sie Ihr Benutzerprofil nich löschen.',
      'Info'
    );
    expect(router.navigate).toHaveBeenCalledWith(['/admin/user-management']);
    expect(userServiceSpy.deleteAccount).not.toHaveBeenCalled();
  });

  it('deleteAccount: success should show success toast, logout, navigate, and reset loading', () => {
    component.isAdmin = false;

    userServiceSpy.deleteAccount.and.returnValue(of(void 0));

    component.deleteAccount();

    expect(userServiceSpy.deleteAccount).toHaveBeenCalledTimes(1);

    expect(toastrSpy.success).toHaveBeenCalledWith(
      'Ihr Konto wurde erfolgreich gelöscht.',
      'Erfolg',
      { timeOut: 2000 }
    );

    expect(authServiceSpy.logoutUser).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
    expect(component.deleteLoading).toBeFalse();
  });

  it('deleteAccount: error should format and show error toast and reset loading', () => {
    component.isAdmin = false;

    errorFormatterSpy.format.and.returnValue('Formatted delete error');
    userServiceSpy.deleteAccount.and.returnValue(throwError(() => ({ status: 500 })));

    component.deleteAccount();

    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalledWith(
      'Formatted delete error',
      'Fehler',
      { timeOut: 5000 }
    );
    expect(component.deleteLoading).toBeFalse();
  });
});
