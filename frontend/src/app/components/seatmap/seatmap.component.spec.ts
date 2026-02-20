import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SeatmapComponent } from './seatmap.component';
import { SeatmapService } from '../../services/seatmap.service';
import { TicketService } from '../../services/ticket.service';

describe('SeatmapComponent', () => {
  let component: SeatmapComponent;
  let fixture: ComponentFixture<SeatmapComponent>;

  let seatmapServiceMock: jasmine.SpyObj<SeatmapService>;
  let ticketServiceMock: jasmine.SpyObj<TicketService>;
  let routerMock: jasmine.SpyObj<Router>;

  const apiSeatmapMock: any = {
    stagePosition: 'CENTER',
    stageLabel: 'BÜHNE',
    stageRowStart: 9,
    stageRowEnd: 17,
    stageColStart: 11,
    stageColEnd: 13,
    stageHeightPx: 160,
    stageWidthPx: 220,
    runwayWidthPx: null,
    runwayLengthPx: null,
    runwayOffsetPx: null,
    seats: [
      { id: 101, rowNumber: 1, seatNumber: 1, status: 'FREE', priceCategory: 'STANDARD' },
      { id: 102, rowNumber: 1, seatNumber: 2, status: 'FREE', priceCategory: 'STANDARD' },
      { id: 103, rowNumber: 2, seatNumber: 1, status: 'SOLD', priceCategory: 'PREMIUM' }
    ]
  };

  beforeEach(async () => {
    seatmapServiceMock = jasmine.createSpyObj('SeatmapService', ['getSeatmap']);
    ticketServiceMock = jasmine.createSpyObj('TicketService', ['createTicket']);
    routerMock = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        SeatmapComponent,
        HttpClientTestingModule
      ],
      providers: [
        { provide: SeatmapService, useValue: seatmapServiceMock },
        { provide: TicketService, useValue: ticketServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SeatmapComponent);
    component = fixture.componentInstance;
  });

  it('should load seatmap on init using eventId from history.state', () => {
    spyOnProperty(history, 'state', 'get').and.returnValue({ eventId: 12 });

    seatmapServiceMock.getSeatmap.and.returnValue(of(apiSeatmapMock));

    fixture.detectChanges();

    expect(seatmapServiceMock.getSeatmap).toHaveBeenCalledWith(12);
    expect(component.eventIdForTickets).toBe(12);
    expect(component.isLoading).toBeFalse();
    expect(component.rows.length).toBeGreaterThan(0);
  });

  it('toggleSeat should select and deselect a free seat', () => {
    component.rows = [
      [{ id: 101, row: 1, number: 1, status: 'free', priceCategory: 'middle' }]
    ];

    const seat = component.rows[0][0]!;
    component.toggleSeat(seat);

    expect(component.selectedSeats.has('1-1')).toBeTrue();
    expect(seat.status).toBe('selected');

    component.toggleSeat(seat);

    expect(component.selectedSeats.has('1-1')).toBeFalse();
    expect(seat.status).toBe('free');
  });

  it('toggleSeat should NOT select sold seats', () => {
    const soldSeat = {
      id: 103,
      row: 2,
      number: 1,
      status: 'sold' as const,
      priceCategory: 'expensive' as const
    };

    component.toggleSeat(soldSeat);

    expect(component.selectedSeats.size).toBe(0);
    expect(soldSeat.status).toBe('sold');
  });

  it('onContinue should call createTicket with correct payload and navigate to cart', () => {
    component.eventIdForTickets = 12;
    component.rows = [[
      { id: 101, row: 1, number: 1, status: 'free', priceCategory: 'middle' },
      { id: 102, row: 1, number: 2, status: 'free', priceCategory: 'middle' }
    ]];

    component.selectedSeats.add('1-1');
    component.selectedSeats.add('1-2');

    (component as any).seatByKey.set('1-1', component.rows[0][0]);
    (component as any).seatByKey.set('1-2', component.rows[0][1]);

    const createdTicketsMock = [{ id: 999 }, { id: 1000 }] as any;
    ticketServiceMock.createTicket.and.returnValue(of(createdTicketsMock));

    component.onContinue();

    expect(ticketServiceMock.createTicket).toHaveBeenCalledWith([
      { eventId: 12, seatId: 101 },
      { eventId: 12, seatId: 102 }
    ]);

    expect(routerMock.navigate).toHaveBeenCalledWith(
      ['/tickets/cart'],
      { state: { tickets: createdTicketsMock } }
    );
  });

  it('should set error message when getSeatmap fails', () => {
    spyOnProperty(history, 'state', 'get').and.returnValue({ eventId: 12 });

    seatmapServiceMock.getSeatmap.and.returnValue(
      throwError(() => new Error('boom'))
    );

    fixture.detectChanges();

    expect(component.isLoading).toBeFalse();
    expect(component.error).toBe('Saalplan konnte nicht geladen werden.');
  });
});
