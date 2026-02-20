import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { EventEditComponent } from './event-edit.component';
import { EventService } from '../../services/event.service';
import { LocationService } from '../../services/location.service';
import { ArtistService } from '../../services/artist.service';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { EventDetail } from '../../dtos/event-detail';

describe('EventEditComponent', () => {
  let component: EventEditComponent;
  let fixture: ComponentFixture<EventEditComponent>;
  let eventService: jasmine.SpyObj<EventService>;
  let locationService: jasmine.SpyObj<LocationService>;
  let artistService: jasmine.SpyObj<ArtistService>;
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

  const mockLocations = [
    { id: 1, name: 'Wiener Stadthalle', zipCode: 1150, city: 'Wien', street: 'Vogelweidplatz', streetNumber: '14' }
  ];

  const mockArtists = [
    { id: 1, name: 'Artist One', isBand: false },
    { id: 2, name: 'Band Two', isBand: true, memberIds: [1] }
  ];

  beforeEach(async () => {
    const eventServiceSpy = jasmine.createSpyObj('EventService', [
      'getEventById',
      'updateEvent',
      'uploadImage',
      'refreshImageCache',
      'getImageUrl',
      'checkImageExists',
      'deleteImage'
    ]);
    const locationServiceSpy = jasmine.createSpyObj('LocationService', ['getAllLocations']);
    const artistServiceSpy = jasmine.createSpyObj('ArtistService', ['getAllArtists']);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getUserRole']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const toastrSpy = jasmine.createSpyObj('ToastrService', ['error', 'warning', 'success']);

    await TestBed.configureTestingModule({
      imports: [EventEditComponent],
      providers: [
        FormBuilder,
        { provide: EventService, useValue: eventServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: ArtistService, useValue: artistServiceSpy },
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

    fixture = TestBed.createComponent(EventEditComponent);
    component = fixture.componentInstance;
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
    locationService = TestBed.inject(LocationService) as jasmine.SpyObj<LocationService>;
    artistService = TestBed.inject(ArtistService) as jasmine.SpyObj<ArtistService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastr = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;

    eventService.getEventById.and.returnValue(of(mockEvent));
    eventService.getImageUrl.and.returnValue('/api/events/1/image');
    eventService.checkImageExists.and.returnValue(of(true));
    eventService.deleteImage.and.returnValue(of(void 0));
    locationService.getAllLocations.and.returnValue(of(mockLocations));
    artistService.getAllArtists.and.returnValue(of(mockArtists));
    authService.isLoggedIn.and.returnValue(true);
    authService.getUserRole.and.returnValue('ADMIN');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load event data on init', () => {
    component.ngOnInit();

    expect(eventService.getEventById).toHaveBeenCalledWith(1);
    expect(component.eventId).toBe(1);
    expect(component.eventForm.get('title')?.value).toBe('Test Concert');
    expect(component.loadingEvent).toBeFalse();
  });

  it('should show error when event loading fails', () => {
    eventService.getEventById.and.returnValue(throwError(() => new Error('Not found')));

    component.ngOnInit();

    expect(toastr.error).toHaveBeenCalledWith(
      'Veranstaltung konnte nicht geladen werden',
      'Fehler'
    );
    expect(component.loadingEvent).toBeFalse();
  });

  it('should populate form with event data', () => {
    component.ngOnInit();

    expect(component.eventForm.get('id')?.value).toBe(1);
    expect(component.eventForm.get('title')?.value).toBe('Test Concert');
    expect(component.eventForm.get('type')?.value).toBe('Konzert');
    expect(component.eventForm.get('durationMinutes')?.value).toBe(120);
    expect(component.eventForm.get('locationId')?.value).toBe(1);
    expect(component.selectedArtists.length).toBe(1);
  });

  it('should not submit invalid form', () => {
    component.ngOnInit();
    component.eventForm.patchValue({ title: '' });

    component.onSubmit();

    expect(toastr.warning).toHaveBeenCalledWith(
      'Bitte füllen Sie alle Pflichtfelder korrekt aus',
      'Validierung'
    );
    expect(eventService.updateEvent).not.toHaveBeenCalled();
  });

  it('should update event successfully', () => {
    const updatedEvent = { ...mockEvent, title: 'Updated Event' };
    eventService.updateEvent.and.returnValue(of(updatedEvent as any));

    component.ngOnInit();
    component.eventForm.patchValue({ title: 'Updated Event' });

    component.onSubmit();

    expect(eventService.updateEvent).toHaveBeenCalled();
    expect(toastr.success).toHaveBeenCalled();
  });

  it('should upload new image when updating event', () => {
    const updatedEvent = { ...mockEvent, title: 'Updated Event' };
    const mockFile = new File([''], 'new-image.jpg', { type: 'image/jpeg' });

    eventService.updateEvent.and.returnValue(of(updatedEvent as any));
    eventService.uploadImage.and.returnValue(of(void 0));

    component.ngOnInit();
    component.selectedImageFile = mockFile;

    component.onSubmit();

    expect(eventService.updateEvent).toHaveBeenCalled();
  });

  it('should show error when update fails', () => {
    eventService.updateEvent.and.returnValue(
      throwError(() => ({ error: { message: 'Cannot update event with sold tickets' } }))
    );

    component.ngOnInit();

    component.onSubmit();

    expect(toastr.error).toHaveBeenCalledWith(
      'Cannot update event with sold tickets',
      'Fehler',
      { timeOut: 5000 }
    );
  });

  it('should filter artists on search', () => {
    component.ngOnInit();

    component.artists = mockArtists;
    component.selectedArtists = [mockArtists[0]];

    component.artistSearchTerm = 'Band';

    component.onArtistSearchChange();

    expect(component.filteredArtists.length).toBe(1);
    expect(component.filteredArtists[0].name).toBe('Band Two');
    expect(component.showArtistDropdown).toBeTrue();
  });


  it('should add artist to selected list', () => {
    component.ngOnInit();
    const newArtist = mockArtists[1];

    component.selectArtistFromDropdown(newArtist);

    expect(component.selectedArtists.length).toBe(2);
    expect(component.selectedArtists).toContain(newArtist);
  });

  it('should remove artist from selected list', () => {
    component.ngOnInit();
    const artistToRemove = component.selectedArtists[0];

    component.removeArtist(artistToRemove);

    expect(component.selectedArtists.length).toBe(0);
  });

  it('should handle image selection', () => {
    const validFile = new File([''], 'test.jpg', { type: 'image/jpeg' });
    Object.defineProperty(validFile, 'size', { value: 1024 });
    const event = { target: { files: [validFile] } } as any;

    component.onImageSelected(event);

    expect(component.selectedImageFile).toBe(validFile);
  });

  it('should remove new image selection', () => {
    component.selectedImageFile = new File([''], 'test.jpg');
    component.previewImageUrl = 'blob:test';

    component.removeNewImage();

    expect(component.selectedImageFile).toBeNull();
    expect(component.previewImageUrl).toBeNull();
  });

  it('should navigate back to event detail', () => {
    component.eventId = 1;

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/events', 1]);
  });

  it('should navigate to events list if no eventId', () => {
    component.eventId = null;

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/events']);
  });

  it('should format datetime correctly for form', () => {
    component.ngOnInit();

    const dateTimeValue = component.eventForm.get('dateTime')?.value;
    expect(dateTimeValue).toContain('2026-06-01');
    expect(dateTimeValue).toContain('20:00');
  });
});
