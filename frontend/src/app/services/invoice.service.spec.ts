import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { InvoiceService } from './invoice.service';
import { AuthService } from './auth.service';
import { InvoiceCreate, SimpleInvoice, DetailedInvoice } from '../dtos/invoice';

describe('InvoiceService', () => {
  let service: InvoiceService;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const baseUri = '/api/v1/invoices';

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['isLoggedIn']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        InvoiceService,
        { provide: AuthService, useValue: authService }
      ]
    });

    service = TestBed.inject(InvoiceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // --------------------------------------------------
  // GET my invoices
  // --------------------------------------------------

  it('should return empty array if not logged in (getMyInvoices)', () => {
    authService.isLoggedIn.and.returnValue(false);

    service.getMyInvoices().subscribe(res => {
      expect(res).toEqual([]);
    });

    httpMock.expectNone(`${baseUri}/my`);
  });

  it('should get my invoices if logged in', () => {
    authService.isLoggedIn.and.returnValue(true);

    const mockInvoices: SimpleInvoice[] = [
      { id: 1, invoiceNumber: 'INV-1', userId: 5 }
    ];

    service.getMyInvoices().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].invoiceNumber).toBe('INV-1');
    });

    const req = httpMock.expectOne(`${baseUri}/my`);
    expect(req.request.method).toBe('GET');
    req.flush(mockInvoices);
  });

  it('should return empty array on error (getMyInvoices)', () => {
    authService.isLoggedIn.and.returnValue(true);

    service.getMyInvoices().subscribe(res => {
      expect(res).toEqual([]);
    });

    const req = httpMock.expectOne(`${baseUri}/my`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });
  });

  // --------------------------------------------------
  // GET by id
  // --------------------------------------------------

  it('should return null if not logged in (getById)', () => {
    authService.isLoggedIn.and.returnValue(false);

    service.getById(10).subscribe(res => {
      expect(res).toBeNull();
    });

    httpMock.expectNone(`${baseUri}/10`);
  });

  it('should get invoice by id if logged in', () => {
    authService.isLoggedIn.and.returnValue(true);

    const mockInvoice: DetailedInvoice = {
      id: 10,
      invoiceNumber: 'INV-10',
      userId: 3,
      invoiceCancellationDate: '',
      tickets: []
    };

    service.getById(10).subscribe(res => {
      expect(res?.id).toBe(10);
    });

    const req = httpMock.expectOne(`${baseUri}/10`);
    expect(req.request.method).toBe('GET');
    req.flush(mockInvoice);
  });

  it('should return null on error (getById)', () => {
    authService.isLoggedIn.and.returnValue(true);

    service.getById(10).subscribe(res => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne(`${baseUri}/10`);
    req.flush('Error', { status: 403, statusText: 'Forbidden' });
  });

  // --------------------------------------------------
  // CREATE
  // --------------------------------------------------

  it('should not create invoice if not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);

    service.create({} as InvoiceCreate).subscribe(res => {
      expect(res).toBeNull();
    });

    httpMock.expectNone(baseUri);
  });

  it('should create invoice if logged in', () => {
    authService.isLoggedIn.and.returnValue(true);

    const dto: InvoiceCreate = {
      invoiceNumber: 'INV-1',
      invoiceDate: '2026-01-01',
      userId: 1,
      userName: 'Max',
      userAddress: 'Teststreet',
      eventDate: '2026-01-10',
      ticketIds: [1, 2]
    };

    service.create(dto).subscribe(res => {
      expect(res?.invoiceNumber).toBe('INV-1');
    });

    const req = httpMock.expectOne(baseUri);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush({ id: 1, invoiceNumber: 'INV-1', userId: 1 });
  });

  // --------------------------------------------------
  // DOWNLOAD PDF
  // --------------------------------------------------

  it('should not download pdf if not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);

    service.downloadPdf(5).subscribe(res => {
      expect(res).toBeNull();
    });

    httpMock.expectNone(`${baseUri}/5/download`);
  });

  it('should download pdf if logged in', () => {
    authService.isLoggedIn.and.returnValue(true);

    const blob = new Blob(['PDF'], { type: 'application/pdf' });

    service.downloadPdf(5).subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(`${baseUri}/5/download`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');

    req.flush(blob);
  });

  // --------------------------------------------------
  // CREDIT PDF
  // --------------------------------------------------

  it('should download credit pdf', () => {
    const blob = new Blob(['PDF'], { type: 'application/pdf' });

    service.downloadCreditPdf([1, 2]).subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(`${baseUri}/credit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual([1, 2]);
    expect(req.request.responseType).toBe('blob');

    req.flush(blob);
  });

  it('should download credit pdf by id', () => {
    const blob = new Blob(['PDF'], { type: 'application/pdf' });

    service.downloadCreditPdfById(99).subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(`${baseUri}/credit/99/download`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');

    req.flush(blob);
  });

  // --------------------------------------------------
  // OPEN PDF
  // --------------------------------------------------

  it('should open pdf in new tab', () => {
    authService.isLoggedIn.and.returnValue(true);

    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window, 'open');

    const blob = new Blob(['PDF'], { type: 'application/pdf' });

    service.openPdf(5);

    const req = httpMock.expectOne(`${baseUri}/5/download`);
    req.flush(blob);

    expect(window.URL.createObjectURL).toHaveBeenCalled();
    expect(window.open).toHaveBeenCalledWith('blob:url');
  });
});
