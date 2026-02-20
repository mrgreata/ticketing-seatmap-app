import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { TicketService } from './ticket.service';
import { Globals } from '../global/globals';
import {
  TicketPurchased,
  TicketReserved,
  SimpleReservation,
  TicketCancelled
} from '../dtos/ticket';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;
  let globals: Globals;

  const mockBaseUri = 'http://localhost:8080/api';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TicketService,
        {
          provide: Globals,
          useValue: { backendUri: mockBaseUri }
        }
      ]
    });

    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
    globals = TestBed.inject(Globals);
  });

  afterEach(() => {
    httpMock.verify(); // stellt sicher, dass keine offenen Requests bleiben
  });

  // --------------------------------------------------
  // Purchase / Create
  // --------------------------------------------------

  it('should purchase tickets (PATCH /tickets/purchasing)', () => {
    const ids = [1, 2, 3];
    const mockResponse: TicketPurchased[] = [
      { id: 1 } as TicketPurchased,
      { id: 2 } as TicketPurchased
    ];

    service.purchaseTickets(ids).subscribe(res => {
      expect(res.length).toBe(2);
      expect(res[0].id).toBe(1);
    });

    const req = httpMock.expectOne(`${mockBaseUri}/tickets/purchasing`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(ids);

    req.flush(mockResponse);
  });

  it('should create tickets (POST /tickets)', () => {
    const tickets = [{ eventId: 1, seatId: 10 }];

    service.createTicket(tickets).subscribe();

    const req = httpMock.expectOne(`${mockBaseUri}/tickets`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(tickets);

    req.flush([]);
  });

  // --------------------------------------------------
  // Reservations
  // --------------------------------------------------

  it('should reserve tickets (PATCH /reservations)', () => {
    const ids = [5, 6];

    service.reserveTickets(ids).subscribe();

    const req = httpMock.expectOne(`${mockBaseUri}/reservations`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(ids);

    req.flush({});
  });

  it('should get my reservations (GET /reservations/my)', () => {
    const mockReservations: TicketReserved[] = [
      { ticketId: 1, eventName: 'Test Event' } as TicketReserved
    ];

    service.getMyReservations().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].ticketId).toBe(1);
    });

    const req = httpMock.expectOne(`${mockBaseUri}/reservations/my`);
    expect(req.request.method).toBe('GET');

    req.flush(mockReservations);
  });

  it('should get reservation by id (GET /reservations/:id)', () => {
    const mockReservation: SimpleReservation = {
      userId: 10,
      ticketIds: [1, 2]
    };

    service.getReservationById(99).subscribe(res => {
      expect(res.userId).toBe(10);
      expect(res.ticketIds.length).toBe(2);
    });

    const req = httpMock.expectOne(`${mockBaseUri}/reservations/99`);
    expect(req.request.method).toBe('GET');

    req.flush(mockReservation);
  });

  it('should cancel reservation (PATCH /reservations/cancellation)', () => {
    const ids = [7, 8];

    service.cancelReservation(ids).subscribe();

    const req = httpMock.expectOne(`${mockBaseUri}/reservations/cancellation`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(ids);

    req.flush({});
  });

  // --------------------------------------------------
  // Tickets
  // --------------------------------------------------

  it('should get my tickets (GET /tickets/my)', () => {
    const mockTickets: TicketPurchased[] = [
      { id: 1, eventName: 'Coldplay' } as TicketPurchased
    ];

    service.getMyTickets().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].eventName).toBe('Coldplay');
    });

    const req = httpMock.expectOne(`${mockBaseUri}/tickets/my`);
    expect(req.request.method).toBe('GET');

    req.flush(mockTickets);
  });

  it('should get my cancelled tickets (GET /tickets/cancelled/my)', () => {
    const mockCancelled: TicketCancelled[] = [
      { id: 1, eventName: 'Test Event' } as TicketCancelled
    ];

    service.getMyCancelledTickets().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].id).toBe(1);
    });

    const req = httpMock.expectOne(`${mockBaseUri}/tickets/cancelled/my`);
    expect(req.request.method).toBe('GET');

    req.flush(mockCancelled);
  });

  it('should cancel tickets (DELETE /tickets)', () => {
    const ids = [3, 4];

    service.cancelTickets(ids).subscribe();

    const req = httpMock.expectOne(`${mockBaseUri}/tickets`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual(ids);

    req.flush({});
  });
});
