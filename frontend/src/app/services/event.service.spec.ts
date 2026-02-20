import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EventService } from './event.service';
import { Globals } from '../global/globals';
import { Event } from '../dtos/event';
import { EventDetail } from '../dtos/event-detail';
import { TopTenEvent } from '../dtos/top-ten-event';
import { Page } from '../dtos/page';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;
  let globals: Globals;

  const mockEvent: Event = {
    id: 1,
    title: 'Test Concert',
    type: 'Konzert',
    durationMinutes: 120,
    dateTime: '2026-06-01T20:00:00',
    locationName: 'Wiener Stadthalle',
    locationCity: 'Wien',
    minPrice: 2500,
    description: 'Test Description'
  };

  const mockEventDetail: EventDetail = {
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
    artists: [{ id: 1, name: 'Test Artist' }],
    ticketCount: 100,
    minPrice: 2500
  };

  const mockPage: Page<Event> = {
    content: [mockEvent],
    totalElements: 1,
    totalPages: 1,
    size: 12,
    number: 0,
    first: true,
    last: true,
    empty: false
  };

  const mockTopTenEvents: TopTenEvent[] = [
    { eventId: 1, title: 'Event 1', type: 'Konzert', ticketsSold: 500 },
    { eventId: 2, title: 'Event 2', type: 'Oper', ticketsSold: 400 }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        EventService,
        {
          provide: Globals,
          useValue: { backendUri: 'http://localhost:8080/api/v1' }
        }
      ]
    });

    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
    globals = TestBed.inject(Globals);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all events with pagination', () => {
    service.getAllEvents(0, 12).subscribe(data => {
      expect(data).toEqual(mockPage);
      expect(data.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events?page=0&size=12`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should search events with all parameters', () => {
    service.searchEvents(
      'Concert',
      'Konzert',
      120,
      '2026-06-01T00:00:00',
      '2026-06-30T23:59:59',
      1,
      1000,
      5000,
      0,
      12
    ).subscribe(data => {
      expect(data).toEqual(mockPage);
    });

    const req = httpMock.expectOne(request =>
      request.url === `${globals.backendUri}/events/search` &&
      request.params.get('title') === 'Concert' &&
      request.params.get('type') === 'Konzert' &&
      request.params.get('duration') === '120' &&
      request.params.get('locationId') === '1' &&
      request.params.get('priceMin') === '1000' &&
      request.params.get('priceMax') === '5000'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should search events with only title parameter', () => {
    service.searchEvents('Concert', undefined, undefined, undefined, undefined, undefined, undefined, undefined, 0, 12)
      .subscribe(data => {
        expect(data).toEqual(mockPage);
      });

    const req = httpMock.expectOne(request =>
      request.url === `${globals.backendUri}/events/search` &&
      request.params.get('title') === 'Concert' &&
      !request.params.has('type')
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should get event by id', () => {
    service.getEventById(1).subscribe(data => {
      expect(data).toEqual(mockEventDetail);
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockEventDetail);
  });

  it('should get top ten events', () => {
    service.getTopTen(6, 2026, 'Konzert').subscribe(data => {
      expect(data).toEqual(mockTopTenEvents);
      expect(data.length).toBe(2);
    });

    const req = httpMock.expectOne(request =>
      request.url === `${globals.backendUri}/events/top-ten` &&
      request.params.get('month') === '6' &&
      request.params.get('year') === '2026' &&
      request.params.get('type') === 'Konzert'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockTopTenEvents);
  });

  it('should get top ten events without type filter', () => {
    service.getTopTen(6, 2026).subscribe(data => {
      expect(data).toEqual(mockTopTenEvents);
    });

    const req = httpMock.expectOne(request =>
      request.url === `${globals.backendUri}/events/top-ten` &&
      !request.params.has('type')
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockTopTenEvents);
  });

  it('should create event', () => {
    const createPayload = {
      title: 'New Event',
      type: 'Konzert',
      dateTime: '2026-07-01T20:00:00',
      locationId: 1,
      artistIds: [1, 2]
    };

    const createdEvent: EventDetail = {
      id: 1,
      title: 'New Event',
      type: 'Konzert',
      durationMinutes: 120,
      description: 'Test',
      dateTime: '2026-07-01T20:00:00',
      location: {
        id: 1,
        zipCode: 1010,
        city: 'Wien',
        street: 'Teststraße',
        streetNumber: '1'
      },
      artists: [{ id: 1, name: 'Artist 1' }],
      ticketCount: 0,
      minPrice: 0
    };

    service.createEvent(createPayload).subscribe((data: any) => {
      expect(data.id).toBe(1);
      expect(data.title).toBe('New Event');
      expect(data.location.city).toBe('Wien');
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createPayload);
    req.flush(createdEvent);
  });

  it('should update event', () => {
    const updatePayload = {
      id: 1,
      title: 'Updated Event',
      type: 'Oper',
      dateTime: '2026-07-01T20:00:00',
      locationId: 1,
      artistIds: [1]
    };

    const updatedEvent: EventDetail = {
      id: 1,
      title: 'Updated Event',
      type: 'Oper',
      durationMinutes: 120,
      description: 'Updated',
      dateTime: '2026-07-01T20:00:00',
      location: {
        id: 1,
        zipCode: 1010,
        city: 'Wien',
        street: 'Teststraße',
        streetNumber: '1'
      },
      artists: [{ id: 1, name: 'Artist 1' }],
      ticketCount: 0,
      minPrice: 0
    };

    service.updateEvent(1, updatePayload).subscribe((data: any) => {
      expect(data.id).toBe(1);
      expect(data.title).toBe('Updated Event');
      expect(data.type).toBe('Oper');
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatePayload);
    req.flush(updatedEvent);
  });

  it('should delete event', () => {
    service.deleteEvent(1).subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });


  it('should upload image', () => {
    const mockFile = new File(['image content'], 'test.jpg', { type: 'image/jpeg' });

    service.uploadImage(1, mockFile).subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/1/image`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(null);
  });

  it('should generate correct image URL', () => {
    const url = service.getImageUrl(1, false);

    expect(url).toContain(`${globals.backendUri}/events/1/image`);
    expect(url).toContain('t=');
  });

  it('should generate image URL with cache busting', (done) => {
    const url1 = service.getImageUrl(1, false);

    setTimeout(() => {
      const url2 = service.getImageUrl(1, true);

      expect(url1).not.toEqual(url2);
      done();
    }, 1);
  });


  it('should refresh image cache', (done) => {
    const urlBefore = service.getImageUrl(1);

    setTimeout(() => {
      service.refreshImageCache();

      const urlAfter = service.getImageUrl(1);
      expect(urlBefore).not.toEqual(urlAfter);
      done();
    }, 1);
  });

  it('should check if image exists', () => {
    service.checkImageExists(1).subscribe(exists => {
      expect(exists).toBeTrue();
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/1/image`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(), { status: 200, statusText: 'OK' });
  });

  it('should return false when image does not exist', () => {
    service.checkImageExists(999).subscribe(exists => {
      expect(exists).toBeFalse();
    });

    const req = httpMock.expectOne(`${globals.backendUri}/events/999/image`);
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });
});
