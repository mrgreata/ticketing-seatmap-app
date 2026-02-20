import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { CartComponent } from './cart.component';
import { CartService } from '../../services/cart.service';
import { TicketService } from '../../services/ticket.service';
import { MerchandiseService } from '../../services/merchandise.service';
import { EventService } from '../../services/event.service';
import { ToastrService } from 'ngx-toastr';
import { ErrorFormatterService } from '../../services/error-formatter.service';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;

  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let ticketServiceSpy: jasmine.SpyObj<TicketService>;
  let merchSpy: jasmine.SpyObj<MerchandiseService>;
  let eventSpy: jasmine.SpyObj<EventService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {
    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', [
      'getMyCart', 'updateItem', 'removeItem', 'removeTicket'
    ]);
    ticketServiceSpy = jasmine.createSpyObj<TicketService>('TicketService', ['getMyTickets']);
    merchSpy = jasmine.createSpyObj<MerchandiseService>('MerchandiseService', ['getImageUrl']);
    eventSpy = jasmine.createSpyObj<EventService>('EventService', ['getImageUrl']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['error']);
    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>('ErrorFormatterService', ['format']);

    cartServiceSpy.getMyCart.and.returnValue(of({ items: [] } as any));
    cartServiceSpy.updateItem.and.returnValue(of({ items: [] } as any));
    cartServiceSpy.removeItem.and.returnValue(of(void 0));
    cartServiceSpy.removeTicket.and.returnValue(of(void 0));

    ticketServiceSpy.getMyTickets.and.returnValue(of([]));
    merchSpy.getImageUrl.and.returnValue('m.png');
    eventSpy.getImageUrl.and.returnValue('e.png');
    errorFormatterSpy.format.and.returnValue('Formatted');

    await TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: TicketService, useValue: ticketServiceSpy },
        { provide: MerchandiseService, useValue: merchSpy },
        { provide: EventService, useValue: eventSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit should load cart', () => {
    fixture.detectChanges();
    expect(cartServiceSpy.getMyCart).toHaveBeenCalled();
  });

  it('checkout should show error when cart is empty', () => {
    fixture.detectChanges();

    component.checkout();

    expect(toastrSpy.error).toHaveBeenCalledWith('Warenkorb ist leer.', 'Warenkorb');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('checkout should navigate to /checkout when cart has items', () => {
    cartServiceSpy.getMyCart.and.returnValue(of({
      items: [{ id: 1, type: 'MERCHANDISE', quantity: 1, remainingQuantity: 0, merchandiseId: 9 } as any]
    } as any));

    fixture.detectChanges();

    component.checkout();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/checkout']);
  });

  it('confirmRemoveSelectedCartItem should remove merchandise item and reload', () => {
    const loadSpy = spyOn(component, 'loadCart').and.callThrough();

    cartServiceSpy.getMyCart.and.returnValue(of({
      items: [{ id: 1, type: 'MERCHANDISE', quantity: 1, remainingQuantity: 0 } as any]
    } as any));

    fixture.detectChanges();

    component.selectedForRemoval = { id: 10, type: 'MERCHANDISE' } as any;
    component.confirmRemoveSelectedCartItem();

    expect(cartServiceSpy.removeItem).toHaveBeenCalledWith(10);
    expect(loadSpy).toHaveBeenCalled();
  });

  it('confirmRemoveSelectedCartItem should remove ticket item and reload', () => {
    const loadSpy = spyOn(component, 'loadCart').and.callThrough();
    fixture.detectChanges();

    component.selectedForRemoval = { id: 5, type: 'TICKET', ticketId: 77 } as any;
    component.confirmRemoveSelectedCartItem();

    expect(cartServiceSpy.removeTicket).toHaveBeenCalledWith(77);
    expect(loadSpy).toHaveBeenCalled();
  });

  it('onMerchQtyChange should clamp below min to 1 and call updateItem', () => {
    fixture.detectChanges();

    const item = { id: 1, type: 'MERCHANDISE', quantity: 5, remainingQuantity: 10 } as any;

    component.onMerchQtyChange(item, 0);

    expect(cartServiceSpy.updateItem).toHaveBeenCalledWith(1, { quantity: 1 });
  });

  it('onMerchQtyChange should clamp above max to max and call updateItem', () => {
    fixture.detectChanges();

    const item = { id: 1, type: 'MERCHANDISE', quantity: 5, remainingQuantity: 10 } as any;

    component.onMerchQtyChange(item, 999);

    expect(cartServiceSpy.updateItem).toHaveBeenCalledWith(1, { quantity: 15 });
  });

  it('onMerchQtyChange should toast formatted error and reload on failure', () => {
    const loadSpy = spyOn(component, 'loadCart').and.callThrough();

    cartServiceSpy.updateItem.and.returnValue(throwError(() => ({ status: 500 })));
    errorFormatterSpy.format.and.returnValue('Update failed');

    fixture.detectChanges();

    const item = { id: 1, type: 'MERCHANDISE', quantity: 1, remainingQuantity: 10 } as any;
    component.onMerchQtyChange(item, 2);

    expect(toastrSpy.error).toHaveBeenCalledWith('Update failed', 'Warenkorb');
    expect(loadSpy).toHaveBeenCalled();
  });

  it('getMerchQtyOptions should always include current quantity even if windowed', () => {
    fixture.detectChanges();

    const item = { id: 1, type: 'MERCHANDISE', quantity: 200, remainingQuantity: 800 } as any;

    const opts = component.getMerchQtyOptions(item);

    expect(opts).toContain(200);
    expect(opts.length).toBeLessThanOrEqual(component.QTY_DROPDOWN_WINDOW);

    expect(Math.max(...opts)).toBeLessThanOrEqual(1000);
    expect(Math.min(...opts)).toBeGreaterThanOrEqual(1);
  });

  it('getMerchQtyOptions should return empty for non-merch items', () => {
    fixture.detectChanges();

    const ticket = { id: 2, type: 'TICKET', quantity: 1, remainingQuantity: 10 } as any;
    expect(component.getMerchQtyOptions(ticket)).toEqual([]);
  });
});
