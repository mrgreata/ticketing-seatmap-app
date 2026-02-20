import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';

import { MerchandiseRewardsComponent } from './merchandise-rewards.component';

import { MerchandiseService } from '../../../services/merchandise.service';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';

import { Merchandise } from '../../../dtos/merchandiseDtos/merchandise';

describe('MerchandiseRewardsComponent (paging)', () => {
  let component: MerchandiseRewardsComponent;
  let fixture: ComponentFixture<MerchandiseRewardsComponent>;

  let merchandiseServiceSpy: jasmine.SpyObj<MerchandiseService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;

  beforeEach(async () => {
    merchandiseServiceSpy = jasmine.createSpyObj<MerchandiseService>('MerchandiseService', [
      'getRewardMerchandise',
      'deleteMerchandise',
      'getImageUrl'
    ]);

    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', [
      'getMyRewardPoints',
      'getMyTotalCentsSpent'
    ]);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['success', 'error']);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAdmin']);

    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', ['addItem']);

    merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of([]));
    merchandiseServiceSpy.deleteMerchandise.and.returnValue(of(void 0));
    merchandiseServiceSpy.getImageUrl.and.callFake((id: number) => `img/${id}`);

    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 0 }));
    userServiceSpy.getMyTotalCentsSpent.and.returnValue(of({ totalCentsSpent: 0 }));

    authServiceSpy.isAdmin.and.returnValue(false);

    cartServiceSpy.addItem.and.returnValue(of({ items: [] } as any));

    await TestBed.configureTestingModule({
      imports: [MerchandiseRewardsComponent, RouterTestingModule],
      providers: [
        { provide: MerchandiseService, useValue: merchandiseServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: CartService, useValue: cartServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MerchandiseRewardsComponent);
    component = fixture.componentInstance;

    (component as any).pageSize = 12;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit should load rewards, points, spent, and set isAdmin', () => {
    authServiceSpy.isAdmin.and.returnValue(true);

    fixture.detectChanges();

    expect(merchandiseServiceSpy.getRewardMerchandise).toHaveBeenCalledTimes(1);
    expect(userServiceSpy.getMyRewardPoints).toHaveBeenCalledTimes(1);
    expect(userServiceSpy.getMyTotalCentsSpent).toHaveBeenCalledTimes(1);
    expect(component.isAdmin).toBeTrue();

    if ((component as any).currentPage !== undefined) {
      expect((component as any).currentPage).toBe(0);
    }
  });

  describe('loadRewards(resetPaging)', () => {
    it('should show first 12 rewards and set hasMore=true if more exist', () => {
      const rewards = Array.from({ length: 13 }, (_, i) => ({ id: i + 1 } as any as Merchandise));
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of(rewards));

      (component as any).loadRewards(true);

      expect(component.loading).toBeFalse();
      expect(component.error).toBeUndefined();

      expect(component.rewards.length).toBe(12);
      expect((component as any).hasMore).toBeTrue();
      if ((component as any).currentPage !== undefined) {
        expect((component as any).currentPage).toBe(0);
      }
    });

    it('should show all rewards and set hasMore=false if <= 12 exist', () => {
      const rewards = Array.from({ length: 5 }, (_, i) => ({ id: i + 1 } as any as Merchandise));
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of(rewards));

      (component as any).loadRewards(true);

      expect(component.rewards.length).toBe(5);
      expect((component as any).hasMore).toBeFalse();
    });

    it('should set error and stop loading on failure', () => {
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(
        throwError(() => new Error('boom'))
      );

      (component as any).loadRewards(true);

      expect(component.loading).toBeFalse();
      expect(component.error).toBe('Fehler beim Laden der Prämien!');
    });
  });

  describe('loadMore()', () => {
    it('should append next page and update hasMore', () => {
      const rewards = Array.from({ length: 25 }, (_, i) => ({ id: i + 1 } as any as Merchandise));
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of(rewards));

      (component as any).loadRewards(true);
      expect(component.rewards.length).toBe(12);
      expect((component as any).hasMore).toBeTrue();

      (component as any).loadMore();

      if ((component as any).currentPage !== undefined) {
        expect((component as any).currentPage).toBe(1);
      }
      expect(component.rewards.length).toBe(24);
      expect((component as any).hasMore).toBeTrue();

      (component as any).loadMore();

      if ((component as any).currentPage !== undefined) {
        expect((component as any).currentPage).toBe(2);
      }
      expect(component.rewards.length).toBe(25);
      expect((component as any).hasMore).toBeFalse();
    });

    it('should do nothing if hasMore=false', () => {
      const rewards = Array.from({ length: 5 }, (_, i) => ({ id: i + 1 } as any as Merchandise));
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of(rewards));

      (component as any).loadRewards(true);
      expect((component as any).hasMore).toBeFalse();

      (component as any).loadMore();

      if ((component as any).currentPage !== undefined) {
        expect((component as any).currentPage).toBe(0);
      }
      expect(component.rewards.length).toBe(5);
    });
  });

  describe('delete()', () => {
    it('should delete, show success toast, clear selection, and reload rewards', () => {
      const rewards = Array.from({ length: 13 }, (_, i) => ({ id: i + 1, name: `R${i + 1}` } as any));
      merchandiseServiceSpy.getRewardMerchandise.and.returnValue(of(rewards));

      const loadRewardsSpy = spyOn(component as any, 'loadRewards').and.callThrough();

      (component as any).loadRewards(true);
      component.selectedForDelete = rewards[0] as any;

      component.delete(rewards[0] as any);

      expect(merchandiseServiceSpy.deleteMerchandise).toHaveBeenCalledWith(1);
      expect(toastrSpy.success).toHaveBeenCalledWith('Artikel erfolgreich gelöscht.', 'Merchandise');
      expect(component.selectedForDelete).toBeUndefined();
      expect(loadRewardsSpy).toHaveBeenCalled();
    });

    it('should show error toast and reset flags on failure', () => {
      merchandiseServiceSpy.deleteMerchandise.and.returnValue(
        throwError(() => ({ status: 500 }))
      );

      component.selectedForDelete = { id: 7 } as any;

      component.delete({ id: 7 } as any);

      expect(toastrSpy.error).toHaveBeenCalledWith('Löschen fehlgeschlagen!', 'Merchandise');
      expect(component.loading).toBeFalse();
      expect(component.selectedForDelete).toBeUndefined();
    });
  });

  describe('redeem()', () => {
    it('should show error toast and not call cart when cannot redeem', () => {
      component.isAdmin = false;
      component.totalCentsSpent = 0;
      component.rewardPoints = 999;

      const merch = {
        id: 1, name: 'Reward', redeemableWithPoints: true, remainingQuantity: 1, pointsPrice: 10
      } as any;

      component.redeem(merch);

      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Diese Prämie ist derzeit nicht verfügbar.',
        'Nicht verfügbar'
      );
      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should add item to cart (redeemedWithPoints=true) and reload data on success', () => {
      component.isAdmin = false;
      component.totalCentsSpent = 10_000;
      component.rewardPoints = 100;

      const merch = {
        id: 5,
        name: 'VIP Reward',
        redeemableWithPoints: true,
        remainingQuantity: 1,
        pointsPrice: 10
      } as any;

      const loadRewardsSpy = spyOn(component as any, 'loadRewards').and.callThrough();
      const loadPointsSpy = spyOn<any>(component, 'loadRewardPoints').and.callThrough();
      const loadSpentSpy = spyOn<any>(component, 'loadTotalCentsSpent').and.callThrough();

      component.redeem(merch);

      expect(cartServiceSpy.addItem).toHaveBeenCalledWith({
        merchandiseId: 5,
        quantity: 1,
        redeemedWithPoints: true
      });

      expect(toastrSpy.success).toHaveBeenCalledWith(
        `Prämie '${merch.name}' wurde dem Warenkorb hinzugefügt.`,
        'Warenkorb'
      );

      expect(loadPointsSpy).toHaveBeenCalled();
      expect(loadSpentSpy).toHaveBeenCalled();
      expect(loadRewardsSpy).toHaveBeenCalled();
    });

    it('should set error message and show toast on cart add failure, and reload points', () => {
      component.isAdmin = false;
      component.totalCentsSpent = 10_000;
      component.rewardPoints = 100;

      cartServiceSpy.addItem.and.returnValue(
        throwError(() => ({ error: { message: 'Nope' } }))
      );

      const loadPointsSpy = spyOn<any>(component, 'loadRewardPoints').and.callThrough();

      const merch = {
        id: 1,
        name: 'Reward',
        redeemableWithPoints: true,
        remainingQuantity: 1,
        pointsPrice: 10
      } as any;

      component.redeem(merch);

      expect(component.error).toBe('Nope');
      expect(toastrSpy.error).toHaveBeenCalledWith('Nope', 'Warenkorb');
      expect(loadPointsSpy).toHaveBeenCalled();
    });
  });

  describe('computed getters', () => {
    it('isRegularCustomer should reflect threshold of 5000 cents', () => {
      component.totalCentsSpent = 4999;
      expect(component.isRegularCustomer).toBeFalse();

      component.totalCentsSpent = 5000;
      expect(component.isRegularCustomer).toBeTrue();
    });

    it('totalSpentEuro should be undefined when totalCentsSpent undefined, else cents/100', () => {
      component.totalCentsSpent = undefined;
      expect(component.totalSpentEuro).toBeUndefined();

      component.totalCentsSpent = 1234;
      expect(component.totalSpentEuro).toBe(12.34);
    });

    it('missingEuroToRegular should be undefined when totalCentsSpent undefined, else missing to threshold', () => {
      component.totalCentsSpent = undefined;
      expect(component.missingEuroToRegular).toBeUndefined();

      component.totalCentsSpent = 0;
      expect(component.missingEuroToRegular).toBe(50);

      component.totalCentsSpent = 6000;
      expect(component.missingEuroToRegular).toBe(0);
    });
  });

  it('getImageUrl should delegate to merchandiseService.getImageUrl', () => {
    const url = component.getImageUrl(12);
    expect(merchandiseServiceSpy.getImageUrl).toHaveBeenCalledWith(12);
    expect(url).toBe('img/12');
  });

  it('confirmDeleteSelectedMerchandise should do nothing when nothing selected', () => {
    const deleteSpy = spyOn(component, 'delete').and.callThrough();
    component.selectedForDelete = undefined;

    component.confirmDeleteSelectedMerchandise();

    expect(deleteSpy).not.toHaveBeenCalled();
  });

  it('confirmDeleteSelectedMerchandise should call delete when selection exists', () => {
    const item = { id: 77 } as any;
    const deleteSpy = spyOn(component, 'delete').and.callThrough();

    component.selectedForDelete = item;
    component.confirmDeleteSelectedMerchandise();

    expect(deleteSpy).toHaveBeenCalledWith(item);
  });
});
