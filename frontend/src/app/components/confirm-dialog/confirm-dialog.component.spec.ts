import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent } from './confirm-dialog.component';
import { CommonModule } from '@angular/common';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfirmDialogComponent],
      imports: [CommonModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;

    component.title = 'Benutzer entsperren';
    component.message = 'Möchten Sie den Benutzer test@test.com wirklich entsperren?';
    component.confirmText = 'Entsperren';
    component.cancelText = 'Abbrechen';
    component.confirmButtonClass = 'btn-warning';

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit confirm event when confirm button is clicked', () => {
    spyOn(component.confirm, 'emit');

    const confirmButton: HTMLButtonElement =
      fixture.nativeElement.querySelector('.btn-warning');

    confirmButton.click();

    expect(component.confirm.emit).toHaveBeenCalled();
  });
});
