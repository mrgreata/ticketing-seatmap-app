import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, RouterModule } from "@angular/router";
import { ToastrService } from "ngx-toastr";
import { FormsModule } from "@angular/forms";

import { CartService } from "../../services/cart.service";
import { CartDto, CartItemDto } from "../../dtos/cartDtos/cart";
import { TicketService } from "../../services/ticket.service";
import { MerchandiseService } from "../../services/merchandise.service";
import { EventService } from "../../services/event.service";
import { SharedModule } from '../../shared/shared.module';
import { ErrorFormatterService } from '../../services/error-formatter.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SharedModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  cart?: CartDto;
  loading = false;
  error?: string;

  private ticketUnitByReservation = new Map<string, number>();
  selectedForRemoval?: CartItemDto;

  private readonly QTY_MIN = 1;

  readonly QTY_DROPDOWN_WINDOW = 101;

  constructor(
    private cartService: CartService,
    private router: Router,
    private toastr: ToastrService,
    private ticketService: TicketService,
    private merchandiseService: MerchandiseService,
    private eventService: EventService,
    private errorFormatter: ErrorFormatterService
  ) {}

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    this.loading = true;
    this.error = undefined;

    this.cartService.getMyCart().subscribe({
      next: cart => {
        this.ticketService.getMyTickets().subscribe({
          next: myTickets => {
            cart.items = cart.items.map(i => {
              if (i.type === 'TICKET' && i.ticketId) {
                const ticket = myTickets.find(t => t.id === i.ticketId);
                if (ticket) {
                  return {
                    ...i,
                    rowNumber: ticket.rowNumber,
                    seatNumber: ticket.seatNumber
                  };
                }
              }
              return i;
            });

            this.cart = cart;
            this.loading = false;
            this.loadTicketUnitPricesFromTickets();
          },
          error: err => {
            console.error('Tickets konnten nicht geladen werden', err);
            // Cart trotzdem setzen
            this.cart = cart;
            this.loading = false;
          }
        });
      },
      error: err => {
        this.loading = false;
        this.error = this.errorFormatter.format(err);
      }
    });
  }

  getCartItemLabel(item: CartItemDto): string {
    if (item.type === 'MERCHANDISE' || item.type === 'REWARD') {
      const prefix = item.type === 'REWARD' ? 'Prämie – ' : '';
      return prefix + (item.name ?? 'Artikel');
    }
    const title = item.eventTitle ?? 'Event';
    return `Ticket – ${title}`;
  }

  getMerchQtyOptions(item: CartItemDto): number[] {
    if (!this.isMerch(item)) return [];

    const max = this.merchMaxQty(item);
    if (max <= 0) return [];

    const currentRaw = item.quantity ?? this.QTY_MIN;
    const current = this.clamp(currentRaw, this.QTY_MIN, max);

    const window = Math.max(1, this.QTY_DROPDOWN_WINDOW);

    const half = Math.floor(window / 2);

    let start = Math.max(this.QTY_MIN, current - half);
    let end = Math.min(max, start + window - 1);

    start = Math.max(this.QTY_MIN, end - window + 1);

    const opts: number[] = [];
    for (let q = start; q <= end; q++) opts.push(q);

    if (!opts.includes(current)) {
      opts.push(current);
      opts.sort((a, b) => a - b);
    }

    return opts;
  }

  onMerchQtyChange(item: CartItemDto, value: number): void {
    if (!this.isMerch(item)) return;

    const max = this.merchMaxQty(item);
    const qty = this.clamp(Number(value), this.QTY_MIN, max);

    item.quantity = qty;

    this.cartService.updateItem(item.id, { quantity: qty }).subscribe({
      next: cart => {
        this.cart = cart;
      },
      error: err => {
        this.toastr.error(this.errorFormatter.format(err), 'Warenkorb');
        this.loadCart();
      }
    });
  }

  isQtyWindowed(item: CartItemDto): boolean {
    if (!this.isMerch(item)) return false;
    return this.merchMaxQty(item) > this.QTY_DROPDOWN_WINDOW;
  }

  remove(item: CartItemDto): void {
    this.selectedForRemoval = item;
  }

  confirmRemoveSelectedCartItem(): void {
    const item = this.selectedForRemoval;
    if (!item) return;

    this.selectedForRemoval = undefined;

    if (item.type === 'MERCHANDISE' || item.type === 'REWARD') {
      this.cartService.removeItem(item.id).subscribe({
        next: () => this.loadCart(),
        error: err => {
          console.error(err);
          this.toastr.error(this.errorFormatter.format(err), 'Warenkorb');
        }
      });
    } else {
      if (!item.ticketId) return;

      this.cartService.removeTicket(item.ticketId).subscribe({
        next: () => this.loadCart(),
        error: err => {
          console.error(err);
          this.toastr.error(this.errorFormatter.format(err), 'Warenkorb');
        }
      });
    }
  }

  checkout(): void {
    if (!this.cart || this.cart.items.length === 0) {
      this.toastr.error('Warenkorb ist leer.', 'Warenkorb');
      return;
    }
    this.router.navigate(['/checkout']);
  }

  get isEmpty(): boolean {
    return !this.cart || this.cart.items.length === 0;
  }

  private loadTicketUnitPricesFromTickets(): void {
    const items = this.cart?.items ?? [];
    const ticketItems = items.filter(i => i.type === 'TICKET');

    if (ticketItems.length === 0) {
      this.ticketUnitByReservation.clear();
      return;
    }

    this.ticketService.getMyTickets().subscribe({
      next: (tickets) => {
        this.ticketUnitByReservation.clear();
        for (const t of tickets) {
          if (!t.id) continue;
          const price = this.parseEuroNumber(t.price);
          this.ticketUnitByReservation.set(String(t.id), price);
        }
      },
      error: err => {
        console.error('Failed to load ticket prices for cart', err);
        this.ticketUnitByReservation.clear();
      }
    });
  }


  private parseEuroNumber(v: unknown): number {
    if (typeof v === 'number') return v;
    const s = String(v ?? '').trim();
    if (!s) return 0;
    const normalized = s.replace('€', '').replace(/\s/g, '').replace(',', '.');
    const n = Number(normalized);
    return Number.isFinite(n) ? n : 0;
  }

  getMerchImageUrl(item: CartItemDto): string | null {
    if (!item.merchandiseId) return null;
    return this.merchandiseService.getImageUrl(item.merchandiseId);
  }

  getEventImageUrl(item: CartItemDto): string | null {
    return item.eventId ? this.eventService.getImageUrl(item.eventId) : null;
  }

  trackByCartItemId(_: number, item: CartItemDto): number {
    return item.id;
  }

  private isMerch(item: CartItemDto): boolean {
    return item.type === 'MERCHANDISE' || item.type === 'REWARD';
  }

  private merchCurrentQty(item: CartItemDto): number {
    return item.quantity ?? 0;
  }

  private merchRemainingQty(item: CartItemDto): number {
    return item.remainingQuantity ?? 0;
  }

  private merchMaxQty(item: CartItemDto): number {
    return this.merchCurrentQty(item) + this.merchRemainingQty(item);
  }

  private clamp(n: number, min: number, max: number): number {
    if (!Number.isFinite(n)) return min;
    if (n < min) return min;
    if (n > max) return max;
    return Math.trunc(n);
  }
}
