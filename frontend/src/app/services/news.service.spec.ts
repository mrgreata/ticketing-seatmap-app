import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NewsService } from './news.service';
import { AuthService } from './auth.service';
import { Page } from '../dtos/page';
import {CreateNewsItem} from "../dtos/newsDtos/create-news-item";
import { SimpleNewsItem } from '../dtos/newsDtos/simple-news-item';
import { DetailedNewsItem } from '../dtos/newsDtos/detailed-news-item';

describe('NewsService', () => {
  let service: NewsService;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const mockSimpleNews: SimpleNewsItem = {
    id: 1,
    title: 'Test News',
    publishedAt: '2026-01-26T12:00:00',
    summary: 'Summary',
    imageData: null
  };

  const mockDetailedNews: DetailedNewsItem = {
    id: 1,
    title: 'Test News',
    publishedAt: '2026-01-26T12:00:00',
    summary: 'Summary',
    text: 'Full text',
    imageData: [1, 2, 3],
    imageUrl: 'data:image/jpeg;base64,AQID'
  };

  const mockPage: Page<SimpleNewsItem> = {
    content: [mockSimpleNews],
    totalElements: 1,
    totalPages: 1,
    size: 12,
    number: 0,
    first: true,
    last: true,
    empty: false
  };

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'isAdmin', 'isUser', 'getUserRole']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NewsService,
        { provide: AuthService, useValue: authSpy }
      ]
    });

    service = TestBed.inject(NewsService);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });



  it('should return empty unread page if not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);

    service.getUnread(0, 12).subscribe(data => {
      expect(data.content.length).toBe(0);
    });
  });

  it('should get news by id', () => {
    service.getById(1).subscribe(data => {
      expect(data.id).toBe(1);
      expect(data.imageUrl).toContain('data:image/jpeg;base64');
    });

    const req = httpMock.expectOne('/api/v1/news/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockDetailedNews);
  });

  it('should create news if admin', () => {
    authService.getUserRole.and.returnValue('ADMIN');

    const createPayload: CreateNewsItem = {
      title: 'New News',
      summary: 'Summary',
      text: 'Text'
    };

    const createdNews: DetailedNewsItem = { ...mockDetailedNews, id: 2, title: 'New News' };

    service.createNews(createPayload).subscribe(data => {
      expect(data.id).toBe(2);
      expect(data.title).toBe('New News');
    });

    const req = httpMock.expectOne('/api/v1/news');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('New News');
    req.flush(createdNews);
  });

  it('should throw error when creating news if not admin', () => {
    authService.getUserRole.and.returnValue('USER');

    const createPayload: CreateNewsItem = { title: 'New', summary: 'Sum', text: 'Text' };

    expect(() => service.createNews(createPayload)).toThrowError('Nur Administratoren können News erstellen');
  });


  it('should delete news if admin', () => {
    authService.getUserRole.and.returnValue('ADMIN');

    service.deleteNews(1).subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne('/api/v1/news/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should upload image', () => {
    const file = new File(['image'], 'test.jpg', { type: 'image/jpeg' });

    service.uploadImage(1, file).subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne('/api/v1/news/1/image');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(null);
  });

  it('should delete image', () => {
    service.deleteImage(1).subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne('/api/v1/news/1/image');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should generate correct image URL', () => {
    const url = service.getImageUrl(1);
    expect(url).toBe('/api/v1/news/1/image');
  });
});
