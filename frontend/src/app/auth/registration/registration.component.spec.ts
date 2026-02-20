import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegistrationComponent } from './registration.component';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, throwError } from 'rxjs';

import { UserService } from '../../services/user.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import {AuthService} from "../../services/auth.service";

describe('RegistrationComponent', () => {

  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;

  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let notificationSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;
  let authServiceMock: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', [
      'registerUser'
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

    authServiceMock = jasmine.createSpyObj<AuthService>('AuthService', [
      'loginWithToken',
      'isLoggedIn',
      'isAdmin'
    ]);

    authServiceMock.isLoggedIn.and.returnValue(false);
    authServiceMock.isAdmin.and.returnValue(false);
    authServiceMock.loginWithToken.and.stub();

    await TestBed.configureTestingModule({
      declarations: [RegistrationComponent],
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: notificationSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy },
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
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

  it('register_invalidForm_doesNotCallService', () => {
    component.registerForm.setValue({
      firstName: '',
      lastName: '',
      email: '',
      password: ''
    });

    component.onSubmit();

    expect(userServiceSpy.registerUser).not.toHaveBeenCalled();
  });

  // --------------------------------------------------
  // SUCCESS
  // --------------------------------------------------

  it('register_validUser_callsService_showsSuccess_andNavigates', () => {
    userServiceSpy.registerUser.and.returnValue(of({
      token: 'a.b.c',
      user: {
        id: 1,
        email: 'test@example.com',
        userRole: 'ROLE_USER'
      }
    }));

    component.registerForm.setValue({
      firstName: 'Max',
      lastName: 'Mustermann',
      email: 'test@example.com',
      password: 'password123'
    });

    component.onSubmit();

    expect(userServiceSpy.registerUser).toHaveBeenCalled();
    expect(notificationSpy.success).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['']);
    expect(authServiceMock.loginWithToken).toHaveBeenCalledWith('a.b.c');
  });

  // --------------------------------------------------
  // BACKEND ERROR
  // --------------------------------------------------

  it('register_backendError_formatsAndShowsError', () => {
    errorFormatterSpy.format.and.returnValue('Backend error');

    userServiceSpy.registerUser.and.returnValue(
      throwError(() => ({ status: 409 }))
    );

    component.registerForm.setValue({
      firstName: 'Max',
      lastName: 'Mustermann',
      email: 'existing@test.com',
      password: 'password123'
    });

    component.onSubmit();

    expect(userServiceSpy.registerUser).toHaveBeenCalled();
    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(notificationSpy.error).toHaveBeenCalled();
  });
});
