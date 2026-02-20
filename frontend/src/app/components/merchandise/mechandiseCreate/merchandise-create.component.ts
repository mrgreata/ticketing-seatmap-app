import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { MerchandiseService } from '../../../services/merchandise.service';
import { MerchandiseCreateDto } from '../../../dtos/merchandiseDtos/merchandise-create';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-merchandise-create',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './merchandise-create.component.html',
  styleUrls: ['./merchandise-create.component.scss']
})
export class MerchandiseCreateComponent {

  isAdmin = false;
  isLoading = false;

  merch: MerchandiseCreateDto = this.emptyDto();

  selectedImageFile: File | null = null;
  previewUrl: string | null = null;

  constructor(
    private merchandiseService: MerchandiseService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
  ) {
    this.isAdmin = this.authService.isAdmin();
  }

  private emptyDto(): MerchandiseCreateDto {
    return {
      description: '',
      name: '',
      unitPrice: null as any,
      rewardPointsPerUnit: null as any,
      remainingQuantity: null as any,
      redeemableWithPoints: false,
      pointsPrice: null as any
    };
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const validationError = this.merchandiseService.validateImageFile(file);
      if (validationError) {
        this.toastr.error(validationError, 'Bild');
        this.removeImage();
        return;
      }

      this.selectedImageFile = file;

      const reader = new FileReader();
      reader.onload = (e: ProgressEvent<FileReader>) => {
        this.previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.previewUrl = null;
  }


  onSubmit(): void {
    if (!this.isAdmin) {
      this.toastr.error('Keine Berechtigung zum Erstellen von Merchandise-Artikeln.', 'Fehler');
      return;
    }

    if (this.isLoading) return;

    const name = (this.merch.name ?? '').trim();
    const desc = (this.merch.description ?? '').trim();

    if (!name || !desc) {
      this.toastr.warning('Bitte Name und Beschreibung ausfüllen.', 'Validierung');
      return;
    }

    if (name.length > 50) {
      this.toastr.warning('Name darf maximal 50 Zeichen lang sein.', 'Validierung');
      return;
    }

    if (desc.length > 250) {
      this.toastr.warning('Beschreibung darf maximal 250 Zeichen lang sein.', 'Validierung');
      return;
    }

    if (this.merch.unitPrice < 0 || this.merch.remainingQuantity < 0 || this.merch.rewardPointsPerUnit < 0) {
      this.toastr.warning('Werte dürfen nicht negativ sein.', 'Validierung');
      return;
    }

    if (!this.merch.redeemableWithPoints) {
      this.merch.pointsPrice = 0;
    } else {
      if (this.merch.pointsPrice < 0) {
        this.toastr.warning('Punktepreis darf nicht negativ sein.', 'Validierung');
        return;
      }
    }

    const MAX_INT_7 = 9_999_999;
    const MAX_PRICE = 9_999_999.99;

    if (this.merch.unitPrice > MAX_PRICE) {
      this.toastr.warning('Preis darf maximal 9.999.999,99 € betragen.', 'Validierung');
      return;
    }

    if (this.merch.remainingQuantity > MAX_INT_7) {
      this.toastr.warning('Verfügbare Menge darf maximal 9.999.999 betragen.', 'Validierung');
      return;
    }

    if (this.merch.rewardPointsPerUnit > MAX_INT_7) {
      this.toastr.warning('Reward Points / Stück dürfen maximal 9.999.999 betragen.', 'Validierung');
      return;
    }

    if (this.merch.redeemableWithPoints && this.merch.pointsPrice > MAX_INT_7) {
      this.toastr.warning('Punktepreis darf maximal 9.999.999 betragen.', 'Validierung');
      return;
    }

    this.merch.name = name;
    this.merch.description = desc;

    this.isLoading = true;

    this.merchandiseService.createMerchandise(this.merch).subscribe({
      next: (created) => {
        const finish = () => {
          this.isLoading = false;
          this.toastr.success('Neuer Artikel erfolgreich angelegt!', 'Merchandise');
          this.router.navigate(['/merchandise'], {
            state: { successMessage: 'Neuer Artikel erfolgreich angelegt!' }
          });
        };

        if (this.selectedImageFile) {
          this.merchandiseService.uploadImage(created.id, this.selectedImageFile).subscribe({
            next: () => finish(),
            error: (err) => {
              this.isLoading = false;
              this.toastr.error(err?.error?.message ?? 'Bild konnte nicht hochgeladen werden.', 'Merchandise');
            }
          });
        } else {
          finish();
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.toastr.error(err?.error?.message ?? 'Anlegen fehlgeschlagen!', 'Merchandise');
      }
    });
  }

  reset(): void {
    this.merch = this.emptyDto();
  }

  goBack(): void {
    this.router.navigate(['/merchandise']);
  }

  onlyNumbers(event: KeyboardEvent): boolean {
    const charCode = event.which ? event.which : event.keyCode;

    if (charCode <= 31) return true;

    if (charCode < 48 || charCode > 57) {
      event.preventDefault();
      return false;
    }
    return true;
  }

  onlyPriceChars(event: KeyboardEvent): boolean {
    const key = event.key;

    if (['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','Home','End'].includes(key)) {
      return true;
    }

    if (/^\d$/.test(key)) return true;

    if (key === ',' || key === '.') {
      const current = String(this.merch.unitPrice ?? '');
      if (current.includes(',') || current.includes('.')) {
        event.preventDefault();
        return false;
      }
      return true;
    }

    event.preventDefault();
    return false;
  }
}
