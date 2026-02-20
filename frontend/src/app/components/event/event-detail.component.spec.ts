import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EventDetailComponent } from './event-detail.component';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { EventDetail } from '../../dtos/event-detail';

describe('EventDetailComponent', () => {
  let component: EventDetailComponent;
  let fixture: ComponentFixture<EventDetailComponent>;
  let eventService: jasmine.SpyObj<EventService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const mockEvent: EventDetail = {
    id: 1,
    title: 'Test Concert',
    type: 'Konzert',
    durationMinutes: 120,
    description: 'Test Description',
    dateTime: '2026-06-01T20:00:00',
    location: {
      id: 1,
      zipCode: 1010,
      city: 'Wien',
      street: 'Teststraße',
      streetNumber: '1'
    },
    artists: [
      { id: 1, name: 'Test Artist' }
    ],
    ticketCount: 100,
    minPrice: 2500
  };

  beforeEach(async () => {
    const eventServiceSpy = jasmine.createSpyObj('EventService', [
      'getEventById',
      'deleteEvent',
      'getImageUrl'
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isAdmin']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const toastrSpy = jasmine.createSpyObj('ToastrService', ['error', 'success']);

    await TestBed.configureTestingModule({
      declarations: [EventDetailComponent],
      providers: [
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({ get: (key: string) => '1' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EventDetailComponent);
    component = fixture.componentInstance;
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastr = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load event on init', () => {
    eventService.getEventById.and.returnValue(of(mockEvent));

    component.ngOnInit();

    expect(eventService.getEventById).toHaveBeenCalledWith(1);
    expect(component.event).toEqual(mockEvent);
    expect(component.loading).toBeFalse();
  });

  it('should show error when event loading fails', () => {
    eventService.getEventById.and.returnValue(throwError(() => new Error('Not found')));

    component.ngOnInit();

    expect(toastr.error).toHaveBeenCalledWith(
      'Veranstaltung konnte nicht geladen werden',
      'Fehler'
    );
    expect(component.loading).toBeFalse();
  });

  it('should format date correctly', () => {
    component.event = mockEvent;

    const formattedDate = component.formatDate('2026-06-01T20:00:00');

    expect(formattedDate).toContain('Juni');
    expect(formattedDate).toContain('2026');
  });

  it('should format time correctly', () => {
    component.event = mockEvent;

    const formattedTime = component.formatTime('2026-06-01T20:00:00');

    expect(formattedTime).toBe('20:00');
  });

  it('should delete event and navigate to events list', () => {
    component.event = mockEvent;
    eventService.deleteEvent.and.returnValue(of(void 0));

    component.deleteEvent();

    expect(eventService.deleteEvent).toHaveBeenCalledWith(1);
    expect(toastr.success).toHaveBeenCalled();
  });

  it('should show error when deleting event fails', () => {
    component.event = mockEvent;
    eventService.deleteEvent.and.returnValue(
      throwError(() => ({ error: { message: 'Cannot delete event with sold tickets' } }))
    );

    component.deleteEvent();

    expect(toastr.error).toHaveBeenCalledWith(
      'Cannot delete event with sold tickets',
      'Fehler',
      { timeOut: 5000 }
    );
  });

  it('should navigate to edit page when editEvent is called', () => {
    component.event = mockEvent;

    component.editEvent();

    expect(router.navigate).toHaveBeenCalledWith(['/events', 1, 'edit']);
  });

  it('should navigate back to events list', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/events']);
  });

  it('should check admin status', () => {
    authService.isAdmin.and.returnValue(true);

    const result = component.isAdmin();

    expect(result).toBeTrue();
    expect(authService.isAdmin).toHaveBeenCalled();
  });

  it('should return placeholder image on image error', () => {
    component.event = mockEvent;
    component.imageError = false;

    const mockImgEvent = { target: { src: '' } } as any;
    component.onImageError(mockImgEvent);

    expect(component.imageError).toBeTrue();
    expect(mockImgEvent.target.src).toContain('placeholder');
  });

  it('should get correct placeholder based on event type', () => {
    component.event = { ...mockEvent, type: 'Oper' };
    component.imageError = true;

    const placeholder = component.getEventImageUrl();

    expect(placeholder).toContain('placeholder_opera.jpg');
  });
});
