import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateUserComponent } from './create-user.component';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ToastrService } from 'ngx-toastr';
import { AdminUserService } from '../../services/admin-user.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import { of, throwError } from 'rxjs';

describe('CreateUserComponent', () => {

  let component: CreateUserComponent;
  let fixture: ComponentFixture<CreateUserComponent>;

  let adminUserServiceSpy: jasmine.SpyObj<AdminUserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {
    adminUserServiceSpy = jasmine.createSpyObj<AdminUserService>(
      'AdminUserService',
      ['createUser']
    );

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', [
      'success',
      'error'
    ]);

    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>(
      'ErrorFormatterService',
      ['format']
    );

    await TestBed.configureTestingModule({
      declarations: [CreateUserComponent],
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AdminUserService, useValue: adminUserServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('submit_invalidForm_doesNotCallService', () => {
    component.createUserForm.setValue({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      userRole: null
    });

    component.onSubmit();

    expect(adminUserServiceSpy.createUser).not.toHaveBeenCalled();
  });

  it('submit_validForm_callsService_andShowsSuccess', () => {
    adminUserServiceSpy.createUser.and.returnValue(of(void 0));

    component.createUserForm.setValue({
      firstName: 'Max',
      lastName: 'Mustermann',
      email: 'max@test.com',
      password: 'password123',
      userRole: 'ROLE_USER'
    });

    component.onSubmit();

    expect(adminUserServiceSpy.createUser).toHaveBeenCalled();
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('submit_backendError_formatsAndShowsError', () => {
    errorFormatterSpy.format.and.returnValue('Backend error');

    adminUserServiceSpy.createUser.and.returnValue(
      throwError(() => ({ status: 409 }))
    );

    component.createUserForm.setValue({
      firstName: 'Max',
      lastName: 'Mustermann',
      email: 'max@test.com',
      password: 'password123',
      userRole: 'ROLE_USER'
    });

    component.onSubmit();

    expect(adminUserServiceSpy.createUser).toHaveBeenCalled();
    expect(errorFormatterSpy.format).toHaveBeenCalled();
    expect(toastrSpy.error).toHaveBeenCalled();
  });
});
