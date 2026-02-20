import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { EventCreateComponent } from './event-create.component';
import { EventService } from '../../services/event.service';
import { LocationService } from '../../services/location.service';
import { ArtistService } from '../../services/artist.service';
import { AuthService } from '../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

describe('EventCreateComponent', () => {
  let component: EventCreateComponent;
  let fixture: ComponentFixture<EventCreateComponent>;
  let eventService: jasmine.SpyObj<EventService>;
  let locationService: jasmine.SpyObj<LocationService>;
  let artistService: jasmine.SpyObj<ArtistService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const mockLocations = [
    { id: 1, name: 'Wiener Stadthalle', zipCode: 1150, city: 'Wien', street: 'Vogelweidplatz', streetNumber: '14' }
  ];

  const mockArtists = [
    { id: 1, name: 'Artist One', isBand: false },
    { id: 2, name: 'Band Two', isBand: true, memberIds: [1] }
  ];

  beforeEach(async () => {
    const eventServiceSpy = jasmine.createSpyObj('EventService', [
      'createEvent',
      'uploadImage',
      'refreshImageCache'
    ]);
    const locationServiceSpy = jasmine.createSpyObj('LocationService', ['getAllLocations']);
    const artistServiceSpy = jasmine.createSpyObj('ArtistService', ['getAllArtists']);
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getUserRole']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const toastrSpy = jasmine.createSpyObj('ToastrService', ['error', 'warning', 'success']);

    await TestBed.configureTestingModule({
      imports: [EventCreateComponent],
      providers: [
        FormBuilder,
        { provide: EventService, useValue: eventServiceSpy },
        { provide: LocationService, useValue: locationServiceSpy },
        { provide: ArtistService, useValue: artistServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EventCreateComponent);
    component = fixture.componentInstance;
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
    locationService = TestBed.inject(LocationService) as jasmine.SpyObj<LocationService>;
    artistService = TestBed.inject(ArtistService) as jasmine.SpyObj<ArtistService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastr = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;

    locationService.getAllLocations.and.returnValue(of(mockLocations));
    artistService.getAllArtists.and.returnValue(of(mockArtists));
    authService.isLoggedIn.and.returnValue(true);
    authService.getUserRole.and.returnValue('ADMIN');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load locations and artists on init', () => {
    component.ngOnInit();

    expect(locationService.getAllLocations).toHaveBeenCalled();
    expect(artistService.getAllArtists).toHaveBeenCalled();
    expect(component.locations).toEqual(mockLocations);
    expect(component.artists).toEqual(mockArtists);
  });

  it('should initialize form with validators', () => {
    expect(component.eventForm).toBeDefined();
    expect(component.eventForm.get('title')?.hasError('required')).toBeTrue();
    expect(component.eventForm.get('type')?.hasError('required')).toBeTrue();
    expect(component.eventForm.get('locationId')?.hasError('required')).toBeTrue();
  });

  it('should not submit invalid form', () => {
    component.onSubmit();

    expect(toastr.warning).toHaveBeenCalledWith(
      'Bitte füllen Sie alle Pflichtfelder korrekt aus',
      'Validierung'
    );
    expect(eventService.createEvent).not.toHaveBeenCalled();
  });

  it('should create event successfully', (done) => {
    const createdEvent = { id: 1, title: 'Test Event' };
    eventService.createEvent.and.returnValue(of(createdEvent as any));

    component.eventForm.patchValue({
      title: 'Test Event',
      type: 'Konzert',
      durationMinutes: 120,
      dateTime: '2026-06-01T20:00',
      locationId: 1
    });

    component.onSubmit();

    setTimeout(() => {
      expect(eventService.createEvent).toHaveBeenCalled();
      expect(toastr.success).toHaveBeenCalled();
      done();
    }, 1100);
  });

  it('should upload image after creating event', (done) => {
    const createdEvent = { id: 1, title: 'Test Event' };
    const mockFile = new File([''], 'test.jpg', { type: 'image/jpeg' });

    eventService.createEvent.and.returnValue(of(createdEvent as any));
    eventService.uploadImage.and.returnValue(of(void 0));

    component.eventForm.patchValue({
      title: 'Test Event',
      type: 'Konzert',
      durationMinutes: 120,
      dateTime: '2026-06-01T20:00',
      locationId: 1
    });
    component.selectedImageFile = mockFile;

    component.onSubmit();

    setTimeout(() => {
      expect(eventService.createEvent).toHaveBeenCalled();
      expect(eventService.uploadImage).toHaveBeenCalledWith(1, mockFile);
      done();
    }, 1100);
  });

  it('should show error when event creation fails', (done) => {
    eventService.createEvent.and.returnValue(
      throwError(() => ({ error: { message: 'Validation error' } }))
    );

    component.eventForm.patchValue({
      title: 'Test Event',
      type: 'Konzert',
      durationMinutes: 120,
      dateTime: '2026-06-01T20:00',
      locationId: 1
    });

    component.onSubmit();

    setTimeout(() => {
      expect(toastr.error).toHaveBeenCalledWith(
        'Validation error',
        'Fehler',
        { timeOut: 5000 }
      );
      done();
    }, 10);
  });

  it('should filter artists on search', () => {
    component.artists = mockArtists;
    component.artistSearchTerm = 'Artist';

    component.onArtistSearchChange();

    expect(component.filteredArtists.length).toBe(1);
    expect(component.filteredArtists[0].name).toBe('Artist One');
    expect(component.showArtistDropdown).toBeTrue();
  });

  it('should select artist from dropdown', () => {
    const artist = mockArtists[0];

    component.selectArtistFromDropdown(artist);

    expect(component.selectedArtists).toContain(artist);
    expect(component.artistSearchTerm).toBe('');
    expect(component.showArtistDropdown).toBeFalse();
  });

  it('should remove selected artist', () => {
    component.selectedArtists = [mockArtists[0]];

    component.removeArtist(mockArtists[0]);

    expect(component.selectedArtists.length).toBe(0);
  });

  it('should validate image file size', () => {
    const largeFile = new File(['x'.repeat(4 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [largeFile] } } as any;

    component.onImageSelected(event);

    expect(component.imageValidationError).toBe('Bild darf maximal 3 MB groß sein');
    expect(component.selectedImageFile).toBeNull();
  });

  it('should validate image file type', () => {
    const invalidFile = new File([''], 'test.pdf', { type: 'application/pdf' });
    const event = { target: { files: [invalidFile] } } as any;

    component.onImageSelected(event);

    expect(component.imageValidationError).toBe('Nur JPG, PNG und WebP Bilder sind erlaubt');
    expect(component.selectedImageFile).toBeNull();
  });

  it('should accept valid image file', () => {
    const validFile = new File([''], 'test.jpg', { type: 'image/jpeg' });
    Object.defineProperty(validFile, 'size', { value: 1024 });
    const event = { target: { files: [validFile] } } as any;

    component.onImageSelected(event);

    expect(component.selectedImageFile).toBe(validFile);
    expect(component.imageValidationError).toBeNull();
  });

  it('should navigate back to events list', () => {
    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/events']);
  });

  it('should only allow numbers in duration field', () => {
    const event = {
      which: 65,
      keyCode: 65,
      preventDefault: jasmine.createSpy('preventDefault')
    } as any;

    const result = component.onlyNumbers(event);

    expect(result).toBeFalse();
    expect(event.preventDefault).toHaveBeenCalled();
  });

  it('should allow numbers in duration field', () => {
    const event = {
      which: 53,
      keyCode: 53
    } as any;

    const result = component.onlyNumbers(event);

    expect(result).toBeTrue();
  });
});
