import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { MerchandiseListComponent } from './merchandise-list.component';
import { MerchandiseService } from '../../../services/merchandise.service';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { ToastrService } from 'ngx-toastr';

import { Merchandise } from '../../../dtos/merchandiseDtos/merchandise';

describe('MerchandiseListComponent (paging)', () => {
  let component: MerchandiseListComponent;
  let fixture: ComponentFixture<MerchandiseListComponent>;

  let merchandiseServiceSpy: jasmine.SpyObj<MerchandiseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let router: Router;

  beforeEach(async () => {
    merchandiseServiceSpy = jasmine.createSpyObj<MerchandiseService>('MerchandiseService', [
      'getAllMerchandise',
      'deleteMerchandise',
      'getImageUrl'
    ]);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAdmin']);

    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', ['addItem']);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['success', 'error']);

    merchandiseServiceSpy.getAllMerchandise.and.returnValue(of([]));
    merchandiseServiceSpy.deleteMerchandise.and.returnValue(of(void 0));
    merchandiseServiceSpy.getImageUrl.and.callFake((id: number) => `img/${id}`);

    authServiceSpy.isAdmin.and.returnValue(false);

    cartServiceSpy.addItem.and.returnValue(of({ items: [] } as any));

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, MerchandiseListComponent],
      providers: [
        { provide: MerchandiseService, useValue: merchandiseServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    spyOn(router, 'getCurrentNavigation').and.returnValue(null);

    fixture = TestBed.createComponent(MerchandiseListComponent);
    component = fixture.componentInstance;

    component.pageSize = 12;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit should set isAdmin and load merchandise with resetPaging=true', () => {
    const loadSpy = spyOn(component, 'loadMerchandise').and.callThrough();
    authServiceSpy.isAdmin.and.returnValue(true);

    fixture.detectChanges();

    expect(component.isAdmin).toBeTrue();
    expect(loadSpy).toHaveBeenCalledWith(true);
    expect(component.currentPage).toBe(0);
  });

  describe('loadMerchandise(resetPaging)', () => {
    it('should show first 12 items and set hasMore=true if more exist', () => {
      const data = Array.from({ length: 13 }, (_, i) => ({ id: i + 1, remainingQuantity: 5 } as any as Merchandise));
      merchandiseServiceSpy.getAllMerchandise.and.returnValue(of(data));

      component.loadMerchandise(true);

      expect(component.loading).toBeFalse();
      expect(component.error).toBeUndefined();

      expect(component.merchandise.length).toBe(12);
      expect(component.hasMore).toBeTrue();
      expect(component.currentPage).toBe(0);
    });

    it('should show all items and set hasMore=false if <= 12 exist', () => {
      const data = Array.from({ length: 5 }, (_, i) => ({ id: i + 1, remainingQuantity: 5 } as any as Merchandise));
      merchandiseServiceSpy.getAllMerchandise.and.returnValue(of(data));

      component.loadMerchandise(true);

      expect(component.merchandise.length).toBe(5);
      expect(component.hasMore).toBeFalse();
      expect(component.currentPage).toBe(0);
    });

    it('should initialize/clamp quantities for visible slice', () => {
      const data: Merchandise[] = [
        { id: 1, remainingQuantity: 3 } as any,
        { id: 2, remainingQuantity: 0 } as any
      ];

      component.setQty(data[0], 999);
      component.setQty(data[1], -5);

      merchandiseServiceSpy.getAllMerchandise.and.returnValue(of(data));

      component.loadMerchandise(true);

      expect(component.merchandise).toEqual(data);

      expect(component.getQty(data[0])).toBe(3);

      expect(component.getQty(data[1])).toBe(1);
    });

    it('should set error message and stop loading on failure', () => {
      merchandiseServiceSpy.getAllMerchandise.and.returnValue(
        throwError(() => new Error('boom'))
      );

      component.loadMerchandise(true);

      expect(component.loading).toBeFalse();
      expect(component.error).toBe('Fehler beim Laden der Merchandise-Artikel!');
    });
  });

  describe('loadMore()', () => {
    it('should append next page and update hasMore', () => {
      const data = Array.from({ length: 25 }, (_, i) => ({ id: i + 1, remainingQuantity: 5 } as any as Merchandise));
      merchandiseServiceSpy.getAllMerchandise.and.returnValue(of(data));

      component.loadMerchandise(true);
      expect(component.merchandise.length).toBe(12);
      expect(component.hasMore).toBeTrue();

      component.loadMore();
      expect(component.currentPage).toBe(1);
      expect(component.merchandise.length).toBe(24);
      expect(component.hasMore).toBeTrue();

      component.loadMore();
      expect(component.currentPage).toBe(2);
      expect(component.merchandise.length).toBe(25);
      expect(component.hasMore).toBeFalse();
    });

    it('should do nothing if hasMore=false', () => {
      const data = Array.from({ length: 5 }, (_, i) => ({ id: i + 1, remainingQuantity: 5 } as any as Merchandise));
      merchandiseServiceSpy.getAllMerchandise.and.returnValue(of(data));

      component.loadMerchandise(true);
      expect(component.hasMore).toBeFalse();

      component.loadMore();
      expect(component.currentPage).toBe(0);
      expect(component.merchandise.length).toBe(5);
    });
  });

  describe('quantity helpers', () => {
    it('getQty should default to 1', () => {
      expect(component.getQty({ id: 123 } as any)).toBe(1);
    });

    it('setQty should clamp to [1..remainingQuantity] (with min max=1)', () => {
      const m = { id: 1, remainingQuantity: 5 } as any;

      component.setQty(m, 0);
      expect(component.getQty(m)).toBe(1);

      component.setQty(m, 999);
      expect(component.getQty(m)).toBe(5);

      component.setQty(m, 3);
      expect(component.getQty(m)).toBe(3);
    });

    it('getQtyOptions should return 1..remainingQuantity', () => {
      expect(component.getQtyOptions({ remainingQuantity: 0 } as any)).toEqual([]);
      expect(component.getQtyOptions({ remainingQuantity: 3 } as any)).toEqual([1, 2, 3]);
      expect(component.getQtyOptions({} as any)).toEqual([]);
    });
  });

  describe('buy()', () => {
    it('should show error and not call cartService when quantity exceeds stock', () => {
      fixture.detectChanges();

      const item = { id: 1, remainingQuantity: 3 } as any;

      component.setQty(item, 3);

      item.remainingQuantity = 2;

      component.buy(item);

      expect(toastrSpy.error).toHaveBeenCalledWith('Nur 2 Stück verfügbar.', 'Menge ungültig');
      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should add to cart and refresh preserving paging on success', () => {
      const item = { id: 7, remainingQuantity: 10 } as any;
      component.setQty(item, 4);

      const refreshSpy = spyOn<any>(component, 'refreshMerchandisePreservePaging').and.callThrough();

      component.buy(item);

      expect(cartServiceSpy.addItem).toHaveBeenCalledWith({
        merchandiseId: 7,
        quantity: 4,
        redeemedWithPoints: false
      });

      expect(toastrSpy.success).toHaveBeenCalledWith('Zum Warenkorb hinzugefügt.', 'Warenkorb');
      expect(refreshSpy).toHaveBeenCalled();
    });

    it('should show server error message on cart failure', () => {
      const item = { id: 7, remainingQuantity: 10 } as any;
      component.setQty(item, 1);

      cartServiceSpy.addItem.and.returnValue(
        throwError(() => ({ error: { message: 'Nope' } }))
      );

      component.buy(item);

      expect(toastrSpy.error).toHaveBeenCalledWith('Nope', 'Warenkorb');
    });

    it('should show fallback message when server has no message', () => {
      const item = { id: 7, remainingQuantity: 10 } as any;
      component.setQty(item, 1);

      cartServiceSpy.addItem.and.returnValue(
        throwError(() => ({ status: 500 }))
      );

      component.buy(item);

      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Hinzufügen zum Warenkorb fehlgeschlagen.',
        'Warenkorb'
      );
    });
  });

  describe('delete()', () => {
    it('should delete, show success toast, clear selection, and refresh preserving paging', () => {
      const item = { id: 10 } as any;
      component.selectedForDelete = item;

      const refreshSpy = spyOn<any>(component, 'refreshMerchandisePreservePaging').and.callThrough();

      component.delete(item);

      expect(merchandiseServiceSpy.deleteMerchandise).toHaveBeenCalledWith(10);
      expect(toastrSpy.success).toHaveBeenCalledWith('Artikel erfolgreich gelöscht.', 'Merchandise');
      expect(component.selectedForDelete).toBeUndefined();
      expect(refreshSpy).toHaveBeenCalled();
    });

    it('should show error toast, stop loading, and clear selection on failure', () => {
      const item = { id: 10 } as any;
      component.selectedForDelete = item;

      merchandiseServiceSpy.deleteMerchandise.and.returnValue(
        throwError(() => ({ status: 500 }))
      );

      component.delete(item);

      expect(toastrSpy.error).toHaveBeenCalledWith('Löschen fehlgeschlagen!', 'Merchandise');
      expect(component.loading).toBeFalse();
      expect(component.selectedForDelete).toBeUndefined();
    });
  });

  it('getImageUrl should delegate to merchandiseService', () => {
    fixture.detectChanges();

    const url = component.getImageUrl(12);

    expect(merchandiseServiceSpy.getImageUrl).toHaveBeenCalledWith(12);
    expect(url).toBe('img/12');
  });

  it('confirmDeleteSelectedMerchandise should do nothing when none selected', () => {
    const delSpy = spyOn(component, 'delete').and.callThrough();

    component.selectedForDelete = undefined;
    component.confirmDeleteSelectedMerchandise();

    expect(delSpy).not.toHaveBeenCalled();
  });

  it('confirmDeleteSelectedMerchandise should call delete when selected', () => {
    const delSpy = spyOn(component, 'delete').and.callThrough();

    const item = { id: 1 } as any;
    component.selectedForDelete = item;

    component.confirmDeleteSelectedMerchandise();

    expect(delSpy).toHaveBeenCalledWith(item);
  });

  it('navigateToCreateMerchandise should navigate to admin creation route', () => {
    component.navigateToCreateMerchandise();
    expect(router.navigate).toHaveBeenCalledWith(['/admin/merchandise/creation']);
  });

  it('onImageError should set hasImage=false on provided object', () => {
    const m = { hasImage: true };
    component.onImageError(m);
    expect(m.hasImage).toBeFalse();
  });

  it('trackByMerchId should return item.id', () => {
    expect(component.trackByMerchId(0, { id: 123 })).toBe(123);
  });
});
