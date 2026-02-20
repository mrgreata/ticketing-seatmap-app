import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { LocationService } from './location.service';
import { Globals } from '../global/globals';

describe('LocationService', () => {
  let service: LocationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LocationService,
        {
          provide: Globals,
          useValue: {
            backendUri: 'http://localhost:8080/api/v1'
          }
        }
      ]
    });
    service = TestBed.inject(LocationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllLocations_sendsCorrectRequest', () => {
    service.getAllLocations().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/locations');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('searchLocations_sendsCorrectRequest', () => {
    service.searchLocations('Stadthalle', 'Vogelweidplatz', 'Wien', 1150).subscribe();

    const req = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/v1/locations/search' &&
      request.params.get('name') === 'Stadthalle' &&
      request.params.get('street') === 'Vogelweidplatz' &&
      request.params.get('city') === 'Wien' &&
      request.params.get('zipCode') === '1150'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getLocationById_sendsCorrectRequest', () => {
    service.getLocationById(1).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/locations/1');
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('getEventsByLocation_sendsCorrectRequest', () => {
    service.getEventsByLocation(1).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/locations/1/events');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
