import { ComponentFixture, TestBed, waitForAsync, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { DatePipe } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { TicketOverviewComponent } from './ticket-overview.component';
import { TicketService } from "../../../services/ticket.service";
import { CartService } from "../../../services/cart.service";
import { InvoiceService } from '../../../services/invoice.service';
import { AuthService } from '../../../services/auth.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';
import { TicketPurchased, TicketReserved } from "../../../dtos/ticket";
import { CartDto } from "../../../dtos/cartDtos/cart";
import {Component} from "@angular/core";

describe('TicketOverviewComponent', () => {
  let component: TicketOverviewComponent;
  let fixture: ComponentFixture<TicketOverviewComponent>;
  let mockTicketService: jasmine.SpyObj<TicketService>;
  let mockCartService: jasmine.SpyObj<CartService>;
  let mockInvoiceService: jasmine.SpyObj<InvoiceService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockToastrService: jasmine.SpyObj<ToastrService>;
  let mockErrorFormatterService: jasmine.SpyObj<ErrorFormatterService>;
  let router: Router;
  @Component({ template: '' })
  class DummyComponent {}

  const mockCartDto: CartDto = {
    id: 1,
    items: [],
    total: 0
  };
  beforeEach(() => {
    TestBed.resetTestingModule();
  });


  beforeEach(waitForAsync(() => {
    // Create spies
    mockTicketService = jasmine.createSpyObj('TicketService', [
      'getMyReservations',
      'getMyTickets',
      'cancelReservation',
      'cancelTickets',
      'purchaseTickets'
    ]);

    mockCartService = jasmine.createSpyObj('CartService', ['addTickets']);
    mockInvoiceService = jasmine.createSpyObj('InvoiceService', [
      'getMyCreditInvoices',
      'downloadPdf',
      'downloadCreditPdf',
      'downloadCreditPdfById',
      'openPdf'
    ]);

    mockAuthService = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getUserRole']);
    mockToastrService = jasmine.createSpyObj('ToastrService', [
      'success',
      'error',
      'warning'
    ]);
    mockErrorFormatterService = jasmine.createSpyObj('ErrorFormatterService', ['format']);

    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          { path: 'cart', component: DummyComponent }
        ]),
        FormsModule,
        HttpClientTestingModule,
        TicketOverviewComponent
      ],

      providers: [
        DatePipe,
        { provide: TicketService, useValue: mockTicketService },
        { provide: CartService, useValue: mockCartService },
        { provide: InvoiceService, useValue: mockInvoiceService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastrService, useValue: mockToastrService },
        { provide: ErrorFormatterService, useValue: mockErrorFormatterService }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TicketOverviewComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);


    mockTicketService.getMyReservations.and.returnValue(of([
      {
        ticketId: 1, // TicketReserved hat 'id', nicht 'ticketId'
        reservationId: 100,
        eventName: 'Taylor Swift Concert',
        rowNumber: 5,
        seatNumber: 12,
        seatId: 50,
        eventDate: new Date('2026-12-25T19:00:00'),
        entryTime: '19:30',
        price: '89.99',
        selected: false, // Hinzufügen
        reservationExpires: new Date('2027-12-25T19:00:00') // Hinzufügen
      }
    ]));

    mockTicketService.getMyTickets.and.returnValue(of([
      {
        id: 2,
        eventName: 'Coldplay Tour',
        rowNumber: 10,
        seatNumber: 15,
        seatId: 51,
        eventDate: new Date('2026-12-25T19:00:00'),
        entryTime: '18:30',
        invoiceId: 200,
        price: 75,
        locationCity: 'Wien',
        selected: false // Hinzufügen
      },
      {
        id: 3,
        eventName: 'Cats Musical',
        rowNumber: 8,
        seatNumber: 5,
        seatId: 52,
        eventDate: new Date('2024-12-25T19:00:00'),
        entryTime: '14:30',
        invoiceId: 201,
        price: 65.00,
        locationCity: 'Wien',
        selected: false // Hinzufügen
      }
    ]));

    mockInvoiceService.getMyCreditInvoices.and.returnValue(of([]));
    mockErrorFormatterService.format.and.returnValue('Test error message');

    // Mock für CartService
    mockCartService.addTickets.and.returnValue(of(mockCartDto));
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize and load all tickets on ngOnInit', fakeAsync(() => {
    // Act
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    // Assert
    expect(mockTicketService.getMyReservations).toHaveBeenCalled();
    expect(mockTicketService.getMyTickets).toHaveBeenCalled();
    expect(mockInvoiceService.getMyCreditInvoices).toHaveBeenCalled();

    // Überprüfe, dass die Tickets korrekt gemappt wurden
    expect(component.reservedTickets.length).toBe(1);
    expect(component.reservedTickets[0].eventName).toBe('Taylor Swift Concert');
    expect(component.reservedTickets[0].ticketId).toBe(1);
    expect(component.reservedTickets[0].selected).toBeFalse();

    expect(component.purchasedTickets.length).toBe(2);
    expect(component.purchasedTickets[0].eventName).toBe('Coldplay Tour');
    expect(component.purchasedTickets[1].eventName).toBe('Cats Musical');

    expect(component.upcomingTickets.length).toBe(1); // Coldplay is future
    expect(component.pastTickets.length).toBe(1); // Cats is past

    expect(component.reservedGroups.length).toBe(1);
    expect(component.upcomingGroups.length).toBe(1);
    expect(component.pastGroups.length).toBe(1);
  }));

  it('should handle error when loading reservations', fakeAsync(() => {
    // Arrange
    const error = new Error('Failed to load reservations');
    mockTicketService.getMyReservations.and.returnValue(throwError(() => error));

    // Act
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    // Assert
    expect(mockToastrService.error).toHaveBeenCalledWith(
      'Test error message',
      'Fehler beim Laden der Reservierungen',
      { enableHtml: true, timeOut: 10000 }
    );
  }));

  it('should handle error when loading purchased tickets', fakeAsync(() => {
    // Arrange
    const error = new Error('Failed to load tickets');
    mockTicketService.getMyTickets.and.returnValue(throwError(() => error));

    // Act
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    // Assert
    expect(mockToastrService.error).toHaveBeenCalledWith(
      'Test error message',
      'Fehler beim Laden der gekauften Tickets',
      { enableHtml: true, timeOut: 10000 }
    );
  }));

  describe('Group Building', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should build reserved groups correctly', () => {
      expect(component.reservedGroups.length).toBe(1);
      expect(component.reservedGroups[0].eventName).toBe('Taylor Swift Concert');
      expect(component.reservedGroups[0].tickets.length).toBe(1);
      expect(component.reservedGroups[0].tickets[0].ticketId).toBe(1);
    });

    it('should build upcoming groups correctly', () => {
      expect(component.upcomingGroups.length).toBe(1);
      expect(component.upcomingGroups[0].eventName).toBe('Coldplay Tour');
      expect(component.upcomingGroups[0].tickets.length).toBe(1);
      expect(component.upcomingGroups[0].tickets[0].id).toBe(2);
    });

    it('should build past groups correctly', () => {
      expect(component.pastGroups.length).toBe(1);
      expect(component.pastGroups[0].eventName).toBe('Cats Musical');
      expect(component.pastGroups[0].tickets.length).toBe(1);
      expect(component.pastGroups[0].tickets[0].id).toBe(3);
    });
  });

  describe('Toggle Methods', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should toggle past tickets collapsed state', () => {
      component.pastCollapsed = true;
      component.togglePastTickets();
      expect(component.pastCollapsed).toBeFalse();
      component.togglePastTickets();
      expect(component.pastCollapsed).toBeTrue();
    });

    it('should toggle upcoming tickets collapsed state', () => {
      component.upcomingCollapsed = false;
      component.toggleUpcomingTickets();
      expect(component.upcomingCollapsed).toBeTrue();
      component.toggleUpcomingTickets();
      expect(component.upcomingCollapsed).toBeFalse();
    });

    it('should check if all tickets in group are selected', () => {
      // Erstelle eine Test-Group
      const group = {
        tickets: [
          { selected: true, id: 1 },
          { selected: true, id: 2 },
          { selected: true, id: 3 }
        ]
      };

      expect(component.isGroupSelected(group)).toBeTrue();

      const group2 = {
        tickets: [
          { selected: true, id: 1 },
          { selected: false, id: 2 },
          { selected: true, id: 3 }
        ]
      };
      expect(component.isGroupSelected(group2)).toBeFalse();
    });

    it('should toggle all tickets in a group', () => {
      // Erstelle eine Test-Group
      const group = {
        tickets: [
          { selected: false, id: 1 },
          { selected: false, id: 2 }
        ]
      };
      const event = { target: { checked: true } } as any;

      component.toggleGroup(group, event);
      expect(group.tickets[0].selected).toBeTrue();
      expect(group.tickets[1].selected).toBeTrue();

      event.target.checked = false;
      component.toggleGroup(group, event);
      expect(group.tickets[0].selected).toBeFalse();
      expect(group.tickets[1].selected).toBeFalse();
    });
  });

  describe('Reservation Actions', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should show warning when no tickets selected for adding to cart', () => {
      component.addReservedTicketsToCart();

      expect(mockToastrService.warning).toHaveBeenCalledWith(
        'Keine Tickets ausgewählt.',
        'Aktion nicht möglich'
      );
      expect(mockCartService.addTickets).not.toHaveBeenCalled();
    });

    it('should add selected reserved tickets to cart', fakeAsync(() => {
      // Arrange
      component.reservedTickets[0].selected = true;

      // Act
      component.addReservedTicketsToCart();
      tick();

      // Assert
      expect(mockCartService.addTickets).toHaveBeenCalledWith([1]);
      expect(mockToastrService.success).toHaveBeenCalledWith(
        'Tickets erfolgreich zum Warenkorb hinzugefügt.',
        'Erfolg'
      );
    }));

    it('should handle error when adding tickets to cart', fakeAsync(() => {
      // Arrange
      component.reservedTickets[0].selected = true;
      mockCartService.addTickets.and.returnValue(throwError(() => new Error('Cart error')));

      // Act
      component.addReservedTicketsToCart();
      tick();

      // Assert
      expect(mockToastrService.error).toHaveBeenCalledWith(
        'Fehler beim Hinzufügen der Tickets zum Warenkorb.',
        'Fehler'
      );
    }));

    it('should cancel reservation successfully', fakeAsync(() => {
      // Arrange
      component.reservedTickets[0].selected = true;
      mockTicketService.cancelReservation.and.returnValue(of({}));

      // Act
      component.cancelReservation();
      tick();

      // Assert
      expect(mockTicketService.cancelReservation).toHaveBeenCalledWith([1]);
      expect(mockToastrService.success).toHaveBeenCalledWith(
        'Reservierungen erfolgreich storniert.',
        'Erfolg'
      );
    }));

    it('should show warning when no reservations selected for cancellation', () => {
      component.cancelReservation();

      expect(mockToastrService.warning).toHaveBeenCalledWith(
        'Bitte wähle mindestens eine Reservierung aus, die storniert werden soll.',
        'Keine Tickets ausgewählt'
      );
    });
  });

  describe('Purchased Tickets Actions', () => {
    beforeEach(fakeAsync(() => {
      component.ngOnInit();
      tick();
      fixture.detectChanges();
    }));

    it('should show warning when no purchased tickets selected for cancellation', () => {
      component.cancelPurchasedTickets();

      expect(mockToastrService.warning).toHaveBeenCalledWith(
        'Bitte wähle mindestens ein gekauftes Ticket aus, um es zu stornieren.',
        'Keine Tickets ausgewählt'
      );
    });

    it('should group tickets by invoice correctly', () => {
      // Arrange
      component.upcomingTickets[0].selected = true;
      component.upcomingTickets[0].invoiceId = 200;

      // Act
      component.cancelPurchasedTickets();

      // Assert
      expect(component.showCreditModal).toBeTrue();
    });

    it('should download invoices for selected tickets', fakeAsync(() => {
      // Arrange
      component.purchasedTickets[0].selected = true;
      component.purchasedTickets[0].invoiceId = 200;
      mockInvoiceService.downloadPdf.and.returnValue(of(new Blob()));

      // Act
      component.downloadSelectedInvoices();
      tick();

      // Assert
      expect(mockInvoiceService.downloadPdf).toHaveBeenCalledWith(200);
    }));

    it('should show warning when no tickets selected for invoice download', () => {
      component.downloadSelectedInvoices();

      expect(mockToastrService.warning).toHaveBeenCalledWith(
        'Bitte wähle mindestens ein Ticket aus, um die Rechnung(en) herunterzuladen.',
        'Keine Tickets ausgewählt'
      );
    });
  });

  describe('Modal Methods', () => {
    it('should close invoice modal and reset state', () => {
      component.showInvoiceModal = true;
      (component as any).pendingReservedIds = [1, 2, 3];
      component.wantInvoice = false;

      component.cancelInvoiceModal();

      expect(component.showInvoiceModal).toBeFalse();
      expect((component as any).pendingReservedIds).toEqual([]);
      expect(component.wantInvoice).toBeTrue();
    });

    it('should close credit modal and reset state', () => {
      component.showCreditModal = true;
      (component as any).pendingCreditInvoices = [[200, [1, 2]]];
      component.wantCreditInvoice = false;

      component.cancelCreditModal();

      expect(component.showCreditModal).toBeFalse();
      expect((component as any).pendingCreditInvoices).toEqual([]);
      expect(component.wantCreditInvoice).toBeTrue();
    });
  });



  describe('Edge Cases', () => {
    it('should handle empty reservation list', fakeAsync(() => {
      // Arrange
      mockTicketService.getMyReservations.and.returnValue(of([]));

      // Act
      component.ngOnInit();
      tick();

      // Assert
      expect(component.reservedTickets.length).toBe(0);
      expect(component.reservedGroups.length).toBe(0);
    }));

    it('should handle empty purchased tickets list', fakeAsync(() => {
      // Arrange
      mockTicketService.getMyTickets.and.returnValue(of([]));

      // Act
      component.ngOnInit();
      tick();

      // Assert
      expect(component.purchasedTickets.length).toBe(0);
      expect(component.upcomingTickets.length).toBe(0);
      expect(component.pastTickets.length).toBe(0);
      expect(component.upcomingGroups.length).toBe(0);
      expect(component.pastGroups.length).toBe(0);
    }));
  });

  // Zusätzliche Tests für spezifische Methoden
  describe('Specific Method Tests', () => {

    it('should group tickets by invoice', () => {
      const tickets: TicketPurchased[] = [
        { id: 1, invoiceId: 100, selected: false } as TicketPurchased,
        { id: 2, invoiceId: 100, selected: false } as TicketPurchased,
        { id: 3, invoiceId: 200, selected: false } as TicketPurchased
      ];

      const result = (component as any).groupTicketsByInvoice(tickets);

      expect(result.size).toBe(2);
      expect(result.get(100)).toEqual([1, 2]);
      expect(result.get(200)).toEqual([3]);
    });
  });
});
