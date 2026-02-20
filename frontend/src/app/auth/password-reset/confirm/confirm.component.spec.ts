import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmComponent } from './confirm.component';

import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, throwError } from 'rxjs';

import { PasswordResetService } from '../../../services/password-reset.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';

describe('ConfirmPasswordResetComponent', () => {

  let component: ConfirmComponent;
  let fixture: ComponentFixture<ConfirmComponent>;

  let passwordResetServiceSpy: jasmine.SpyObj<PasswordResetService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let notificationSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {

    passwordResetServiceSpy = jasmine.createSpyObj<PasswordResetService>(
      'PasswordResetService',
      ['confirmPasswordReset']
    );

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    notificationSpy = jasmine.createSpyObj<ToastrService>(
      'ToastrService',
      ['success', 'error']
    );

    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>(
      'ErrorFormatterService',
      ['format']
    );

    await TestBed.configureTestingModule({
      declarations: [ConfirmComponent],
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: PasswordResetService, useValue: passwordResetServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: notificationSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: () => 'RESET_TOKEN'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // --------------------------------------------------
  // BASIC
  // --------------------------------------------------

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --------------------------------------------------
  // INVALID FORM
  // --------------------------------------------------

  it('reset_invalidForm_doesNotCallService_andMarksFormTouched', () => {
    component.resetForm.setValue({
      password: '',
      confirmPassword: ''
    });

    component.onSubmit();

    expect(passwordResetServiceSpy.confirmPasswordReset).not.toHaveBeenCalled();
    expect(component.resetForm.touched).toBeTrue();
    expect(notificationSpy.error).not.toHaveBeenCalled();
  });

  it('reset_passwordsDoNotMatch_doesNotCallService', () => {
    component.resetForm.setValue({
      password: 'password123',
      confirmPassword: 'password456'
    });

    component.onSubmit();

    expect(passwordResetServiceSpy.confirmPasswordReset).not.toHaveBeenCalled();
    expect(component.resetForm.invalid).toBeTrue();
  });

  // --------------------------------------------------
  // SUCCESS
  // --------------------------------------------------

  it('reset_validPassword_callsService_showsSuccess_andNavigates', () => {
    passwordResetServiceSpy.confirmPasswordReset.and.returnValue(of(void 0));

    component.resetForm.setValue({
      password: 'password123',
      confirmPassword: 'password123'
    });

    component.onSubmit();

    expect(passwordResetServiceSpy.confirmPasswordReset)
    .toHaveBeenCalledWith('RESET_TOKEN', 'password123');

    expect(notificationSpy.success).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  // --------------------------------------------------
  // BACKEND ERROR
  // --------------------------------------------------

  it('reset_backendError_formatsAndShowsError', () => {
    errorFormatterSpy.format.and.returnValue('Reset failed');

    passwordResetServiceSpy.confirmPasswordReset.and.returnValue(
      throwError(() => ({ status: 409 }))
    );

    component.resetForm.setValue({
      password: 'password123',
      confirmPassword: 'password123'
    });

    component.onSubmit();

    expect(passwordResetServiceSpy.confirmPasswordReset).toHaveBeenCalled();
    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(notificationSpy.error).toHaveBeenCalledWith(
      'Reset failed',
      'Passwort zurücksetzen fehlgeschlagen'
    );
  });
});
