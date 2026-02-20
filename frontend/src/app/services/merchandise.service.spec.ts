import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { MerchandiseService } from './merchandise.service';

import { Merchandise } from '../dtos/merchandiseDtos/merchandise';
import { MerchandisePurchaseRequestDto } from '../dtos/merchandiseDtos/merchandise-purchase';
import { MerchandiseCreateDto } from '../dtos/merchandiseDtos/merchandise-create';
import { RewardRedeemRequestDto } from '../dtos/merchandiseDtos/reward-redeem';

describe('MerchandiseService', () => {
  let service: MerchandiseService;
  let httpMock: HttpTestingController;

  const baseUrl = 'http://localhost:8080/api/v1/merchandise';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(MerchandiseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllMerchandise() should GET /merchandise', () => {
    const mock: Merchandise[] = [{ id: 1 } as unknown as Merchandise];

    let actual: Merchandise[] | undefined;
    service.getAllMerchandise().subscribe(res => (actual = res));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');

    req.flush(mock);
    expect(actual).toEqual(mock);
  });

  it('purchase() should POST /merchandise/purchase with request body', () => {
    const dto: MerchandisePurchaseRequestDto = {
      merchandiseId: 1,
      quantity: 2
    } as unknown as MerchandisePurchaseRequestDto;

    let actual: unknown;
    service.purchase(dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUrl}/purchase`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush({ ok: true });
    expect(actual).toEqual({ ok: true });
  });

  it('createMerchandise() should POST /merchandise with dto', () => {
    const dto: MerchandiseCreateDto = {
      name: 'Shirt',
      description: 'Nice',
      unitPrice: 19.99
    } as unknown as MerchandiseCreateDto;

    const mockCreated = { id: 10, name: 'Shirt' } as unknown as Merchandise;

    let actual: Merchandise | undefined;
    service.createMerchandise(dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush(mockCreated);
    expect(actual).toEqual(mockCreated);
  });

  it('getRewardMerchandise() should GET /merchandise/rewards', () => {
    const mock: Merchandise[] = [{ id: 2 } as unknown as Merchandise];

    let actual: Merchandise[] | undefined;
    service.getRewardMerchandise().subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUrl}/rewards`);
    expect(req.request.method).toBe('GET');

    req.flush(mock);
    expect(actual).toEqual(mock);
  });

  it('deleteMerchandise() should DELETE /merchandise/:id', () => {
    const id = 123;

    let completed = false;
    service.deleteMerchandise(id).subscribe({
      next: () => {},
      complete: () => (completed = true)
    });

    const req = httpMock.expectOne(`${baseUrl}/${id}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('getMerchandiseById() should GET /merchandise/:id', () => {
    const id = 5;
    const mock = { id: 5, name: 'Cap' } as unknown as Merchandise;

    let actual: Merchandise | undefined;
    service.getMerchandiseById(id).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUrl}/${id}`);
    expect(req.request.method).toBe('GET');

    req.flush(mock);
    expect(actual).toEqual(mock);
  });

  it('redeemRewards() should POST /merchandise/rewards/redeem with request body', () => {
    const dto: RewardRedeemRequestDto = {
      rewardMerchandiseId: 7,
      quantity: 1
    } as unknown as RewardRedeemRequestDto;

    let actual: unknown;
    service.redeemRewards(dto).subscribe(res => (actual = res));

    const req = httpMock.expectOne(`${baseUrl}/rewards/redeem`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);

    req.flush({ success: true });
    expect(actual).toEqual({ success: true });
  });

  it('uploadImage() should POST /merchandise/:id/image as FormData containing "image"', () => {
    const id = 9;

    const file = new File(['dummy'], 'test.webp', { type: 'image/webp' });

    let completed = false;
    service.uploadImage(id, file).subscribe({
      next: () => {},
      complete: () => (completed = true)
    });

    const req = httpMock.expectOne(`${baseUrl}/${id}/image`);
    expect(req.request.method).toBe('POST');

    expect(req.request.body instanceof FormData).toBeTrue();
    const formData = req.request.body as FormData;
    expect(formData.get('image')).toBe(file);

    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('getImageUrl() should return /merchandise/:id/image', () => {
    expect(service.getImageUrl(42)).toBe(`${baseUrl}/42/image`);
  });

  describe('validateImageFile()', () => {
    it('should return null for allowed type and <= 3MB', () => {
      const file = new File(['a'], 'ok.png', { type: 'image/png' });
      Object.defineProperty(file, 'size', { value: 3 * 1024 * 1024 });
      expect(service.validateImageFile(file)).toBeNull();
    });

    it('should reject invalid mime type', () => {
      const file = new File(['a'], 'bad.gif', { type: 'image/gif' });
      Object.defineProperty(file, 'size', { value: 1000 });
      expect(service.validateImageFile(file)).toBe('Ungültiger Dateityp. Erlaubt: PNG, JPG, WEBP.');
    });

    it('should reject file larger than 3MB', () => {
      const file = new File(['a'], 'big.jpg', { type: 'image/jpeg' });
      Object.defineProperty(file, 'size', { value: 3 * 1024 * 1024 + 1 });
      expect(service.validateImageFile(file)).toBe('Bild ist zu groß (max. 3MB).');
    });
  });
});
