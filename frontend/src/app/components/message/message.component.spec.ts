import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageComponent } from './message.component';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from '../../services/message.service';
import { AuthService } from '../../services/auth.service';
import { of } from 'rxjs';

describe('MessageComponent', () => {
  let component: MessageComponent;
  let fixture: ComponentFixture<MessageComponent>;

  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    messageServiceSpy = jasmine.createSpyObj<MessageService>(
      'MessageService',
      ['getMessage', 'createMessage']
    );

    messageServiceSpy.getMessage.and.returnValue(of([]));
    messageServiceSpy.createMessage.and.returnValue(of({
      id: 1,
      title: 'Test',
      summary: 'Kurz',
      text: 'Text',
      publishedAt: '2025-01-01T00:00:00Z'
    }));

    authServiceSpy = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['getUserRole']
    );
    authServiceSpy.getUserRole.and.returnValue('USER');

    await TestBed.configureTestingModule({
      declarations: [MessageComponent],
      imports: [
        ReactiveFormsModule,
        RouterTestingModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
