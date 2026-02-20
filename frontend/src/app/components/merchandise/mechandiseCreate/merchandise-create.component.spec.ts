import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { MerchandiseCreateComponent } from './merchandise-create.component';
import { MerchandiseService } from '../../../services/merchandise.service';
import { AuthService } from '../../../services/auth.service';
import { ToastrService } from 'ngx-toastr';

describe('MerchandiseCreateComponent', () => {
  let component: MerchandiseCreateComponent;
  let fixture: ComponentFixture<MerchandiseCreateComponent>;

  let merchandiseServiceSpy: jasmine.SpyObj<MerchandiseService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let router: Router;

  beforeEach(async () => {
    merchandiseServiceSpy = jasmine.createSpyObj<MerchandiseService>('MerchandiseService', [
      'validateImageFile',
      'createMerchandise',
      'uploadImage'
    ]);

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAdmin']);

    toastrSpy = jasmine.createSpyObj<ToastrService>('ToastrService', [
      'error',
      'warning',
      'success'
    ]);

    authServiceSpy.isAdmin.and.returnValue(true);

    merchandiseServiceSpy.validateImageFile.and.returnValue(null);
    merchandiseServiceSpy.createMerchandise.and.returnValue(of({ id: 1 } as any));
    merchandiseServiceSpy.uploadImage.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [MerchandiseCreateComponent],
      providers: [
        { provide: MerchandiseService, useValue: merchandiseServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(MerchandiseCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and set isAdmin from AuthService in constructor', () => {
    expect(component).toBeTruthy();
    expect(authServiceSpy.isAdmin).toHaveBeenCalled();
    expect(component.isAdmin).toBeTrue();
  });

  describe('reset()', () => {
    it('should reset merch to empty dto', () => {
      component.merch.name = 'X';
      component.merch.description = 'Y';

      component.reset();

      expect(component.merch.name).toBe('');
      expect(component.merch.description).toBe('');
      expect(component.merch.redeemableWithPoints).toBeFalse();
    });
  });

  describe('goBack()', () => {
    it('should navigate back to /merchandise', async () => {
      await component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(['/merchandise']);
    });
  });

  describe('removeImage()', () => {
    it('should clear selectedImageFile and previewUrl', () => {
      component.selectedImageFile = {} as any;
      component.previewUrl = 'data:...';

      component.removeImage();

      expect(component.selectedImageFile).toBeNull();
      expect(component.previewUrl).toBeNull();
    });
  });

  describe('onFileSelected()', () => {
    it('should show validation error and remove image when validateImageFile returns error', () => {
      merchandiseServiceSpy.validateImageFile.and.returnValue('Bad file');

      component.selectedImageFile = {} as any;
      component.previewUrl = 'data:old';

      const fakeFile = new File(['x'], 'x.png', { type: 'image/png' });
      const event = { target: { files: [fakeFile] } } as any as Event;

      component.onFileSelected(event);

      expect(merchandiseServiceSpy.validateImageFile).toHaveBeenCalledWith(fakeFile);
      expect(toastrSpy.error).toHaveBeenCalledWith('Bad file', 'Bild');
      expect(component.selectedImageFile).toBeNull();
      expect(component.previewUrl).toBeNull();
    });

    it('should store selected file and set previewUrl via FileReader on success', () => {
      const originalFR = (window as any).FileReader;

      class FakeFileReader {
        public result: string | ArrayBuffer | null = null;
        public onload: ((e: ProgressEvent<FileReader>) => any) | null = null;

        readAsDataURL(_file: File) {
          this.result = 'data:image/png;base64,AAA';
          if (this.onload) {
            this.onload({ target: this } as any);
          }
        }
      }

      (window as any).FileReader = FakeFileReader;

      try {
        const fakeFile = new File(['x'], 'x.png', { type: 'image/png' });
        const event = { target: { files: [fakeFile] } } as any as Event;

        component.onFileSelected(event);

        expect(merchandiseServiceSpy.validateImageFile).toHaveBeenCalledWith(fakeFile);
        expect(component.selectedImageFile).toBe(fakeFile);
        expect(component.previewUrl).toBe('data:image/png;base64,AAA');
      } finally {
        (window as any).FileReader = originalFR;
      }
    });
  });

  describe('onSubmit()', () => {
    function fillValidMerch() {
      component.merch.name = 'Valid Name';
      component.merch.description = 'Valid Desc';
      component.merch.unitPrice = 10 as any;
      component.merch.remainingQuantity = 5 as any;
      component.merch.rewardPointsPerUnit = 2 as any;
      component.merch.redeemableWithPoints = false;
      component.merch.pointsPrice = null as any;
    }

    it('should reject when not admin', () => {
      component.isAdmin = false;

      component.onSubmit();

      expect(toastrSpy.error).toHaveBeenCalledWith(
        'Keine Berechtigung zum Erstellen von Merchandise-Artikeln.',
        'Fehler'
      );
      expect(merchandiseServiceSpy.createMerchandise).not.toHaveBeenCalled();
    });

    it('should do nothing when already loading', () => {
      component.isAdmin = true;
      component.isLoading = true;

      component.onSubmit();

      expect(merchandiseServiceSpy.createMerchandise).not.toHaveBeenCalled();
    });

    it('should validate required fields (name/description)', () => {
      component.isAdmin = true;
      component.merch.name = '   ';
      component.merch.description = '';

      component.onSubmit();

      expect(toastrSpy.warning).toHaveBeenCalledWith('Bitte Name und Beschreibung ausfüllen.', 'Validierung');
      expect(merchandiseServiceSpy.createMerchandise).not.toHaveBeenCalled();
    });

    it('should validate max name length', () => {
      component.isAdmin = true;
      component.merch.name = 'a'.repeat(51);
      component.merch.description = 'ok';

      component.onSubmit();

      expect(toastrSpy.warning).toHaveBeenCalledWith('Name darf maximal 50 Zeichen lang sein.', 'Validierung');
    });

    it('should validate max description length', () => {
      component.isAdmin = true;
      component.merch.name = 'ok';
      component.merch.description = 'a'.repeat(251);

      component.onSubmit();

      expect(toastrSpy.warning).toHaveBeenCalledWith('Beschreibung darf maximal 250 Zeichen lang sein.', 'Validierung');
    });

    it('should validate non-negative numeric fields', () => {
      component.isAdmin = true;
      fillValidMerch();
      component.merch.unitPrice = -1 as any;

      component.onSubmit();

      expect(toastrSpy.warning).toHaveBeenCalledWith('Werte dürfen nicht negativ sein.', 'Validierung');
    });

    it('should set pointsPrice=0 when redeemableWithPoints is false', () => {
      component.isAdmin = true;
      fillValidMerch();
      component.merch.redeemableWithPoints = false;
      component.merch.pointsPrice = 123 as any;

      component.onSubmit();

      expect(merchandiseServiceSpy.createMerchandise).toHaveBeenCalled();
      expect(component.merch.pointsPrice).toBe(0 as any);
    });

    it('should validate pointsPrice non-negative when redeemableWithPoints is true', () => {
      component.isAdmin = true;
      fillValidMerch();
      component.merch.redeemableWithPoints = true;
      component.merch.pointsPrice = -1 as any;

      component.onSubmit();

      expect(toastrSpy.warning).toHaveBeenCalledWith('Punktepreis darf nicht negativ sein.', 'Validierung');
      expect(merchandiseServiceSpy.createMerchandise).not.toHaveBeenCalled();
    });

    it('should create merchandise and navigate with state when no image selected', async () => {
      component.isAdmin = true;
      fillValidMerch();
      component.selectedImageFile = null;

      component.onSubmit();

      expect(merchandiseServiceSpy.createMerchandise).toHaveBeenCalled();
      expect(toastrSpy.success).toHaveBeenCalledWith('Neuer Artikel erfolgreich angelegt!', 'Merchandise');

      expect(router.navigate).toHaveBeenCalledWith(['/merchandise'], {
        state: { successMessage: 'Neuer Artikel erfolgreich angelegt!' }
      });

      expect(component.isLoading).toBeFalse();
      expect(merchandiseServiceSpy.uploadImage).not.toHaveBeenCalled();
    });

    it('should upload image after create and then finish (success)', () => {
      component.isAdmin = true;
      fillValidMerch();
      component.selectedImageFile = new File(['x'], 'x.png', { type: 'image/png' });

      component.onSubmit();

      expect(merchandiseServiceSpy.createMerchandise).toHaveBeenCalled();
      expect(merchandiseServiceSpy.uploadImage).toHaveBeenCalledWith(1, component.selectedImageFile as File);

      expect(toastrSpy.success).toHaveBeenCalledWith('Neuer Artikel erfolgreich angelegt!', 'Merchandise');
      expect(router.navigate).toHaveBeenCalled();
      expect(component.isLoading).toBeFalse();
    });

    it('should show upload error if image upload fails (and not navigate)', () => {
      component.isAdmin = true;
      fillValidMerch();
      component.selectedImageFile = new File(['x'], 'x.png', { type: 'image/png' });

      merchandiseServiceSpy.uploadImage.and.returnValue(
        throwError(() => ({ error: { message: 'Upload failed' } }))
      );

      component.onSubmit();

      expect(merchandiseServiceSpy.createMerchandise).toHaveBeenCalled();
      expect(merchandiseServiceSpy.uploadImage).toHaveBeenCalled();

      expect(toastrSpy.error).toHaveBeenCalledWith('Upload failed', 'Merchandise');
      expect(router.navigate).not.toHaveBeenCalled();
      expect(component.isLoading).toBeFalse();
    });

    it('should show create error message on create failure', () => {
      component.isAdmin = true;
      fillValidMerch();

      merchandiseServiceSpy.createMerchandise.and.returnValue(
        throwError(() => ({ error: { message: 'Create failed' } }))
      );

      component.onSubmit();

      expect(toastrSpy.error).toHaveBeenCalledWith('Create failed', 'Merchandise');
      expect(component.isLoading).toBeFalse();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('onlyNumbers()', () => {
    it('should allow control chars', () => {
      const e = { which: 8, preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyNumbers(e)).toBeTrue();
    });

    it('should block non-digits and call preventDefault', () => {
      const e = { which: 65, preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyNumbers(e)).toBeFalse();
      expect((e as any).preventDefault).toHaveBeenCalled();
    });

    it('should allow digits', () => {
      const e = { which: 50, preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyNumbers(e)).toBeTrue();
      expect((e as any).preventDefault).not.toHaveBeenCalled();
    });
  });

  describe('onlyPriceChars()', () => {
    it('should allow navigation/control keys', () => {
      const e = { key: 'Backspace', preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyPriceChars(e)).toBeTrue();
    });

    it('should allow digits', () => {
      const e = { key: '7', preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyPriceChars(e)).toBeTrue();
      expect((e as any).preventDefault).not.toHaveBeenCalled();
    });

    it('should allow one decimal separator but block the second', () => {
      component.merch.unitPrice = '12.3' as any;

      const e1 = { key: '.', preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyPriceChars(e1)).toBeFalse();
      expect((e1 as any).preventDefault).toHaveBeenCalled();

      component.merch.unitPrice = '12' as any;
      const e2 = { key: '.', preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyPriceChars(e2)).toBeTrue();
    });

    it('should block other chars', () => {
      const e = { key: 'x', preventDefault: jasmine.createSpy('preventDefault') } as any as KeyboardEvent;
      expect(component.onlyPriceChars(e)).toBeFalse();
      expect((e as any).preventDefault).toHaveBeenCalled();
    });
  });
});
