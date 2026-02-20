import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { MerchandiseInvoicesComponent } from './merchandise-invoices.component';
import { InvoiceService } from '../../../services/invoice.service';
import { AuthService } from '../../../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { SimpleInvoice } from '../../../dtos/invoice';

describe('MerchandiseInvoicesComponent', () => {
  let component: MerchandiseInvoicesComponent;
  let fixture: ComponentFixture<MerchandiseInvoicesComponent>;

  let invoiceServiceSpy: jasmine.SpyObj<InvoiceService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  beforeEach(async () => {
    invoiceServiceSpy = jasmine.createSpyObj<InvoiceService>('InvoiceService', [
      'getMyMerchandiseInvoices',
      'openPdf'
    ]);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn',
      'isAdmin'
    ]);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', ['error']);

    await TestBed.configureTestingModule({
      imports: [MerchandiseInvoicesComponent],
      providers: [
        { provide: InvoiceService, useValue: invoiceServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MerchandiseInvoicesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('ngOnInit should call loadMyMerchandiseInvoices (via getMyMerchandiseInvoices) when logged in and not admin', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    invoiceServiceSpy.getMyMerchandiseInvoices.and.returnValue(of([]));

    fixture.detectChanges();

    expect(invoiceServiceSpy.getMyMerchandiseInvoices).toHaveBeenCalledTimes(1);
    expect(component.merchInvoicesLoading).toBeFalse();
    expect(component.merchInvoices).toEqual([]);
  });

  it('should early-return and clear state when not logged in', () => {
    component.merchInvoices = [{ id: 1 } as unknown as SimpleInvoice];
    component.merchInvoicesError = 'x';
    component.selectedInvoiceIds.add(123);

    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    fixture.detectChanges();

    expect(invoiceServiceSpy.getMyMerchandiseInvoices).not.toHaveBeenCalled();
    expect(component.merchInvoices).toEqual([]);
    expect(component.merchInvoicesError).toBeUndefined();
    expect(component.selectedInvoiceIds.size).toBe(0);
  });

  it('should early-return and clear state when admin', () => {
    component.merchInvoices = [{ id: 1 } as unknown as SimpleInvoice];
    component.merchInvoicesError = 'x';
    component.selectedInvoiceIds.add(123);

    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(true);

    fixture.detectChanges();

    expect(invoiceServiceSpy.getMyMerchandiseInvoices).not.toHaveBeenCalled();
    expect(component.merchInvoices).toEqual([]);
    expect(component.merchInvoicesError).toBeUndefined();
    expect(component.selectedInvoiceIds.size).toBe(0);
  });

  it('should set loading true then populate invoices on success', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    const mockInvoices = [
      { id: 10 } as unknown as SimpleInvoice,
      { id: 11 } as unknown as SimpleInvoice
    ];

    invoiceServiceSpy.getMyMerchandiseInvoices.and.returnValue(of(mockInvoices));

    fixture.detectChanges();

    expect(component.merchInvoicesLoading).toBeFalse();
    expect(component.merchInvoicesError).toBeUndefined();
    expect(component.merchInvoices).toEqual(mockInvoices);
    expect(component.selectedInvoiceIds.size).toBe(0);
  });

  it('should set error message and stop loading on failure', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    invoiceServiceSpy.getMyMerchandiseInvoices.and.returnValue(
      throwError(() => new Error('boom'))
    );

    fixture.detectChanges();

    expect(component.merchInvoicesLoading).toBeFalse();
    expect(component.merchInvoicesError).toBe('Bestellungen konnten nicht geladen werden.');
  });

  it('reloadMerchandiseInvoices should trigger another load', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    invoiceServiceSpy.getMyMerchandiseInvoices.and.returnValue(of([]));

    fixture.detectChanges();
    component.reloadMerchandiseInvoices();

    expect(invoiceServiceSpy.getMyMerchandiseInvoices).toHaveBeenCalledTimes(2);
  });

  it('toggleInvoiceSelection should add/remove ids based on checked', () => {
    component.toggleInvoiceSelection(1, true);
    component.toggleInvoiceSelection(2, true);
    expect(component.selectedInvoiceIds.has(1)).toBeTrue();
    expect(component.selectedInvoiceIds.has(2)).toBeTrue();

    component.toggleInvoiceSelection(1, false);
    expect(component.selectedInvoiceIds.has(1)).toBeFalse();
    expect(component.selectedInvoiceIds.has(2)).toBeTrue();
  });

  it('openSelectedInvoices should show toastr error if nothing selected', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);
    fixture.detectChanges();

    component.openSelectedInvoices();

    expect(toastrSpy.error).toHaveBeenCalledWith(
      'Bitte wählen Sie mindestens eine Rechnung aus.',
      'Rechnung'
    );
    expect(invoiceServiceSpy.openPdf).not.toHaveBeenCalled();
  });

  it('openSelectedInvoices should call openPdf for each selected invoice id', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);
    fixture.detectChanges();

    component.selectedInvoiceIds.add(5);
    component.selectedInvoiceIds.add(9);

    component.openSelectedInvoices();

    expect(toastrSpy.error).not.toHaveBeenCalled();
    expect(invoiceServiceSpy.openPdf).toHaveBeenCalledTimes(2);
    expect(invoiceServiceSpy.openPdf).toHaveBeenCalledWith(5);
    expect(invoiceServiceSpy.openPdf).toHaveBeenCalledWith(9);
  });
});
