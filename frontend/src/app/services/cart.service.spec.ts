import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CartService } from './cart.service';
import { Globals } from '../global/globals';

import {
  CartDto,
  CartAddMerchandiseItemDto,
  CartCheckoutRequestDto,
  CartCheckoutResultDto,
  CartUpdateItemDto
} from '../dtos/cartDtos/cart';

describe('CartService', () => {
  let service: CartService;
  let httpMock: HttpTestingController;

  const backendUri = 'http://localhost:8080/api/v1';
  const baseUri = `${backendUri}/cart`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        {
          provide: Globals,
          useValue: { backendUri }
        }
      ]
    });

    service = TestBed.inject(CartService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMyCart() should GET /cart', () => {
    const mockCart = { items: [] } as unknown as CartDto;

    let actual: CartDto | undefined;
    service.getMyCart().subscribe(res => (actual = res));

    const req = httpMock.expectOne(baseUri);
    expect(req.request.method).toBe('GET');

    req.flush(mockCart);
    expect(actual).toEqual(mockCart);
  });

  it('addItem() should POST /cart/items with dto', () => {
    const dto: CartAddMerchandiseItemDto = {
      merchandiseId: 123,
      quantity: 2
    } as CartAddMerchandiseItemDto;

    const mockCart = { items: [] } as unknown as CartDto;

    let actual: CartDto | undefined;
    service.addItem(dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUri}/items`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush(mockCart);
    expect(actual).toEqual(mockCart);
  });

  it('updateItem() should PATCH /cart/items/:id with dto', () => {
    const cartItemId = 42;
    const dto: CartUpdateItemDto = { quantity: 5 } as CartUpdateItemDto;

    const mockCart = { items: [] } as unknown as CartDto;

    let actual: CartDto | undefined;
    service.updateItem(cartItemId, dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUri}/items/${cartItemId}`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(dto);

    req.flush(mockCart);
    expect(actual).toEqual(mockCart);
  });

  it('removeItem() should DELETE /cart/items/:id', () => {
    const cartItemId = 7;

    let completed = false;
    service.removeItem(cartItemId).subscribe({
      next: () => {},
      complete: () => (completed = true)
    });

    const req = httpMock.expectOne(`${baseUri}/items/${cartItemId}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('checkout() should POST /cart/checkout with dto', () => {
    const dto: CartCheckoutRequestDto = {
      paymentMethod: 'PAYPAL'
    } as unknown as CartCheckoutRequestDto;

    const mockResult = { invoiceId: 99 } as unknown as CartCheckoutResultDto;

    let actual: CartCheckoutResultDto | undefined;
    service.checkout(dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUri}/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush(mockResult);
    expect(actual).toEqual(mockResult);
  });

  it('addTickets() should POST /cart/tickets with ticketIds array', () => {
    const ticketIds = [1, 2, 3];
    const mockCart = { items: [] } as unknown as CartDto;

    let actual: CartDto | undefined;
    service.addTickets(ticketIds).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUri}/tickets`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(ticketIds);

    req.flush(mockCart);
    expect(actual).toEqual(mockCart);
  });

  it('removeTicket() should DELETE /cart/tickets/:ticketId', () => {
    const ticketId = 1234;

    let completed = false;
    service.removeTicket(ticketId).subscribe({
      next: () => {},
      complete: () => (completed = true)
    });

    const req = httpMock.expectOne(`${baseUri}/tickets/${ticketId}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(null);
    expect(completed).toBeTrue();
  });
});
