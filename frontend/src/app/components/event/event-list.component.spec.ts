import {ComponentFixture, TestBed} from '@angular/core/testing';
import {EventListComponent} from './event-list.component';
import {EventService} from '../../services/event.service';
import {ArtistService} from '../../services/artist.service';
import {LocationService} from '../../services/location.service';
import {AuthService} from '../../services/auth.service';
import {Router} from '@angular/router';
import {ToastrService} from 'ngx-toastr';
import {of} from 'rxjs';

describe('EventListComponent', () => {
  let component: EventListComponent;
  let fixture: ComponentFixture<EventListComponent>;
  let eventService: jasmine.SpyObj<EventService>;

  beforeEach(async () => {
    const eventServiceSpy = jasmine.createSpyObj('EventService', [
      'getAllEvents',
      'searchEvents',
      'checkImageExists'
    ]);

    await TestBed.configureTestingModule({
      declarations: [EventListComponent],
      providers: [
        {provide: EventService, useValue: eventServiceSpy},
        {provide: ArtistService, useValue: {}},
        {provide: LocationService, useValue: {}},
        {provide: AuthService, useValue: {isAdmin: () => false}},
        {provide: Router, useValue: {events: of()}},
        {provide: ToastrService, useValue: {}}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EventListComponent);
    component = fixture.componentInstance;
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load events on init', () => {
    eventService.getAllEvents.and.returnValue(
      of({
        content: [],
        last: true,
        totalElements: 0,
        totalPages: 0,
        size: 12,
        number: 0,
        first: true,
        empty: true
      } as any)
    );

    component.ngOnInit();

    expect(eventService.getAllEvents).toHaveBeenCalled();
    expect(component.isLoading).toBeFalse();
  });

  it('should set priceError if min price is greater than max price', () => {
    component.searchPriceMin = 50;
    component.searchPriceMax = 10;

    component.validatePrices();

    expect(component.priceError).toBeTruthy();
  });

  it('should show error if zip code is not 4 digits', () => {
    component.searchLocationZip = '123';

    component.validateZip();

    expect(component.zipError).toBeTruthy();
  });

  it('should call searchEvents when searchMode is event', () => {
    spyOn(component, 'searchEvents');
    component.searchMode = 'event';

    component.onSearch();

    expect(component.searchEvents).toHaveBeenCalled();
  });

});
