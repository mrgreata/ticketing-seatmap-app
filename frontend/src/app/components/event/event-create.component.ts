import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {ToastrService} from 'ngx-toastr';
import {EventService} from '../../services/event.service';
import {LocationService} from '../../services/location.service';
import {ArtistService} from '../../services/artist.service';
import {AuthService} from '../../services/auth.service';
import {Location} from '../../dtos/location';
import {Artist} from '../../dtos/artist';

@Component({
  selector: 'app-event-create',
  standalone: true,
  templateUrl: './event-create.component.html',
  styleUrls: ['./event-create.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule]
})
export class EventCreateComponent implements OnInit {

  eventTypes = ['Konzert', 'Oper', 'Musical', 'Festival', 'Kino'];

  eventForm: FormGroup;

  locations: Location[] = [];
  artists: Artist[] = [];
  selectedArtists: Artist[] = [];

  artistSearchTerm: string = '';
  filteredArtists: Artist[] = [];
  showArtistDropdown: boolean = false;

  isLoading = false;
  isAuthenticated = false;
  isAdmin = false;

  selectedImageFile: File | null = null;
  previewImageUrl: string | null = null;
  minDateTime: string = '';
  imageValidationError: string | null = null;

  private readonly MAX_IMAGE_SIZE = 3 * 1024 * 1024;
  private readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  constructor(
    private fb: FormBuilder,
    private eventService: EventService,
    private locationService: LocationService,
    private artistService: ArtistService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.eventForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(50)]],
      type: ['', [Validators.required, Validators.maxLength(100)]],
      durationMinutes: [null, [Validators.required, Validators.min(1), Validators.max(9999)]],
      description: ['', Validators.maxLength(800)],
      dateTime: ['', [Validators.required, this.futureDateValidator()]],
      locationId: [null, Validators.required],
      artistIds: [[]]
    });
  }

  ngOnInit() {
    this.checkAuthStatus();
    this.loadLocations();
    this.loadArtists();
    this.setDefaultDateTime();
    this.setMinDateTime();
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    const userRole = this.authService.getUserRole();
    this.isAdmin = userRole === 'ADMIN';
  }

  private loadLocations(): void {
    this.locationService.getAllLocations().subscribe({
      next: (locations) => {
        this.locations = locations;
      },
      error: (err) => {
        this.toastr.error('Fehler beim Laden der Orte', 'Fehler');
      }
    });
  }

  private loadArtists(): void {
    this.artistService.getAllArtists().subscribe({
      next: (artists) => {
        this.artists = artists;
        this.filteredArtists = [];
      },
      error: (err) => {
        this.toastr.error('Fehler beim Laden der Künstler', 'Fehler');
      }
    });
  }

  private setDefaultDateTime(): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(20, 0, 0, 0);

    const year = tomorrow.getFullYear();
    const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const day = String(tomorrow.getDate()).padStart(2, '0');
    const hours = String(tomorrow.getHours()).padStart(2, '0');
    const minutes = String(tomorrow.getMinutes()).padStart(2, '0');

    const defaultDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
    this.eventForm.patchValue({dateTime: defaultDateTime});
  }

  onlyNumbers(event: KeyboardEvent): boolean {
    const charCode = event.which ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      event.preventDefault();
      return false;
    }
    return true;
  }

  private setMinDateTime(): void {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    this.minDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private futureDateValidator() {
    return (control: any) => {
      if (!control.value) {
        return null;
      }

      const selectedDate = new Date(control.value);
      const now = new Date();

      if (selectedDate <= now) {
        return {pastDate: true};
      }

      return null;
    };
  }

  onArtistSearchChange(): void {
    const searchLower = this.artistSearchTerm.toLowerCase().trim();

    if (searchLower.length === 0) {
      this.filteredArtists = [];
      this.showArtistDropdown = false;
      return;
    }

    this.filteredArtists = this.artists.filter(artist =>
      artist.name.toLowerCase().includes(searchLower) &&
      !this.selectedArtists.some(selected => selected.id === artist.id)
    );

    this.showArtistDropdown = this.filteredArtists.length > 0;
  }

  selectArtistFromDropdown(artist: Artist): void {
    this.selectedArtists.push(artist);

    this.eventForm.patchValue({
      artistIds: this.selectedArtists.map(a => a.id)
    });

    this.artistSearchTerm = '';
    this.filteredArtists = [];
    this.showArtistDropdown = false;
  }

  removeArtist(artist: Artist): void {
    const index = this.selectedArtists.findIndex(a => a.id === artist.id);

    if (index > -1) {
      this.selectedArtists.splice(index, 1);

      this.eventForm.patchValue({
        artistIds: this.selectedArtists.map(a => a.id)
      });
    }
  }

  onArtistInputBlur(): void {
    setTimeout(() => {
      this.showArtistDropdown = false;
    }, 200);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.selectedImageFile = null;
    this.previewImageUrl = null;
    this.imageValidationError = null;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    if (file.size > this.MAX_IMAGE_SIZE) {
      this.imageValidationError = 'Bild darf maximal 3 MB groß sein';
      this.toastr.error(this.imageValidationError, 'Ungültiges Bild');
      input.value = '';
      return;
    }

    if (!this.ALLOWED_IMAGE_TYPES.includes(file.type)) {
      this.imageValidationError = 'Nur JPG, PNG und WebP Bilder sind erlaubt';
      this.toastr.error(this.imageValidationError, 'Ungültiges Bild');
      input.value = '';
      return;
    }

    this.selectedImageFile = file;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.previewImageUrl = e.target.result;
    };
    reader.readAsDataURL(this.selectedImageFile);
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.previewImageUrl = null;
    this.imageValidationError = null;

    const fileInput = document.getElementById('eventImage') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  private truncateText(text: string, maxLength: number = 50): string {
    if (text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + '...';
  }

  onSubmit(): void {
    if (this.isLoading) {
      return;
    }

    if (this.selectedImageFile && this.imageValidationError) {
      this.toastr.error('Bitte korrigieren Sie das Bild', 'Validierung');
      return;
    }

    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      this.toastr.warning('Bitte füllen Sie alle Pflichtfelder korrekt aus', 'Validierung');
      return;
    }

    this.isLoading = true;

    const formValue = this.eventForm.value;
    const dateTimeIso = new Date(formValue.dateTime).toISOString();

    const payload = {
      title: formValue.title,
      type: formValue.type,
      durationMinutes: formValue.durationMinutes || undefined,
      description: formValue.description || undefined,
      dateTime: dateTimeIso,
      locationId: formValue.locationId!,
      artistIds: formValue.artistIds.length > 0 ? formValue.artistIds : undefined
    };

    this.eventService.createEvent(payload).subscribe({
      next: (createdEvent) => {
        const navigateToDetail = () => {
          this.toastr.success(
            `Veranstaltung "${this.truncateText(createdEvent.title)}" wurde erfolgreich erstellt!`,
            'Erfolg',
            {timeOut: 3000}
          );
          setTimeout(() => {
            this.router.navigate(['/events', createdEvent.id]);
          }, 1000);
        };

        if (this.selectedImageFile) {
          this.eventService.uploadImage(createdEvent.id, this.selectedImageFile).subscribe({
            next: () => {
              this.eventService.refreshImageCache();
              navigateToDetail();
            },
            error: (err) => {
              this.isLoading = false;
              this.toastr.error(
                err.error?.message || 'Veranstaltungsbild konnte nicht hochgeladen werden',
                'Fehler',
                {timeOut: 5000}
              );
            }
          });
        } else {
          navigateToDetail();
        }
      },
      error: (err) => {
        this.toastr.error(
          err.error?.message || 'Fehler beim Erstellen der Veranstaltung',
          'Fehler',
          {timeOut: 5000}
        );
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }
}
