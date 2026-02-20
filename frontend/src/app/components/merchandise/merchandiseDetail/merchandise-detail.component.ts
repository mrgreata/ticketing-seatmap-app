import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Merchandise } from '../../../dtos/merchandiseDtos/merchandise';
import { MerchandiseService } from '../../../services/merchandise.service';
import { ToastrService } from 'ngx-toastr';
import { CartService } from '../../../services/cart.service';
import { ErrorFormatterService } from '../../../services/error-formatter.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-merchandise-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './merchandise-detail.component.html',
  styleUrls: ['./merchandise-detail.component.scss']
})
export class MerchandiseDetailComponent implements OnInit {

  item?: Merchandise;
  quantity = 1;

  isAdmin = false;

  loading = false;
  error?: string;
  success?: string;
  buying = false;
  redeeming = false;
  rewardPoints?: number;
  totalCentsSpent?: number;
  private readonly REGULAR_CUSTOMER_THRESHOLD_CENTS = 5000;

  /** Max number of quantity options shown in the dropdown to avoid rendering huge option lists. */
  readonly QTY_SELECT_CAP = 100;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private merchandiseService: MerchandiseService,
    private cartService: CartService,
    private toastr: ToastrService,
    private errorFormatter: ErrorFormatterService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error = 'Ungültige Artikel-ID!';
      return;
    }
    this.loadItem(id);
    this.isAdmin = this.authService.isAdmin();
    this.loadMyRewardState();
  }

  loadItem(id: number): void {
    this.loading = true;
    this.error = undefined;
    this.success = undefined;

    this.merchandiseService.getMerchandiseById(id).subscribe({
      next: item => {
        this.item = item;

        this.quantity = this.clampQtyToUiMax(this.quantity, item?.remainingQuantity);

        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.error = 'Fehler beim Laden des Artikels!';
        this.loading = false;
      }
    });
  }

  private clampQtyToUiMax(qty: number, remaining?: number | null): number {
    const stockMax = Math.max(1, remaining ?? 0);
    const uiMax = Math.min(stockMax, this.QTY_SELECT_CAP);
    return Math.min(Math.max(1, qty ?? 1), uiMax);
  }

  onQuantityChange(q: number): void {
    this.quantity = this.clampQtyToUiMax(q, this.item?.remainingQuantity);
  }

  private loadMyRewardState(): void {
    this.userService.getMyRewardPoints().subscribe({
      next: dto => this.rewardPoints = dto.rewardPoints,
      error: () => {}
    });

    this.userService.getMyTotalCentsSpent().subscribe({
      next: dto => this.totalCentsSpent = dto.totalCentsSpent,
      error: () => {}
    });
  }

  buy(): void {
    if (!this.item) return;

    const qty = this.clampQtyToUiMax(this.quantity, this.item.remainingQuantity);

    if (qty > this.item.remainingQuantity) {
      this.toastr.error(`Nur ${this.item.remainingQuantity} Stück verfügbar.`, `Menge ungültig`);
      return;
    }

    this.buying = true;
    this.cartService.addItem({ merchandiseId: this.item.id, quantity: qty, redeemedWithPoints: false }).subscribe({
      next: () => {
        this.toastr.success('Zum Warenkorb hinzugefügt.', 'Warenkorb');
        this.buying = false;
        this.loadItem(this.item!.id);
      },
      error: err => {
        this.toastr.error(
          this.errorFormatter.format(err),
          'Warenkorb',
          { enableHtml: true, timeOut: 10000 }
        );
        this.buying = false;
      }
    });
  }

  backToList(): void {
    this.router.navigate(['/merchandise']);
  }

  get remaining(): number {
    return this.item?.remainingQuantity ?? 0;
  }

  get quantityInvalid(): boolean {
    const qty = this.quantity > 0 ? this.quantity : 1;
    return !!this.item && qty > this.remaining;
  }

  getImageUrl(): string | null {
    if (!this.item) return null;
    return this.merchandiseService.getImageUrl(this.item.id);
  }

  onImageError(): void {
    if (this.item) {
      this.item.hasImage = false;
    }
  }

  redeemWithPoints(): void {
    if (!this.item) return;

    if (!this.item.redeemableWithPoints) {
      this.toastr.error('Dieser Artikel kann nicht mit Prämienpunkten eingelöst werden.', 'Prämien');
      return;
    }

    if ((this.item.remainingQuantity ?? 0) <= 0) {
      this.toastr.error('Diese Prämie ist derzeit nicht verfügbar.', 'Nicht verfügbar');
      return;
    }

    if (!this.isRegularCustomer) {
      this.toastr.error(
        'Sie sind noch kein Stammkunde. Prämien können erst ab 50,00 € Gesamtumsatz eingelöst werden.',
        'Prämien'
      );
      return;
    }

    this.redeeming = true;
    const qty = this.clampQtyToUiMax(this.quantity, this.item.remainingQuantity);

    this.cartService.addItem({
      merchandiseId: this.item.id,
      quantity: qty,
      redeemedWithPoints: true
    }).subscribe({
      next: () => {
        this.toastr.success(`Prämie '${this.item!.name}' wurde dem Warenkorb hinzugefügt.`, 'Warenkorb');
        this.redeeming = false;

        this.loadMyRewardState();
        this.loadItem(this.item!.id);
      },
      error: err => {
        this.toastr.error(
          this.errorFormatter.format(err),
          'Prämien',
          { enableHtml: true, timeOut: 10000 }
        );
        this.redeeming = false;

        this.loadMyRewardState();
      }
    });
  }

  get isRegularCustomer(): boolean {
    return (this.totalCentsSpent ?? 0) >= this.REGULAR_CUSTOMER_THRESHOLD_CENTS;
  }

  getQtyOptions(remaining?: number | null): number[] {
    const max = Math.max(0, remaining ?? 0);
    const capped = Math.min(max, this.QTY_SELECT_CAP);
    return Array.from({ length: capped }, (_, i) => i + 1);
  }

}
