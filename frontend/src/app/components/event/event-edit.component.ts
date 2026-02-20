import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
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
import {EventDetail} from '../../dtos/event-detail';
import {SharedModule} from '../../shared/shared.module';

@Component({
  selector: 'app-event-edit',
  standalone: true,
  templateUrl: './event-edit.component.html',
  styleUrls: ['./event-edit.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule
  ]
})
export class EventEditComponent implements OnInit {

  eventTypes = ['Konzert', 'Oper', 'Musical', 'Festival', 'Kino'];

  eventId: number | null = null;
  eventForm: FormGroup;

  locations: Location[] = [];
  artists: Artist[] = [];
  selectedArtists: Artist[] = [];
  selectedImageFile: File | null = null;

  currentImageUrl: string | null = null;
  previewImageUrl: string | null = null;

  artistSearchTerm: string = '';
  filteredArtists: Artist[] = [];
  showArtistDropdown: boolean = false;

  isLoading = false;
  loadingEvent = true;

  isAuthenticated = false;
  isAdmin = false;
  imageMarkedForDeletion: boolean = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private eventService: EventService,
    private locationService: LocationService,
    private artistService: ArtistService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.eventForm = this.fb.group({
      id: [null],
      title: ['', [Validators.required, Validators.maxLength(50)]],
      type: ['', [Validators.required, Validators.maxLength(100)]],
      durationMinutes: [null, [Validators.required, Validators.min(1), Validators.max(9999)]],
      description: ['', Validators.maxLength(1000)],
      dateTime: ['', Validators.required],
      locationId: [null, Validators.required],
      artistIds: [[]]
    });
  }

  ngOnInit() {
    this.checkAuthStatus();
    this.loadLocations();
    this.loadArtists();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.eventId = +id;
        this.loadEvent(this.eventId);
      } else {
        this.toastr.error('Keine Veranstaltung gefunden', 'Error');
        this.loadingEvent = false;
      }
    });
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    const userRole = this.authService.getUserRole();
    this.isAdmin = userRole === 'ADMIN';
  }

  private loadEvent(id: number): void {
    this.eventService.getEventById(id).subscribe({
      next: (event: EventDetail) => {
        const date = new Date(event.dateTime);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const dateTimeFormatted = `${year}-${month}-${day}T${hours}:${minutes}`;

        this.selectedArtists = event.artists.map(a => ({id: a.id, name: a.name}));

        this.eventForm.patchValue({
          id: event.id,
          title: event.title,
          type: event.type,
          durationMinutes: event.durationMinutes,
          description: event.description || '',
          dateTime: dateTimeFormatted,
          locationId: event.location.id,
          artistIds: this.selectedArtists.map(a => a.id)
        });

        this.eventService.checkImageExists(event.id).subscribe({
          next: (exists) => {
            if (exists) {
              this.currentImageUrl = this.eventService.getImageUrl(event.id);
            } else {
              this.currentImageUrl = null;
            }
          },
          error: () => {
            this.currentImageUrl = null;
          }
        });

        this.imageMarkedForDeletion = false;
        this.loadingEvent = false;
      },
      error: (err) => {
        this.toastr.error('Veranstaltung konnte nicht geladen werden', 'Fehler');
        this.loadingEvent = false;
      }
    });
  }

  private loadLocations(): void {
    this.locationService.getAllLocations().subscribe({
      next: (locations) => {
        this.locations = locations;
      }
    });
  }

  private loadArtists(): void {
    this.artistService.getAllArtists().subscribe({
      next: (artists) => {
        this.artists = artists;
        this.filteredArtists = [];
      }
    });
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

    if (!input.files || input.files.length === 0) {
      this.selectedImageFile = null;
      this.previewImageUrl = null;
      return;
    }

    this.selectedImageFile = input.files[0];

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.previewImageUrl = e.target.result;
    };
    reader.readAsDataURL(this.selectedImageFile);
  }

  removeNewImage(): void {
    this.selectedImageFile = null;
    this.previewImageUrl = null;

    const fileInput = document.getElementById('eventImage') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  confirmDeleteImage(): void {
    this.imageMarkedForDeletion = true;
    this.currentImageUrl = null;
    this.toastr.info('Bild wird beim Speichern gelöscht', 'Info');
  }


  onlyNumbers(event: KeyboardEvent): boolean {
    const charCode = event.which ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      event.preventDefault();
      return false;
    }
    return true;
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

    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      this.toastr.warning('Bitte füllen Sie alle Pflichtfelder korrekt aus', 'Validierung');
      return;
    }

    this.isLoading = true;

    const formValue = this.eventForm.value;
    const dateTimeIso = new Date(formValue.dateTime).toISOString();

    const payload = {
      id: formValue.id,
      title: formValue.title,
      type: formValue.type,
      durationMinutes: formValue.durationMinutes,
      description: formValue.description || undefined,
      dateTime: dateTimeIso,
      locationId: formValue.locationId!,
      artistIds: formValue.artistIds.length > 0 ? formValue.artistIds : undefined
    };

    this.eventService.updateEvent(formValue.id, payload).subscribe({
      next: (updatedEvent) => {
        const navigateToDetail = () => {
          this.toastr.success(
            `Veranstaltung "${this.truncateText(updatedEvent.title)}" wurde erfolgreich aktualisiert!`,
            'Erfolg',
            {timeOut: 3000}
          );
          setTimeout(() => {
            this.router.navigate(['/events', updatedEvent.id]);
          }, 1000);
        };

        if (this.imageMarkedForDeletion) {
          this.eventService.deleteImage(updatedEvent.id).subscribe({
            next: () => {
              this.eventService.refreshImageCache();

              if (this.selectedImageFile) {
                this.eventService.uploadImage(updatedEvent.id, this.selectedImageFile).subscribe({
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
              this.isLoading = false;
              this.toastr.error(
                err.error?.message || 'Fehler beim Löschen des Bildes',
                'Fehler',
                {timeOut: 5000}
              );
            }
          });
        } else if (this.selectedImageFile) {
          this.eventService.uploadImage(updatedEvent.id, this.selectedImageFile).subscribe({
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
          err.error?.message || 'Fehler beim Aktualisieren der Veranstaltung',
          'Fehler',
          {timeOut: 5000}
        );
        this.isLoading = false;
      }
    });
  }

  goBack(): void {
    if (this.eventId) {
      this.router.navigate(['/events', this.eventId]);
    } else {
      this.router.navigate(['/events']);
    }
  }
}
