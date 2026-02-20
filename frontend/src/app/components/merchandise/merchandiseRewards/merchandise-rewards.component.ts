import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { Merchandise } from '../../../dtos/merchandiseDtos/merchandise';
import { MerchandiseService } from '../../../services/merchandise.service';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { SharedModule } from '../../../shared/shared.module';

@Component({
  selector: 'app-merchandise-rewards',
  standalone: true,
  imports: [CommonModule, RouterLink, SharedModule],
  templateUrl: './merchandise-rewards.component.html',
  styleUrls: ['./merchandise-rewards.component.scss']
})
export class MerchandiseRewardsComponent implements OnInit {

  // full dataset
  private allRewards: Merchandise[] = [];

  // visible slice
  rewards: Merchandise[] = [];

  loading = false;
  error?: string;

  rewardPoints?: number;
  redeemSuccess?: string;
  totalCentsSpent?: number;

  success?: string;
  isAdmin = false;

  selectedForDelete?: Merchandise;

  // paging
  pageSize = 12;
  currentPage = 0;
  hasMore = false;
  isLoadingMore = false;

  private readonly REGULAR_CUSTOMER_THRESHOLD_CENTS = 5000;

  constructor(
    private merchandiseService: MerchandiseService,
    private userService: UserService,
    private toastr: ToastrService,
    private authService: AuthService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();

    this.loadRewards(true);
    this.loadRewardPoints();
    this.loadTotalCentsSpent();
  }

  loadRewards(resetPaging: boolean): void {
    this.loading = true;
    this.error = undefined;
    this.redeemSuccess = undefined;
    this.isLoadingMore = false;

    if (resetPaging) {
      this.currentPage = 0;
      this.hasMore = false;
    }

    this.merchandiseService.getRewardMerchandise().subscribe({
      next: data => {
        this.allRewards = data ?? [];

        const maxPage = Math.max(0, Math.ceil(this.allRewards.length / this.pageSize) - 1);
        this.currentPage = Math.min(this.currentPage, maxPage);

        this.applyCurrentSlice();
        this.loading = false;
      },
      error: err => {
        console.error(err);
        this.error = 'Fehler beim Laden der Prämien!';
        this.loading = false;
      }
    });
  }

  private applyCurrentSlice(): void {
    const visibleCount = Math.min(
      this.allRewards.length,
      (this.currentPage + 1) * this.pageSize
    );

    this.rewards = this.allRewards.slice(0, visibleCount);
    this.hasMore = this.allRewards.length > this.rewards.length;
  }

  loadMore(): void {
    if (this.isLoadingMore || !this.hasMore) return;

    this.isLoadingMore = true;
    this.currentPage++;

    this.applyCurrentSlice();

    this.isLoadingMore = false;
  }

  delete(item: Merchandise): void {
    this.loading = true;
    this.error = undefined;
    this.success = undefined;

    this.merchandiseService.deleteMerchandise(item.id).subscribe({
      next: () => {
        this.toastr.success('Artikel erfolgreich gelöscht.', 'Merchandise');
        this.selectedForDelete = undefined;

        // refresh but keep already loaded pages
        this.loadRewards(false);
      },
      error: err => {
        console.error(err);
        this.toastr.error('Löschen fehlgeschlagen!', 'Merchandise');
        this.loading = false;
        this.selectedForDelete = undefined;
      }
    });
  }

  redeem(m: Merchandise): void {
    this.error = undefined;
    this.redeemSuccess = undefined;

    if (!this.canRedeem(m)) {
      this.toastr.error('Diese Prämie ist derzeit nicht verfügbar.', 'Nicht verfügbar');
      return;
    }

    this.cartService.addItem({
      merchandiseId: m.id,
      quantity: 1,
      redeemedWithPoints: true
    }).subscribe({
      next: () => {
        this.toastr.success(`Prämie '${m.name}' wurde dem Warenkorb hinzugefügt.`, 'Warenkorb');

        // points/spent might change
        this.loadRewardPoints();
        this.loadTotalCentsSpent();

        // refresh rewards but keep paging
        this.loadRewards(false);
      },
      error: err => {
        console.error(err);
        const msg = err?.error?.message ?? 'Hinzufügen zum Warenkorb fehlgeschlagen.';
        this.error = msg;
        this.toastr.error(msg, 'Warenkorb');

        // still refresh points view
        this.loadRewardPoints();
      }
    });
  }

  private loadRewardPoints(): void {
    this.userService.getMyRewardPoints().subscribe({
      next: points => this.rewardPoints = points.rewardPoints,
      error: () => {}
    });
  }

  private loadTotalCentsSpent(): void {
    this.userService.getMyTotalCentsSpent().subscribe({
      next: dto => this.totalCentsSpent = dto.totalCentsSpent,
      error: () => {}
    });
  }

  canRedeem(m: Merchandise): boolean {
    if (this.isAdmin) return false;
    if (!this.isRegularCustomer) return false;
    if (!m.redeemableWithPoints) return false;
    if ((m.remainingQuantity ?? 0) <= 0) return false;

    const price = m.pointsPrice ?? 0;
    if (price <= 0) return false;

    const points = this.rewardPoints ?? 0;
    return points >= price;
  }

  get isRegularCustomer(): boolean {
    return (this.totalCentsSpent ?? 0) >= this.REGULAR_CUSTOMER_THRESHOLD_CENTS;
  }

  get totalSpentEuro(): number | undefined {
    return this.totalCentsSpent === undefined ? undefined : this.totalCentsSpent / 100;
  }

  get missingEuroToRegular(): number | undefined {
    if (this.totalCentsSpent === undefined) return undefined;
    const missingCents = Math.max(0, this.REGULAR_CUSTOMER_THRESHOLD_CENTS - this.totalCentsSpent);
    return missingCents / 100;
  }

  getImageUrl(id: number): string {
    return this.merchandiseService.getImageUrl(id);
  }

  confirmDeleteSelectedMerchandise(): void {
    if (!this.selectedForDelete) return;
    this.delete(this.selectedForDelete);
  }
}
