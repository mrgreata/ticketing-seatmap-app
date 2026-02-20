import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';

describe('LoginComponent', () => {

  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let notificationSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'loginUser'
    ]);

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    notificationSpy = jasmine.createSpyObj<ToastrService>('ToastrService', [
      'success',
      'error'
    ]);

    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>(
      'ErrorFormatterService',
      ['format']
    );

    await TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: notificationSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
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

  it('login_invalidForm_showsError_andDoesNotCallService', () => {
    component.loginForm.setValue({
      email: '',
      password: ''
    });

    component.loginUser();

    expect(authServiceSpy.loginUser).not.toHaveBeenCalled();
    expect(component.loginForm.touched).toBeTrue();
    expect(notificationSpy.error).not.toHaveBeenCalled();
  });

  // --------------------------------------------------
  // SUCCESS
  // --------------------------------------------------

  it('login_validCredentials_callsService_showsSuccess_andNavigates', () => {
    authServiceSpy.loginUser.and.returnValue(of('JWT_TOKEN'));

    component.loginForm.setValue({
      email: 'test@example.com',
      password: 'password123'
    });

    component.loginUser();

    expect(authServiceSpy.loginUser).toHaveBeenCalled();
    expect(notificationSpy.success).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['']);
  });

  // --------------------------------------------------
  // BACKEND ERROR
  // --------------------------------------------------

  it('login_backendError_formatsAndShowsError', () => {
    errorFormatterSpy.format.and.returnValue('Backend error');

    authServiceSpy.loginUser.and.returnValue(
      throwError(() => ({ status: 401 }))
    );

    component.loginForm.setValue({
      email: 'test@example.com',
      password: 'wrongPassword'
    });

    component.loginUser();

    expect(authServiceSpy.loginUser).toHaveBeenCalled();
    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(notificationSpy.error).toHaveBeenCalled();
  });
});
