import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthInterceptor } from './auth-interceptor';
import { AuthService } from '../services/auth.service';
import { Globals } from '../global/globals';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  const authServiceMock = {
    getToken: () => 'TEST_TOKEN'
  };

  const globalsMock = {
    backendUri: 'http://localhost:8080/api/v1'
  } as Globals;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Globals, useValue: globalsMock }, // ✅ WICHTIG
        {
          provide: HTTP_INTERCEPTORS,
          useClass: AuthInterceptor,
          multi: true
        }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should add Authorization header for normal API requests', () => {
    http.get('http://localhost:8080/api/v1/tickets/my').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/tickets/my');
    expect(req.request.headers.get('Authorization')).toBe('Bearer TEST_TOKEN');
    req.flush([]);
  });

  it('should NOT add Authorization header for login endpoint', () => {
    http.post('http://localhost:8080/api/v1/authentication', { email: 'a', password: 'b' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/authentication');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ token: 'x' });
  });
});
