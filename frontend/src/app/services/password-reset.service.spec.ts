import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';

import { PasswordResetService } from './password-reset.service';
import { Globals } from '../global/globals';

describe('PasswordResetService', () => {

  let service: PasswordResetService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PasswordResetService,
        {
          provide: Globals,
          useValue: {
            backendUri: 'http://localhost:8080/api/v1'
          }
        }
      ]
    });

    service = TestBed.inject(PasswordResetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ----------------------------------------
  // BASIC
  // ----------------------------------------

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ----------------------------------------
  // REQUEST RESET
  // ----------------------------------------

  it('requestPasswordReset_sendsCorrectRequest', () => {
    service.requestPasswordReset('test@test.com').subscribe();

    const req = httpMock.expectOne(
      'http://localhost:8080/api/v1/users/password-reset/request'
    );

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'test@test.com'
    });

    req.flush(null);
  });

  // ----------------------------------------
  // CONFIRM RESET
  // ----------------------------------------

  it('confirmPasswordReset_sendsCorrectRequest', () => {
    service.confirmPasswordReset('TOKEN123', 'newPassword').subscribe();

    const req = httpMock.expectOne(
      'http://localhost:8080/api/v1/users/password-reset/confirmation'
    );

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      token: 'TOKEN123',
      newPassword: 'newPassword'
    });

    req.flush(null);
  });
});
