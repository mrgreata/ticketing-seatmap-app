import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn',
      'isAdmin',
      'logoutUser'
    ]);
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate'], {
      events: of(),
      url: '/'
    });

    await TestBed.configureTestingModule({
      declarations: [HeaderComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
