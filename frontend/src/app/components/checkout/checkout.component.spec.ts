import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { CheckoutComponent } from './checkout.component';
import { CartService } from '../../services/cart.service';
import { TicketService } from '../../services/ticket.service';
import { ToastrService } from 'ngx-toastr';
import { InvoiceService } from '../../services/invoice.service';
import { ErrorFormatterService } from '../../services/error-formatter.service';
import { PaymentMethod } from '../../types/payment-method';

describe('CheckoutComponent', () => {
  let component: CheckoutComponent;
  let fixture: ComponentFixture<CheckoutComponent>;

  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let invoiceSpy: jasmine.SpyObj<InvoiceService>;
  let errorFormatterSpy: jasmine.SpyObj<ErrorFormatterService>;

  beforeEach(async () => {
    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', ['getMyCart', 'checkout']);
    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['error', 'success']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate', 'getCurrentNavigation']);
    invoiceSpy = jasmine.createSpyObj<InvoiceService>('InvoiceService', ['downloadPdf']);
    errorFormatterSpy = jasmine.createSpyObj<ErrorFormatterService>('ErrorFormatterService', ['format']);

    cartServiceSpy.getMyCart.and.returnValue(of({
      items: [
        { type: 'MERCHANDISE', merchandiseId: 1, name: 'Shirt', quantity: 2, unitPrice: 10 } as any,
        { type: 'TICKET', ticketId: 7, eventTitle: 'Concert', unitPrice: 50 } as any
      ]
    } as any));

    cartServiceSpy.checkout.and.returnValue(of({
      ticketInvoiceId: null,
      merchandiseInvoiceId: null,
      rewardInvoiceId: null
    } as any));

    invoiceSpy.downloadPdf.and.returnValue(of(new Blob(['x'], { type: 'application/pdf' })));
    routerSpy.getCurrentNavigation.and.returnValue(null);
    errorFormatterSpy.format.and.returnValue('Formatted');

    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: TicketService, useValue: jasmine.createSpyObj<TicketService>('TicketService', ['getMyTickets']) },
        { provide: InvoiceService, useValue: invoiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: ErrorFormatterService, useValue: errorFormatterSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CheckoutComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('ngOnInit should load cart and build lines', () => {
    fixture.detectChanges();

    expect(cartServiceSpy.getMyCart).toHaveBeenCalled();
    expect(component.cart).toBeTruthy();

    expect(component.merchandiseLines.length).toBe(1);
    expect(component.merchandiseLines[0].merchandiseId).toBe(1);
    expect(component.merchandiseLines[0].totalPrice).toBe(20);

    expect(component.ticketLines.length).toBe(1);
    expect(component.ticketLines[0].ticketId).toBe(7);
  });

  it('loadCart should show error + set error when cart empty', () => {
    cartServiceSpy.getMyCart.and.returnValue(of({ items: [] } as any));

    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(
      'Warenkorb ist leer, bitte fügen Sie einen Artikel hinzu!',
      'Warenkorb leer'
    );
    expect(component.error).toBe('Warenkorb ist leer, bitte fügen Sie einen Artikel hinzu!');
  });

  it('confirmCheckout should checkout, show success, navigate, and close modal', () => {
    fixture.detectChanges();

    (component as any).pendingCartItems = [{ type: 'MERCHANDISE', merchandiseId: 1, quantity: 1 } as any];
    component.showInvoiceModal = true;
    component.wantInvoice = false;

    component.confirmCheckout();

    expect(cartServiceSpy.checkout).toHaveBeenCalledWith({
      paymentMethod: component.paymentMethod,
      paymentDetail: component.paymentDetail
    });

    expect(toastrSpy.success).toHaveBeenCalledWith('Bestellung erfolgreich abgeschlossen!', 'Erfolg');
    expect(routerSpy.navigate).toHaveBeenCalledWith([''], { state: { successMessage: 'Erfolgreich bezahlt!' } });
    expect(component.showInvoiceModal).toBeFalse();
    expect((component as any).pendingCartItems.length).toBe(0);
  });

  it('confirmCheckout should download invoices when wantInvoice=true and ids returned', () => {
    fixture.detectChanges();

    cartServiceSpy.checkout.and.returnValue(of({
      ticketInvoiceId: 11,
      merchandiseInvoiceId: 22,
      rewardInvoiceId: 33
    } as any));

    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window, 'open');

    (component as any).pendingCartItems = [{ type: 'TICKET', ticketId: 7 } as any];
    component.wantInvoice = true;

    component.confirmCheckout();

    expect(invoiceSpy.downloadPdf).toHaveBeenCalledWith(11);
    expect(invoiceSpy.downloadPdf).toHaveBeenCalledWith(22);
    expect(invoiceSpy.downloadPdf).toHaveBeenCalledWith(33);
    expect(window.open).toHaveBeenCalledTimes(3);
  });

  it('confirmCheckout should show formatted error on checkout failure', () => {
    fixture.detectChanges();

    cartServiceSpy.checkout.and.returnValue(throwError(() => ({ status: 500 })));
    errorFormatterSpy.format.and.returnValue('Checkout failed');

    (component as any).pendingCartItems = [{ type: 'MERCHANDISE', merchandiseId: 1 } as any];

    component.confirmCheckout();

    expect(toastrSpy.error).toHaveBeenCalledWith(
      'Checkout failed',
      'Bezahlvorgang fehlgeschlagen',
      { enableHtml: true, timeOut: 10000 }
    );
  });

  it('submitPaypal should set method PAYPAL and open invoice modal when form valid', () => {
    fixture.detectChanges();

    component.showInvoiceModal = false;
    component.paymentMethod = PaymentMethod.CREDIT_CARD;

    const fakeForm = {
      invalid: false,
      form: { markAllAsTouched: () => {} }
    } as any;

    component.submitPaypal(fakeForm);

    expect(component.paymentMethod).toBe(PaymentMethod.PAYPAL);
    expect(component.showInvoiceModal).toBeTrue();
  });
});
