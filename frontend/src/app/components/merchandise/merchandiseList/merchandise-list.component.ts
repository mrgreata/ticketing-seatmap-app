import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { Merchandise } from '../../../dtos/merchandiseDtos/merchandise';
import { MerchandiseService } from '../../../services/merchandise.service';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { SharedModule } from '../../../shared/shared.module';

@Component({
  selector: 'app-merchandise-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SharedModule],
  templateUrl: './merchandise-list.component.html',
  styleUrls: ['./merchandise-list.component.scss']
})
export class MerchandiseListComponent implements OnInit {

  private allMerchandise: Merchandise[] = [];
  merchandise: Merchandise[] = [];

  loading = false;
  error?: string;
  success?: string;

  isAdmin = false;
  selectedForDelete?: Merchandise;

  pageSize = 12;
  currentPage = 0;

  hasMore = false;
  isLoadingMore = false;

  private qtyById = new Map<number, number>();

  readonly QTY_SELECT_CAP = 100;

  constructor(
    private merchandiseService: MerchandiseService,
    private authService: AuthService,
    private router: Router,
    private cartService: CartService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const msg = nav?.extras?.state?.['successMessage'];

    this.isAdmin = this.authService.isAdmin();
    if (msg) {
      this.success = msg;
    }

    this.loadMerchandise(true);
  }

  loadMerchandise(resetPaging: boolean): void {
    this.loading = true;
    this.error = undefined;
    this.success = undefined;
    this.isLoadingMore = false;

    if (resetPaging) {
      this.currentPage = 0;
      this.hasMore = false;
    }

    this.merchandiseService.getAllMerchandise().subscribe({
      next: data => {
        this.allMerchandise = data ?? [];
        this.applyCurrentSlice();
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.error = 'Fehler beim Laden der Merchandise-Artikel!';
        this.loading = false;
      }
    });
  }

  private applyCurrentSlice(): void {
    const visibleCount = Math.min(
      this.allMerchandise.length,
      (this.currentPage + 1) * this.pageSize
    );

    this.merchandise = this.allMerchandise.slice(0, visibleCount);
    this.hasMore = this.allMerchandise.length > this.merchandise.length;

    this.initOrClampQuantities(this.merchandise);
  }

  private refreshMerchandisePreservePaging(): void {
    this.merchandiseService.getAllMerchandise().subscribe({
      next: data => {
        this.allMerchandise = data ?? [];

        const maxPage = Math.max(0, Math.ceil(this.allMerchandise.length / this.pageSize) - 1);
        this.currentPage = Math.min(this.currentPage, maxPage);

        this.applyCurrentSlice();
      },
      error: err => {
        console.error(err);
        this.toastr.error('Merchandise konnte nicht aktualisiert werden.', 'Merchandise');
      }
    });
  }


  private initOrClampQuantities(items: Merchandise[]): void {
    for (const m of items) {
      const stockMax = Math.max(1, m.remainingQuantity ?? 0);
      const uiMax = Math.min(stockMax, this.QTY_SELECT_CAP);

      const current = this.getQty(m);
      const clamped = Math.min(Math.max(1, current), uiMax);

      this.qtyById.set(m.id, clamped);
    }
  }

  loadMore(): void {
    if (this.isLoadingMore || !this.hasMore) return;

    this.isLoadingMore = true;
    this.currentPage++;

    this.applyCurrentSlice();

    this.isLoadingMore = false;
  }

  buy(item: Merchandise): void {
    if (this.isAdmin) return;

    const quantity = this.getQty(item);
    if (quantity > (item.remainingQuantity ?? 0)) {
      this.toastr.error(`Nur ${item.remainingQuantity} Stück verfügbar.`, 'Menge ungültig');
      return;
    }

    this.cartService.addItem({
      merchandiseId: item.id,
      quantity,
      redeemedWithPoints: false
    }).subscribe({
      next: () => {
        this.toastr.success('Zum Warenkorb hinzugefügt.', 'Warenkorb');
        this.refreshMerchandisePreservePaging();
      },
      error: err => {
        const msg = err?.error?.message ?? 'Hinzufügen zum Warenkorb fehlgeschlagen.';
        this.toastr.error(msg, 'Warenkorb');
      }
    });
  }

  getQty(m: Merchandise): number {
    return this.qtyById.get(m.id) ?? 1;
  }

  setQty(m: Merchandise, q: number): void {
    const stockMax = Math.max(1, m.remainingQuantity ?? 0);
    const uiMax = Math.min(stockMax, this.QTY_SELECT_CAP);

    const clamped = Math.min(Math.max(1, q ?? 1), uiMax);
    this.qtyById.set(m.id, clamped);
  }

  getQtyOptions(m: Merchandise): number[] {
    const max = Math.max(0, m.remainingQuantity ?? 0);
    const capped = Math.min(max, this.QTY_SELECT_CAP);
    return Array.from({ length: capped }, (_, i) => i + 1);
  }


  isQuantityTooHigh(item: Merchandise): boolean {
    return this.getQty(item) > (item.remainingQuantity ?? 0);
  }

  delete(item: Merchandise): void {
    this.loading = true;
    this.error = undefined;
    this.success = undefined;

    this.merchandiseService.deleteMerchandise(item.id).subscribe({
      next: () => {
        this.toastr.success('Artikel erfolgreich gelöscht.', 'Merchandise');
        this.selectedForDelete = undefined;
        this.refreshMerchandisePreservePaging();
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.toastr.error('Löschen fehlgeschlagen!', 'Merchandise');
        this.loading = false;
        this.selectedForDelete = undefined;
      }
    });
  }

  getImageUrl(id: number): string {
    return this.merchandiseService.getImageUrl(id);
  }

  confirmDeleteSelectedMerchandise(): void {
    if (!this.selectedForDelete) return;
    this.delete(this.selectedForDelete);
  }

  navigateToCreateMerchandise(): void {
    this.router.navigate(['/admin/merchandise/creation']);
  }

  onImageError(m: any): void {
    m.hasImage = false;
  }

  trackByMerchId(_: number, item: { id: number }): number {
    return item.id;
  }
}
