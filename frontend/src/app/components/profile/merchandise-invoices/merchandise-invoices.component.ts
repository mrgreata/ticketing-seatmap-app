import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';

import { InvoiceService } from "../../../services/invoice.service";
import { AuthService } from '../../../services/auth.service';
import { SimpleInvoice } from '../../../dtos/invoice';

@Component({
  selector: 'app-merchandise-invoices',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './merchandise-invoices.component.html',
  styleUrl: './merchandise-invoices.component.scss',
})
export class MerchandiseInvoicesComponent implements OnInit {

  merchInvoices: SimpleInvoice[] = [];
  merchInvoicesLoading = false;
  merchInvoicesError?: string;

  selectedInvoiceIds = new Set<number>();

  constructor(
    private invoiceService: InvoiceService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadMyMerchandiseInvoices();
  }

  reloadMerchandiseInvoices(): void {
    this.loadMyMerchandiseInvoices();
  }

  private loadMyMerchandiseInvoices(): void {
    if (!this.authService.isLoggedIn() || this.authService.isAdmin()) {
      this.merchInvoices = [];
      this.merchInvoicesError = undefined;
      this.selectedInvoiceIds.clear();
      return;
    }

    this.merchInvoicesLoading = true;
    this.merchInvoicesError = undefined;
    this.selectedInvoiceIds.clear();

    this.invoiceService.getMyMerchandiseInvoices().subscribe({
      next: (invoices) => {
        this.merchInvoices = invoices ?? [];
        this.merchInvoicesLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.merchInvoicesError = 'Bestellungen konnten nicht geladen werden.';
        this.merchInvoicesLoading = false;
      }
    });
  }

  toggleInvoiceSelection(invoiceId: number, checked: boolean): void {
    if (checked) {
      this.selectedInvoiceIds.add(invoiceId);
    } else {
      this.selectedInvoiceIds.delete(invoiceId);
    }
  }

  openSelectedInvoices(): void {
    if (this.selectedInvoiceIds.size === 0) {
      this.toastr.error('Bitte wählen Sie mindestens eine Rechnung aus.', 'Rechnung');
      return;
    }

    for (const id of this.selectedInvoiceIds) {
      this.invoiceService.openPdf(id);
    }
  }

  parseInvoiceDate(invoiceNumber: string | undefined | null): Date | null {
    if (!invoiceNumber) return null;

    const m = /^INV-(\d{8})-(\d{6})-/.exec(invoiceNumber);
    if (!m) return null;

    const ymd = m[1];
    const hms = m[2];

    const year = Number(ymd.slice(0, 4));
    const month = Number(ymd.slice(4, 6));
    const day = Number(ymd.slice(6, 8));

    const hour = Number(hms.slice(0, 2));
    const minute = Number(hms.slice(2, 4));

    const d = new Date(year, month - 1, day, hour, minute);

    return Number.isNaN(d.getTime()) ? null : d;
  }

  openInvoice(invoiceId: number): void {
    this.invoiceService.openPdf(invoiceId);
  }
}
