import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from "@angular/forms";
import {TicketCancelled, TicketPurchased, TicketReserved} from '../../../dtos/ticket';
import {TicketService} from "../../../services/ticket.service";
import {CartService} from "../../../services/cart.service";
import {InvoiceService} from '../../../services/invoice.service';
import {AuthService} from '../../../services/auth.service';
import {ToastrService} from 'ngx-toastr';
import {ErrorFormatterService} from '../../../services/error-formatter.service';
import {catchError, forkJoin, last, map, of} from "rxjs";

interface TicketPurchasedGroup {
  eventName: string;
  tickets: TicketPurchased[]; // oder TicketReserved für reservierte Tickets
  nextEventDate: Date;        // das früheste Datum in der Gruppe (für Sortierung)
}

interface TicketReservedGroup {
  eventName: string;
  tickets: TicketReserved[]; // oder TicketReserved für reservierte Tickets
  nextEventDate: Date;        // das früheste Datum in der Gruppe (für Sortierung)
}

@Component({
  selector: 'app-ticket-purchased',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './ticket-overview.component.html',
  styleUrls: ['./ticket-overview.component.scss']
})
export class TicketOverviewComponent implements OnInit {

  reservedTickets: TicketReserved[] = [];
  purchasedTickets: TicketPurchased[] = [];
  pastTickets: TicketPurchased[] = [];
  upcomingTickets: TicketPurchased[] = [];


  pastCollapsed = true;
  upcomingCollapsed = false;
  creditCollapsed = true;

  showInvoiceModal = false;
  wantInvoice = true;
  private pendingReservedIds: number[] = [];

  showCreditModal = false;
  pendingCreditInvoices: [number, number[]][] = [];
  wantCreditInvoice = true;
  creditInvoices: {
    id: number;
    invoiceNumber: string;
    invoiceCancellationDate: string;
    selected: boolean;
    groups: {
      eventName: string;
      eventDate: Date;
      count: number;
    }[];
  }[] = [];
  reservedGroups: TicketReservedGroup[] = [];
  upcomingGroups: TicketPurchasedGroup[] = [];
  pastGroups: TicketPurchasedGroup[] = [];


  constructor(
    private router: Router,
    private ticketService: TicketService,
    private authService: AuthService,
    private invoiceService: InvoiceService,
    private cartService: CartService,
    private notification: ToastrService,
    private errorFormatter: ErrorFormatterService
  ) {
  }

  ngOnInit(): void {
    this.reload();
  }


  private reload(): void {
    const now = new Date();
    now.setHours(0, 0, 0, 0);

    this.loadReservations(now);
    this.loadPurchasedTickets(now);
    this.loadCreditInvoices();
  }

  private buildUpcomingGroups() {
    const map = new Map<string, TicketPurchased[]>();

    this.upcomingTickets.forEach(t => {
      if (!map.has(t.eventName)) map.set(t.eventName, []);
      map.get(t.eventName)!.push(t);
    });

    this.upcomingGroups = Array.from(map.entries())
      .map(([eventName, tickets]) => {
        tickets.sort((a, b) => a.eventDate.getTime() - b.eventDate.getTime());
        return {eventName, tickets, nextEventDate: tickets[0].eventDate};
      })
      .sort((a, b) => a.nextEventDate.getTime() - b.nextEventDate.getTime());
  }


  private buildPastGroups() {
    const map = new Map<string, TicketPurchased[]>();

    this.pastTickets.forEach(t => {
      if (!map.has(t.eventName)) map.set(t.eventName, []);
      map.get(t.eventName)!.push(t);
    });

    this.pastGroups = Array.from(map.entries())
      .map(([eventName, tickets]) => {
        tickets.sort((a, b) => a.eventDate.getTime() - b.eventDate.getTime());
        return {eventName, tickets, nextEventDate: tickets[0].eventDate};
      })
      .sort((a, b) => a.nextEventDate.getTime() - b.nextEventDate.getTime());
  }


  private buildReservedGroups() {
    const map = new Map<string, TicketReserved[]>();

    this.reservedTickets.forEach(t => {
      if (!map.has(t.eventName)) map.set(t.eventName, []);
      map.get(t.eventName)!.push(t);
    });

    this.reservedGroups = Array.from(map.entries())
      .map(([eventName, tickets]) => {
        tickets.sort((a, b) => a.eventDate.getTime() - b.eventDate.getTime());
        return {eventName, tickets, nextEventDate: tickets[0].eventDate};
      })
      .sort((a, b) => a.nextEventDate.getTime() - b.nextEventDate.getTime());
  }


  private loadCreditInvoices() {
    this.invoiceService.getMyCreditInvoices().subscribe({
      next: invoices => {
        this.creditInvoices = invoices.map(inv => {
          const grouped: Record<string, { eventName: string; eventDate: Date; count: number }> = {};

          inv.tickets.forEach(t => {
            const eventDateObj = new Date(t.eventDate);

            const key = `${t.eventName}|${eventDateObj.toISOString()}`;

            if (!grouped[key]) {
              grouped[key] = {
                eventName: t.eventName,
                eventDate: eventDateObj,
                count: 0
              };
            }
            grouped[key].count++;
          });

          return {
            id: inv.id,
            invoiceNumber: inv.invoiceNumber,
            invoiceCancellationDate: inv.invoiceCancellationDate,
            selected: false,
            groups: Object.values(grouped)
          };
        });
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Laden der Stornorechnungen',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }


  private loadReservations(now: Date) {
    this.ticketService.getMyReservations().subscribe({
      next: dtos => {
        this.reservedTickets = dtos
          .map(dto => this.mapReservation(dto))
          .filter(t => t.eventDate >= now);
        this.reservedTickets.forEach(t => t.selected = false);
        this.buildReservedGroups();
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Laden der Reservierungen',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }


  public mapReservation(dto: any): TicketReserved {
    const [hours, minutes] = dto.entryTime.split(':').map(Number);
    const eventDateTime = new Date(dto.eventDate);
    eventDateTime.setHours(hours, minutes, 0, 0);
    const reservationExpires = new Date(eventDateTime.getTime() - 30 * 60 * 1000);

    return {
      ticketId: dto.ticketId,
      reservationId: dto.reservationId,
      reservationNumber: dto.reservationNumber,
      eventName: dto.eventName,
      rowNumber: dto.rowNumber,
      seatNumber: dto.seatNumber,
      seatId: dto.seatId,
      eventDate: eventDateTime,
      entryTime: dto.entryTime,
      price: dto.price,
      selected: false,
      reservationExpires
    };
  }


  private loadPurchasedTickets(now: Date) {
    this.ticketService.getMyTickets().subscribe({
      next: dtos => {
        this.purchasedTickets = dtos.map(t => {
          const [hours, minutes] = t.entryTime?.split(':').map(Number) ?? [0, 0];
          const eventDate = new Date(t.eventDate);
          eventDate.setHours(hours, minutes, 0, 0);
          return {...t, selected: false, eventDate};
        });
        this.pastTickets = this.purchasedTickets.filter(t => t.eventDate < now);
        this.upcomingTickets = this.purchasedTickets.filter(t => t.eventDate >= now);
        this.buildUpcomingGroups();
        this.buildPastGroups();
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Laden der gekauften Tickets',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }

  togglePastTickets() {
    this.pastCollapsed = !this.pastCollapsed;
  }

  toggleUpcomingTickets() {
    this.upcomingCollapsed = !this.upcomingCollapsed;
  }


  isGroupSelected(group: { tickets: { selected: boolean }[] }): boolean {
    return group.tickets.length > 0 && group.tickets.every(t => t.selected);
  }


  toggleGroup(group: { tickets: { selected: boolean }[] }, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    group.tickets.forEach(t => t.selected = checked);
  }


  addReservedTicketsToCart(): void {
    const selectedTickets = this.reservedTickets.filter(t => t.selected);

    if (!selectedTickets.length) {
      this.notification.warning('Keine Tickets ausgewählt.', 'Aktion nicht möglich');
      return;
    }

    console.log('Trying to add these tickets:', selectedTickets);

    const ticketIds = selectedTickets.map(t => t.ticketId);
    console.log('Calling service add Tickets with these ids: ', ticketIds);
    this.cartService.addTickets(ticketIds).subscribe({
      next: () => {
        this.notification.success('Tickets erfolgreich zum Warenkorb hinzugefügt.', 'Erfolg');
        this.router.navigate(['/cart']);
      },
      error: (err) => {
        console.error('Fehler beim Hinzufügen der Tickets zum Warenkorb', err);
        this.notification.error('Fehler beim Hinzufügen der Tickets zum Warenkorb.', 'Fehler');
      }
    });
  }


  cancelInvoiceModal(): void {
    this.showInvoiceModal = false;
    this.pendingReservedIds = [];
    this.wantInvoice = true;
  }

  confirmPurchase(): void {
    if (!this.pendingReservedIds.length) return;

    this.ticketService.purchaseTickets(this.pendingReservedIds).subscribe({
      next: res => {
        const invoiceId = res[0]?.invoiceId;
        if (this.wantInvoice && invoiceId) this.invoiceService.openPdf(invoiceId);

        this.showInvoiceModal = false;
        this.reload();
        this.notification.success('Tickets erfolgreich gekauft.', 'Erfolg');
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Kauf',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }

  cancelReservation(): void {
    const selectedTickets = this.reservedTickets.filter(t => t.selected);
    if (!selectedTickets.length) {
      this.notification.warning(
        'Bitte wähle mindestens eine Reservierung aus, die storniert werden soll.',
        'Keine Tickets ausgewählt'
      );
      return;
    }

    this.ticketService.cancelReservation(selectedTickets.map(t => t.ticketId)).subscribe({
      next: () => {
        this.reload();
        this.notification.success('Reservierungen erfolgreich storniert.', 'Erfolg');
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Stornieren der Reservierungen',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }


  cancelPurchasedTickets(): void {
    const selectedTickets: TicketPurchased[] = [
      ...this.upcomingGroups.flatMap(g => g.tickets.filter(t => t.selected)),
    ];

    console.log('Selected tickets:', selectedTickets);

    if (!selectedTickets.length) {
      this.notification.warning(
        'Bitte wähle mindestens ein gekauftes Ticket aus, um es zu stornieren.',
        'Keine Tickets ausgewählt'
      );
      return;
    }

    const invoiceGroups = this.groupTicketsByInvoice(selectedTickets);
    this.pendingCreditInvoices = Array.from(invoiceGroups.entries());
    if (!this.pendingCreditInvoices.length) return;

    console.log('Geht durch');
    this.showCreditModal = true;
  }


  cancelCreditModal(): void {
    this.showCreditModal = false;
    this.pendingCreditInvoices = [];
    this.wantCreditInvoice = true;
  }

  confirmCreditInvoice(): void {
    if (!this.pendingCreditInvoices || !this.pendingCreditInvoices.length) return;

    const requests = this.pendingCreditInvoices.map(([invoiceId, ticketIds]) => {
      if (this.wantCreditInvoice) {
        return this.invoiceService.downloadCreditPdf(ticketIds).pipe(
          map(blob => {
            if (!blob) return;
            const url = window.URL.createObjectURL(blob);
            window.open(url);
          }),
          catchError(err => {
            this.notification.error(
              this.errorFormatter.format(err),
              'Fehler beim Download der Gutschrift',
              {enableHtml: true, timeOut: 10000}
            );
            return of(null);
          })
        );
      } else {
        return this.ticketService.cancelTickets(ticketIds).pipe(
          map(() => {
            this.notification.success('Tickets erfolgreich storniert.', 'Erfolg');
          }),
          catchError(err => {
            this.notification.error(
              this.errorFormatter.format(err),
              'Fehler beim Stornieren der Tickets',
              {enableHtml: true, timeOut: 10000}
            );
            return of(null);
          })
        );
      }
    });

    forkJoin(requests).subscribe({
      next: () => {
        this.reload();
        this.pendingCreditInvoices = [];
        this.showCreditModal = false;
      }
    });
  }


  downloadSelectedInvoices(): void {
    const selectedTickets = this.purchasedTickets.filter(t => t.selected);

    if (!selectedTickets.length) {
      this.notification.warning(
        'Bitte wähle mindestens ein Ticket aus, um die Rechnung(en) herunterzuladen.',
        'Keine Tickets ausgewählt'
      );
      return;
    }

    const uniqueInvoiceIds = Array.from(
      new Set(selectedTickets.map(t => Number(t.invoiceId)))
    );

    uniqueInvoiceIds.forEach(id => {
      this.invoiceService.downloadPdf(id).subscribe({
        next: blob => {
          if (!blob) return;
          const url = window.URL.createObjectURL(blob);
          window.open(url);
        },
        error: err => {
          this.notification.error(
            this.errorFormatter.format(err),
            'Fehler beim Herunterladen der Rechnung',
            {enableHtml: true, timeOut: 10000}
          );
        }
      });
    });
  }


  private groupTicketsByInvoice(tickets: TicketPurchased[]): Map<number, number[]> {
    const map = new Map<number, number[]>();
    try {
      tickets.forEach(t => {
        if (!map.has(t.invoiceId!)) map.set(t.invoiceId!, []);
        map.get(t.invoiceId!)!.push(t.id);
      });
    } catch (err) {
      this.notification.error(
        'Fehler beim Gruppieren der Tickets nach Rechnungen.',
        'Verarbeitung fehlgeschlagen'
      );
      console.error('groupTicketsByInvoice error', err);
    }
    return map;
  }

  protected readonly last = last;
}
