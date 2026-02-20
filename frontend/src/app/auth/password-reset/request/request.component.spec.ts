import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RequestComponent } from './request.component';

import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';

import { PasswordResetService } from '../../../services/password-reset.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';

describe('RequestPasswordResetComponent', () => {

  let component: RequestComponent;
  let fixture: ComponentFixture<RequestComponent>;

  let passwordResetServiceSpy: jasmine.SpyObj<PasswordResetService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {

    passwordResetServiceSpy = jasmine.createSpyObj(
      'PasswordResetService',
      ['requestPasswordReset']
    );

    toastrSpy = jasmine.createSpyObj(
      'ToastrService',
      ['success', 'error']
    );

    errorFormatterSpy = jasmine.createSpyObj(
      'ErrorFormatterService',
      ['format']
    );

    await TestBed.configureTestingModule({
      declarations: [RequestComponent],
      imports: [
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: PasswordResetService, useValue: passwordResetServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // -----------------------------------------
  // BASIC
  // -----------------------------------------

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // -----------------------------------------
  // FORM VALIDATION
  // -----------------------------------------

  it('request_invalidForm_doesNotCallService', () => {
    component.resetForm.setValue({ email: '' });

    component.requestReset();

    expect(passwordResetServiceSpy.requestPasswordReset).not.toHaveBeenCalled();
  });

  // -----------------------------------------
  // SUCCESS
  // -----------------------------------------

  it('request_validEmail_callsService_andShowsSuccess', () => {
    passwordResetServiceSpy.requestPasswordReset.and.returnValue(of(void 0));

    component.resetForm.setValue({ email: 'test@test.com' });

    component.requestReset();

    expect(passwordResetServiceSpy.requestPasswordReset)
    .toHaveBeenCalledWith('test@test.com');

    expect(component.success).toBeTrue();
    expect(toastrSpy.success).toHaveBeenCalled();
  });
});
