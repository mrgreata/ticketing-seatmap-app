import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import { ArtistService } from './artist.service';
import { environment } from '../../environments/environment';

describe('ArtistService', () => {
  let service: ArtistService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ArtistService]
    });
    service = TestBed.inject(ArtistService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllArtists_sendsCorrectRequest', () => {
    service.getAllArtists().subscribe();

    const req = httpMock.expectOne(`${environment.backendUrl}/artists`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getArtistById_sendsCorrectRequest', () => {
    service.getArtistById(1).subscribe();

    const req = httpMock.expectOne(`${environment.backendUrl}/artists/1`);
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });

  it('createArtist_sendsCorrectRequest', () => {
    const newArtist = {
      name: 'New Artist',
      isBand: false
    };

    service.createArtist(newArtist).subscribe();

    const req = httpMock.expectOne(`${environment.backendUrl}/artists`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newArtist);
    req.flush(null);
  });

  it('searchArtists_sendsCorrectRequest', () => {
    service.searchArtists('Beatles', true).subscribe();

    const req = httpMock.expectOne(request =>
      request.url === `${environment.backendUrl}/artists/search` &&
      request.params.get('name') === 'Beatles' &&
      request.params.get('includeBands') === 'true'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getEventsByArtist_sendsCorrectRequest', () => {
    service.getEventsByArtist(1).subscribe();

    const req = httpMock.expectOne(`${environment.backendUrl}/artists/1/events`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
