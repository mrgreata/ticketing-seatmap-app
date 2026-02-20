import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { MessageService } from './message.service';
import { Globals } from '../global/globals';
import { Message } from '../dtos/message';

describe('MessageService', () => {
  let service: MessageService;
  let httpMock: HttpTestingController;

  const globalsMock = { backendUri: 'http://localhost:8080/api/v1' } as Globals;
  const base = 'http://localhost:8080/api/v1/messages';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MessageService,
        { provide: Globals, useValue: globalsMock },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MessageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMessage should GET /messages', () => {
    const mockMessages: Message[] = [
      { id: 1, title: 'Hello', summary: 'S1', text: 'T1', publishedAt: new Date() } as any
    ];

    service.getMessage().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res[0].id).toBe(1);
      expect(res[0].title).toBe('Hello');
    });

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');

    req.flush(mockMessages);
  });

  it('getMessageById should GET /messages/:id', () => {
    const id = 7;
    const mockMessage: Message = { id, title: 'Detail', summary: 'S', text: 'T', publishedAt: new Date() } as any;

    service.getMessageById(id).subscribe(res => {
      expect(res.id).toBe(7);
      expect(res.title).toBe('Detail');
    });

    const req = httpMock.expectOne(`${base}/${id}`);
    expect(req.request.method).toBe('GET');

    req.flush(mockMessage);
  });

  it('createMessage should POST /messages with payload', () => {
    const payload: Message = { title: 'New', summary: 'S', text: 'T', publishedAt: new Date() } as any;
    const returned: Message = { ...payload, id: 99 } as any;

    service.createMessage(payload).subscribe(res => {
      expect(res.id).toBe(99);
      expect(res.title).toBe('New');
    });

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush(returned);
  });
});
