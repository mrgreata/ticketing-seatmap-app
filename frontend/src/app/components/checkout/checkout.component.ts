import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { NgForm } from '@angular/forms';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import { LuhnValidatorDirective, NotExpiredValidatorDirective } from '../../validators/payment-validators.directive';

import { PaymentDetailDto } from '../../dtos/paymentDtos/payment-detail';
import { PaymentMethod } from '../../types/payment-method';
import { ToastrService } from 'ngx-toastr';
import { CartService } from '../../services/cart.service';
import {CartDto, CartItemDto} from "../../dtos/cartDtos/cart";
import {TicketService} from "../../services/ticket.service";
import { InvoiceService } from '../../services/invoice.service';

type MerchandiseSummaryLine = {
  merchandiseId: number;
  name: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
};

type TicketSummaryLine = {
  ticketId: number;
  eventTitle: string;
  ticketCount: number;
  totalPrice: number;
};

@Component({
  selector: 'app-merchandise-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LuhnValidatorDirective, NotExpiredValidatorDirective],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {

  cart?: CartDto;
  merchandiseLines: MerchandiseSummaryLine[] = [];
  ticketLines: TicketSummaryLine[] = [];

  paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD;
  paymentDetail: PaymentDetailDto = {};

  readonly PaymentMethod = PaymentMethod;

  loading = false;
  error?: string;

  showInvoiceModal = false;
  wantInvoice = true;
  ticketInvoiceId: number | null;
  merchInvoiceId: number | null;


  private pendingCartItems: CartItemDto[] = [];


  constructor(
    private cartService: CartService,
    private ticketService: TicketService,
    private router: Router,
    private toastr: ToastrService,
    private invoiceService: InvoiceService,
    private errorFormatter: ErrorFormatterService
  ) {}

  ngOnInit() {
    this.loadCart();

    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state as any;

    if (state?.showInvoiceModal) {
      this.showInvoiceModal = true;
      this.wantInvoice = true;
      this.pendingCartItems = [];

      this.ticketInvoiceId = state.ticketInvoiceId;
      this.merchInvoiceId = state.merchandiseInvoiceId;
    }
  }


  private loadCart(): void {
    this.loading = true;
    this.error = undefined;

    this.cartService.getMyCart().subscribe({
      next: cart => {
        this.loading = false;
        this.cart = cart;
        if (!cart.items || cart.items.length == 0) {
          this.toastr.error(
            'Warenkorb ist leer, bitte fügen Sie einen Artikel hinzu!',
            'Warenkorb leer'
          );
          this.error = 'Warenkorb ist leer, bitte fügen Sie einen Artikel hinzu!';
          return;
        }
        this.merchandiseLines = this.buildMerchandiseLines(cart.items);
        this.buildTicketLines(cart.items);
      },
      error: err => {
        this.loading = false;
        this.toastr.error(
          this.errorFormatter.format(err),
          'Fehler',
          { enableHtml: true, timeOut: 10000 }
        );
      }
    });
  }

  private buildMerchandiseLines(items: CartItemDto[]): MerchandiseSummaryLine[] {
    const merchItems = items.filter(i => i.type === 'MERCHANDISE' || i.type === 'REWARD');

    return merchItems
      .filter(i => i.merchandiseId != null)
      .map(i => {
        const qty = i.quantity ?? 0;
        const unit = i.unitPrice ?? 0;
        return {
          merchandiseId: i.merchandiseId as number,
          name: i.name ?? `Artikel #${i.merchandiseId}`,
          quantity: qty,
          unitPrice: unit,
          totalPrice: unit * qty
        };
      })
      .filter(l => l.quantity > 0);
  }

  private buildTicketLines(items: CartItemDto[]): void {
    const ticketItems = items.filter(i => i.type === 'TICKET');
    if (ticketItems.length === 0) {
      this.ticketLines = [];
      return;
    }

    const grouped = new Map<string, TicketSummaryLine>();

    ticketItems.forEach(i => {
      const key = `${i.eventTitle ?? 'Event'}|${i.unitPrice ?? 0}`;
      if (grouped.has(key)) {
        const existing = grouped.get(key)!;
        existing.ticketCount += 1;
        existing.totalPrice += i.unitPrice ?? 0;
      } else {
        grouped.set(key, {
          ticketId: i.ticketId ?? 0,
          eventTitle: i.eventTitle ?? 'Event',
          ticketCount: 1,
          totalPrice: i.unitPrice ?? 0
        });
      }
    });

    // Map in Array konvertieren
    this.ticketLines = Array.from(grouped.values());
  }

  onPaymentMethodChange(): void {
    this.paymentDetail = {}
    this.error = undefined;
  }

  submit(form: NgForm): void {
    if (this.loading) return;

    this.error = undefined;

    if (form.invalid) {
      form.form.markAllAsTouched();
      return;
    }

    const hasAnything = (this.cart?.items?.length ?? 0) > 0;
    if (!hasAnything) {
      this.error = 'Keine Artikel im Warenkorb.';
      return;
    }

    this.pendingCartItems = [...(this.cart?.items ?? [])];
    this.showInvoiceModal = true;
  }

  cancelInvoiceModal(): void {
    this.showInvoiceModal = false;
    this.pendingCartItems = [];
    this.wantInvoice = true;
  }

  private downloadInvoices(ticketInvoiceId?: number | null, merchInvoiceId?: number | null, rewardInvoiceId? : number): void {

    if (ticketInvoiceId) {
      this.invoiceService.downloadPdf(ticketInvoiceId).subscribe(blob => {
        if (!blob) return;
        const url = window.URL.createObjectURL(blob);
        window.open(url);
      });
    }

    if (merchInvoiceId) {
      this.invoiceService.downloadPdf(merchInvoiceId).subscribe(blob => {
        if (!blob) return;
        const url = window.URL.createObjectURL(blob);
        window.open(url);
      });
    }

    if (rewardInvoiceId) {
      this.invoiceService.downloadPdf(rewardInvoiceId).subscribe(blob => {
        if (!blob) return;
        const url = window.URL.createObjectURL(blob);
        window.open(url);
      });
    }
  }

  confirmCheckout(): void {
    if (!this.pendingCartItems.length) return;

    this.loading = true;

    this.cartService.checkout({
      paymentMethod: this.paymentMethod,
      paymentDetail: this.paymentDetail
    }).subscribe({
      next: res => {
        this.loading = false;
        this.toastr.success('Bestellung erfolgreich abgeschlossen!', 'Erfolg');

        if (this.wantInvoice) {
          this.downloadInvoices(res.ticketInvoiceId, res.merchandiseInvoiceId, res.rewardInvoiceId);
        }
        this.showInvoiceModal = false;


        this.router.navigate([''], {
          state: { successMessage: 'Erfolgreich bezahlt!' }
        });

        this.pendingCartItems = [];
      },
      error: err => {
        this.loading = false;
        this.toastr.error(
          this.errorFormatter.format(err),
          'Bezahlvorgang fehlgeschlagen',
          { enableHtml: true, timeOut: 10000 }
        );
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/cart']);
  }

  submitPaypal(form: NgForm): void {
    if (this.loading) return;

    this.paymentMethod = PaymentMethod.PAYPAL;
    this.submit(form);
  }
}
