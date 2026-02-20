import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { MerchandiseDetailComponent } from './merchandise-detail.component';
import { MerchandiseService } from '../../../services/merchandise.service';
import { CartService } from '../../../services/cart.service';
import { ToastrService } from 'ngx-toastr';
import { ErrorFormatterService } from '../../../services/error-formatter.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';

describe('MerchandiseDetailComponent', () => {
  let component: MerchandiseDetailComponent;
  let fixture: ComponentFixture<MerchandiseDetailComponent>;

  let routeStub: any;
  let router: Router;

  let merchandiseServiceSpy: jasmine.SpyObj<MerchandiseService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    routeStub = {
      snapshot: {
        paramMap: {
          get: () => '1'
        }
      }
    };

    merchandiseServiceSpy = jasmine.createSpyObj<MerchandiseService>('MerchandiseService', [
      'getMerchandiseById',
      'getImageUrl'
    ]);

    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', ['addItem']);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['success', 'error']);

    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>('ErrorFormatterService', ['format']);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAdmin']);

    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', [
      'getMyRewardPoints',
      'getMyTotalCentsSpent'
    ]);

    merchandiseServiceSpy.getMerchandiseById.and.returnValue(
      of({ id: 1, name: 'Item', remainingQuantity: 10, hasImage: true, redeemableWithPoints: true } as any)
    );
    merchandiseServiceSpy.getImageUrl.and.returnValue('img/1');

    cartServiceSpy.addItem.and.returnValue(of({ items: [] } as any));

    authServiceSpy.isAdmin.and.returnValue(false);

    userServiceSpy.getMyRewardPoints.and.returnValue(of({ rewardPoints: 100 }));
    userServiceSpy.getMyTotalCentsSpent.and.returnValue(of({ totalCentsSpent: 10_000 }));

    errorFormatterSpy.format.and.returnValue('Formatted error');

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        MerchandiseDetailComponent
      ],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: MerchandiseService, useValue: merchandiseServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(MerchandiseDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit: should set error when route id is invalid', () => {
    routeStub.snapshot.paramMap.get = () => null;

    fixture.detectChanges();

    expect(component.error).toBe('Ungültige Artikel-ID!');
    expect(merchandiseServiceSpy.getMerchandiseById).not.toHaveBeenCalled();
  });

  it('ngOnInit: should load item, set isAdmin, and load reward state', () => {
    authServiceSpy.isAdmin.and.returnValue(true);

    fixture.detectChanges();

    expect(merchandiseServiceSpy.getMerchandiseById).toHaveBeenCalledWith(1);
    expect(component.isAdmin).toBeTrue();

    expect(userServiceSpy.getMyRewardPoints).toHaveBeenCalledTimes(1);
    expect(userServiceSpy.getMyTotalCentsSpent).toHaveBeenCalledTimes(1);

    expect(component.item?.id).toBe(1);
    expect(component.quantity).toBe(1);
    expect(component.loading).toBeFalse();
  });

  describe('loadItem()', () => {


    it('should set error and stop loading on failure', () => {
      spyOn(console, 'error');
      merchandiseServiceSpy.getMerchandiseById.and.returnValue(
        throwError(() => new Error('boom'))
      );

      component.loadItem(1);

      expect(component.error).toBe('Fehler beim Laden des Artikels!');
      expect(component.loading).toBeFalse();
    });
  });

  describe('buy()', () => {
    it('should do nothing if item is undefined', () => {
      component.item = undefined;

      component.buy();

      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });


    it('should add to cart and reload item on success', () => {
      fixture.detectChanges();

      component.quantity = 3;
      component.item!.remainingQuantity = 10;

      const loadSpy = spyOn(component, 'loadItem').and.callThrough();

      component.buy();

      expect(cartServiceSpy.addItem).toHaveBeenCalledWith({
        merchandiseId: 1,
        quantity: 3,
        redeemedWithPoints: false
      });
      expect(toastrSpy.success).toHaveBeenCalledWith('Zum Warenkorb hinzugefügt.', 'Warenkorb');
      expect(component.buying).toBeFalse();
      expect(loadSpy).toHaveBeenCalledWith(1);
    });

    it('should show formatted error toast on cart failure', () => {
      fixture.detectChanges();

      cartServiceSpy.addItem.and.returnValue(
        throwError(() => ({ status: 500 }))
      );

      component.quantity = 1;
      component.buy();

      expect(errorFormatterSpy.format).toHaveBeenCalled();
      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Formatted error',
        'Warenkorb',
        { enableHtml: true, timeOut: 10000 }
      );
      expect(component.buying).toBeFalse();
    });
  });

  describe('redeemWithPoints()', () => {
    it('should do nothing if item is undefined', () => {
      component.item = undefined;

      component.redeemWithPoints();

      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should reject when item is not redeemableWithPoints', () => {
      fixture.detectChanges();

      component.item!.redeemableWithPoints = false;

      component.redeemWithPoints();

      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Dieser Artikel kann nicht mit Prämienpunkten eingelöst werden.',
        'Prämien'
      );
      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should reject when out of stock', () => {
      fixture.detectChanges();

      component.item!.remainingQuantity = 0;

      component.redeemWithPoints();

      expect(toastrSpy.error).toHaveBeenCalledWith('Diese Prämie ist derzeit nicht verfügbar.', 'Nicht verfügbar');
      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should reject when not a regular customer (totalCentsSpent below threshold)', () => {
      fixture.detectChanges();

      component.totalCentsSpent = 0;

      component.redeemWithPoints();

      expect(toastrSpy.error).toHaveBeenCalled();
      expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    });

    it('should add redeemed item to cart and reload state + item on success', () => {
      fixture.detectChanges();

      component.totalCentsSpent = 10_000;
      component.quantity = 2;

      const loadItemSpy = spyOn(component, 'loadItem').and.callThrough();

      component.redeemWithPoints();

      expect(cartServiceSpy.addItem).toHaveBeenCalledWith({
        merchandiseId: 1,
        quantity: 2,
        redeemedWithPoints: true
      });

      expect(toastrSpy.success).toHaveBeenCalledWith(
        `Prämie '${component.item!.name}' wurde dem Warenkorb hinzugefügt.`,
        'Warenkorb'
      );

      expect(component.redeeming).toBeFalse();
      expect(userServiceSpy.getMyRewardPoints).toHaveBeenCalled();
      expect(userServiceSpy.getMyTotalCentsSpent).toHaveBeenCalled();
      expect(loadItemSpy).toHaveBeenCalledWith(1);
    });

    it('should show formatted error toast and still reload reward state on failure', () => {
      fixture.detectChanges();

      cartServiceSpy.addItem.and.returnValue(
        throwError(() => ({ status: 400 }))
      );

      component.redeemWithPoints();

      expect(errorFormatterSpy.format).toHaveBeenCalled();
      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Formatted error',
        'Prämien',
        { enableHtml: true, timeOut: 10000 }
      );
      expect(component.redeeming).toBeFalse();

      expect(userServiceSpy.getMyRewardPoints).toHaveBeenCalled();
      expect(userServiceSpy.getMyTotalCentsSpent).toHaveBeenCalled();
    });
  });

  describe('navigation + helpers', () => {
    it('backToList should navigate to /merchandise', async () => {
      await component.backToList();
      expect(router.navigate).toHaveBeenCalledWith(['/merchandise']);
    });

    it('remaining should default to 0 when item missing', () => {
      component.item = undefined;
      expect(component.remaining).toBe(0);
    });

    it('quantityInvalid should be true when quantity exceeds remaining', () => {
      fixture.detectChanges();

      component.quantity = 5;
      component.item!.remainingQuantity = 2;

      expect(component.quantityInvalid).toBeTrue();
    });

    it('getImageUrl should return null when item missing else delegate to service', () => {
      component.item = undefined;
      expect(component.getImageUrl()).toBeNull();

      component.item = { id: 9 } as any;
      const url = component.getImageUrl();
      expect(merchandiseServiceSpy.getImageUrl).toHaveBeenCalledWith(9);
      expect(url).toBe('img/1');
    });

    it('onImageError should set item.hasImage=false', () => {
      component.item = { id: 1, hasImage: true } as any;
      component.onImageError();
      expect(component.item.hasImage).toBeFalse();
    });

    it('isRegularCustomer should reflect 5000 cents threshold', () => {
      component.totalCentsSpent = 4999;
      expect(component.isRegularCustomer).toBeFalse();

      component.totalCentsSpent = 5000;
      expect(component.isRegularCustomer).toBeTrue();
    });

    it('getQtyOptions should return 1..remaining', () => {
      expect(component.getQtyOptions(0)).toEqual([]);
      expect(component.getQtyOptions(3)).toEqual([1, 2, 3]);
      expect(component.getQtyOptions(undefined)).toEqual([]);
    });
  });
});
