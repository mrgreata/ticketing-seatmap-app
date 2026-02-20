import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from "@angular/forms";
import {ToastrService} from 'ngx-toastr';

import {TicketService} from "../../../services/ticket.service";
import {CartService} from "../../../services/cart.service";
import {InvoiceService} from '../../../services/invoice.service';
import {AuthService} from '../../../services/auth.service';
import {ErrorFormatterService} from '../../../services/error-formatter.service';
import {SimpleInvoice} from '../../../dtos/invoice';

interface InvoiceGroup {
  eventName: string;
  eventDate: Date;
  count: number;
}

interface GroupedInvoice extends SimpleInvoice {
  groups: InvoiceGroup[];
  selected: boolean;
  invoiceDate?: Date;
  invoiceCancellationDate?: Date;
}

@Component({
  selector: 'app-ticket-invoices',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './ticket-invoices.component.html',
  styleUrls: ['./ticket-invoices.component.scss']
})
export class TicketInvoicesComponent implements OnInit {

  // Normale Rechnungen
  ticketInvoices: GroupedInvoice[] = [];
  ticketInvoicesLoading = false;
  ticketInvoicesError?: string;

  // Stornorechnungen
  creditInvoices: GroupedInvoice[] = [];
  creditInvoicesLoading = false;
  creditInvoicesError?: string;

  // UI States
  creditCollapsed = true;

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
    this.reloadInvoices();
  }

  reloadInvoices(): void {
    this.loadMyTicketInvoices();
    this.loadMyCreditInvoices();
  }

  // ================= NORMALE RECHNUNGEN =================

  private loadMyTicketInvoices(): void {
    if (!this.authService.isLoggedIn() || this.authService.isAdmin()) {
      this.ticketInvoices = [];
      this.ticketInvoicesError = undefined;
      return;
    }

    this.ticketInvoicesLoading = true;
    this.ticketInvoicesError = undefined;

    this.invoiceService.getMyInvoices().subscribe({
      next: (invoices: any[]) => {
        this.ticketInvoices = invoices.map(inv => this.buildInvoiceGroups(inv, false));
        this.ticketInvoices.forEach(inv => inv.selected = false);
        this.ticketInvoicesLoading = false;
      },
      error: err => {
        console.error(err);
        this.ticketInvoicesError = 'Rechnungen konnten nicht geladen werden.';
        this.notification.error(
          this.errorFormatter.format(err),
          'Fehler beim Laden der Rechnungen',
          {enableHtml: true, timeOut: 10000}
        );
        this.ticketInvoicesLoading = false;
      }
    });
  }

// ================= STORNORECHNUNGEN =================

  private loadMyCreditInvoices(): void {
    this.creditInvoicesLoading = true;
    this.creditInvoicesError = undefined;

    this.invoiceService.getMyCreditInvoices().subscribe({
      next: (invoices: any[]) => {
        this.creditInvoices = invoices.map(inv => this.buildInvoiceGroups(inv, true));
        this.creditInvoices.forEach(inv => inv.selected = false);
        this.creditInvoicesLoading = false;
      },
      error: err => {
        console.error(err);
        this.creditInvoicesError = 'Stornorechnungen konnten nicht geladen werden.';
        this.notification.error(
          this.errorFormatter.format(err),
          'Fehler beim Laden der Stornorechnungen',
          {enableHtml: true, timeOut: 10000}
        );
        this.creditInvoicesLoading = false;
      }
    });
  }

  // ================= GRUPPIER-LOGIK =================

  private buildInvoiceGroups(inv: any, isCreditInvoice: boolean = false): GroupedInvoice {
    console.log(`Building invoice (credit: ${isCreditInvoice}):`, inv); // DEBUG

    const grouped: Record<string, InvoiceGroup> = {};

    inv.tickets?.forEach((t: any) => {
      // Backend sendet 'eventTitle'
      const eventName = t.eventTitle || 'Unbekanntes Event';

      let eventDateObj: Date;

      // Parse das Event-Datum (z.B. "2026-01-17T19:30:00" oder "2026-01-17T19:30")
      if (t.eventDate) {
        try {
          eventDateObj = new Date(t.eventDate);
        } catch (error) {
          console.warn('Could not parse eventDate from ticket:', t.eventDate);
          eventDateObj = new Date(); // Fallback
        }
      } else {
        eventDateObj = new Date(); // Fallback
      }

      // Validiere das Datum
      if (isNaN(eventDateObj.getTime())) {
        console.warn('Invalid date parsed:', t.eventDate);
        eventDateObj = new Date(); // Fallback
      }

      // Gruppierung nach EventName + EventDate
      const key = `${eventName}|${eventDateObj.toISOString()}`;

      if (!grouped[key]) {
        grouped[key] = {
          eventName: eventName,
          eventDate: eventDateObj,
          count: 0
        };
      }

      grouped[key].count++;
    });

    // Konvertiere die Datums-Strings zu Date-Objekten
    const invoiceDate = inv.invoiceDate ? new Date(inv.invoiceDate) : undefined;
    const invoiceCancellationDate = inv.invoiceCancellationDate
      ? new Date(inv.invoiceCancellationDate)
      : undefined;

    console.log(`Parsed dates - invoiceDate: ${invoiceDate}, cancellationDate: ${invoiceCancellationDate}`); // DEBUG

    return {
      ...inv,
      invoiceDate: invoiceDate,
      invoiceCancellationDate: invoiceCancellationDate,
      groups: Object.values(grouped),
      selected: false,
      isCreditInvoice: isCreditInvoice
    };
  }

  // ================= UI FUNKTIONEN =================

  toggleCreditInvoices() {
    this.creditCollapsed = !this.creditCollapsed;
  }

  // ================= RECHNUNGEN ÖFFNEN =================

  openInvoice(invoiceId: number): void {
    this.invoiceService.openPdf(invoiceId);
    // Kein subscribe hier, da openPdf void zurückgibt
  }

  openCreditInvoice(invoiceId: number): void {
    this.invoiceService.downloadCreditPdfById(invoiceId).subscribe({
      next: blob => {
        if (!blob) return;
        const url = window.URL.createObjectURL(blob);
        window.open(url);
      },
      error: err => this.notification.error(
        this.errorFormatter.format(err),
        'Fehler beim Öffnen der Stornorechnung',
        {enableHtml: true, timeOut: 10000}
      )
    });
  }

  // ================= AUSGEWÄHLTE RECHNUNGEN ÖFFNEN =================

  openSelectedInvoices(): void {
    const selectedInvoices = this.ticketInvoices.filter(inv => inv.selected);

    if (selectedInvoices.length === 0) {
      this.notification.warning(
        'Bitte wählen Sie mindestens eine Rechnung aus.',
        'Keine Rechnungen ausgewählt'
      );
      return;
    }

    selectedInvoices.forEach(inv => {
      this.openInvoice(inv.id);
    });
  }


  openSelectedCreditInvoices(): void {
    const selectedInvoices = this.creditInvoices.filter(inv => inv.selected);

    if (selectedInvoices.length === 0) {
      this.notification.warning(
        'Bitte wählen Sie mindestens eine Stornorechnung aus.',
        'Keine Stornorechnungen ausgewählt'
      );
      return;
    }

    selectedInvoices.forEach(inv => {
      this.openCreditInvoice(inv.id);
    });
  }

}
