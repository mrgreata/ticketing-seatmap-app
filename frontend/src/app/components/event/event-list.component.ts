import {Component, OnInit, OnDestroy} from '@angular/core';
import {Event} from '../../dtos/event';
import {Artist} from '../../dtos/artist';
import {Location} from '../../dtos/location';
import {EventService} from '../../services/event.service';
import {ArtistService} from '../../services/artist.service';
import {LocationService} from '../../services/location.service';
import {Router, NavigationEnd} from '@angular/router';
import {Subscription} from 'rxjs';
import {filter} from 'rxjs/operators';
import {AuthService} from '../../services/auth.service';
import {ToastrService} from 'ngx-toastr';

@Component({
  selector: 'app-event-list',
  templateUrl: './event-list.component.html',
  styleUrls: ['./event-list.component.scss'],
  standalone: false
})
export class EventListComponent implements OnInit, OnDestroy {
  searchMode: 'event' | 'artist' | 'location' = 'event';

  events: Event[] = [];
  isLoading: boolean = false;
  errorMessage: string = '';

  showFilters: boolean = false;

  searchTitle: string = '';
  searchType: string = '';
  searchDuration: number | null = null;
  searchDateFrom: string = '';
  searchDateTo: string = '';
  searchPriceMin: number | null = null;
  searchPriceMax: number | null = null;

  artists: Artist[] = [];
  groupedArtists: { artist: Artist; bands: Artist[] }[] = [];
  searchArtistName: string = '';
  selectedArtist: Artist | null = null;
  artistResultsCollapsed: boolean = false;

  locations: Location[] = [];
  searchLocationName: string = '';
  searchLocationCity: string = '';
  searchLocationZip: string = '';
  searchLocationStreet: string = '';
  selectedLocation: Location | null = null;
  locationResultsCollapsed: boolean = false;

  eventTypes: string[] = ['Theater', 'Konzert', 'Oper', 'Festival', 'Kino'];

  hasSearchedArtists: boolean = false;
  hasSearchedLocations: boolean = false;

  priceError: string = '';
  dateError: string = '';
  zipError: string = '';

  eventsWithImages: Set<number> = new Set();
  checkedEvents: Set<number> = new Set();

  currentPage: number = 0;
  pageSize: number = 12;
  hasMoreEvents: boolean = true;
  isLoadingMore: boolean = false;

  private routerSubscription?: Subscription;

  constructor(
    private eventService: EventService,
    private artistService: ArtistService,
    private locationService: LocationService,
    private router: Router,
    private authService: AuthService,
    private toastr: ToastrService
  ) {
  }

  ngOnInit(): void {
    this.loadAllEvents();

    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        if (event.url === '/events' || event.url.startsWith('/events?')) {
          this.loadAllEvents();
        }
      });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  getTodayDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = (today.getMonth() + 1).toString().padStart(2, '0');
    const day = today.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  preventNonNumeric(event: KeyboardEvent): void {
    const allowedKeys = [
      'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Home', 'End'
    ];

    if (event.ctrlKey || event.metaKey) {
      return;
    }

    if (allowedKeys.includes(event.key)) {
      return;
    }

    if (!/^[0-9]$/.test(event.key)) {
      event.preventDefault();
    }
  }

  preventNonNumericPrice(event: KeyboardEvent): void {
    const allowedKeys = [
      'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Home', 'End'
    ];

    if (event.ctrlKey || event.metaKey) {
      return;
    }

    if (allowedKeys.includes(event.key)) {
      return;
    }

    const input = event.target as HTMLInputElement;

    if ((event.key === ',' || event.key === '.') && (!input.value.includes(',') || !input.value.includes('.'))) {
      return;
    }

    if (!/^[0-9]$/.test(event.key)) {
      event.preventDefault();
    }
  }

  validateDuration(): void {
    if (this.searchDuration !== null) {
      if (this.searchDuration < 1) {
        this.searchDuration = null;
      } else if (this.searchDuration > 9999) {
        this.searchDuration = 9999;
      } else {
        this.searchDuration = Math.floor(this.searchDuration);
      }
    }
  }

  validatePrices(): void {
    this.priceError = '';

    if (this.searchPriceMin !== null) {
      if (this.searchPriceMin < 0) this.searchPriceMin = 0;
      if (this.searchPriceMin > 99999) this.searchPriceMin = 99999;
    }

    if (this.searchPriceMax !== null) {
      if (this.searchPriceMax < 0) this.searchPriceMax = 0;
      if (this.searchPriceMax > 99999) this.searchPriceMax = 99999;
    }

    if (this.searchPriceMin !== null && this.searchPriceMax !== null) {
      if (this.searchPriceMin > this.searchPriceMax) {
        this.priceError = 'Min-Preis darf nicht größer als Max-Preis sein';
      }
    }
  }

  limitPriceValue(event: any, field: 'min' | 'max'): void {
    const input = event.target as HTMLInputElement;
    let value = parseFloat(input.value);

    if (value > 99999) {
      value = 99999;
      input.value = '99999';

      if (field === 'min') {
        this.searchPriceMin = value;
      } else {
        this.searchPriceMax = value;
      }
    }

    if (value < 0) {
      value = 0;
      input.value = '0';

      if (field === 'min') {
        this.searchPriceMin = value;
      } else {
        this.searchPriceMax = value;
      }
    }
  }

  validateDates(): void {
    this.dateError = '';

    if (this.searchDateFrom && this.searchDateTo) {
      const fromDate = new Date(this.searchDateFrom);
      const toDate = new Date(this.searchDateTo);

      if (fromDate > toDate) {
        this.dateError = '"Von Datum" darf nicht nach "Bis Datum" liegen';
      }
    }
  }

  validateZip(): void {
    this.zipError = '';

    this.searchLocationZip = this.searchLocationZip.replace(/[^0-9]/g, '');

    if (this.searchLocationZip.length > 0 && this.searchLocationZip.length !== 4) {
      this.zipError = 'PLZ muss genau 4 Ziffern haben';
    }

    if (this.searchLocationZip.length > 4) {
      this.searchLocationZip = this.searchLocationZip.substring(0, 4);
    }
  }

  onSearch(): void {
    this.validatePrices();
    this.validateDates();
    this.validateZip();
    this.validateDuration();

    if (this.priceError || this.dateError || this.zipError) {
      return;
    }

    if (this.searchMode === 'event') {
      this.searchEvents();
    } else if (this.searchMode === 'artist') {
      this.searchArtists();
    } else if (this.searchMode === 'location') {
      this.searchLocations();
    }
  }

  getEventImageUrl(event: Event): string {
    if (this.eventsWithImages.has(event.id)) {
      return this.eventService.getImageUrl(event.id);
    }
    return this.getPlaceholderImage(event);
  }

  private getPlaceholderImage(event: Event): string {
    const type = event.type?.toLowerCase() || '';

    if (type.includes('konzert') || type.includes('concert')) {
      return 'assets/placeholder_concert.jpg';
    } else if (type.includes('oper') || type.includes('opera')) {
      return 'assets/placeholder_opera.jpg';
    } else if (type.includes('theater')) {
      return 'assets/placeholder_theater.jpg';
    } else if (type.includes('festival')) {
      return 'assets/placeholder_festival.jpg';
    } else if (type.includes('kino') || type.includes('cinema')) {
      return 'assets/placeholder_cinema.jpg';
    }

    return 'assets/placeholder_concert.jpg';
  }

  setSearchMode(mode: 'event' | 'artist' | 'location'): void {
    this.searchMode = mode;
    this.resetSearchFields();

    if (mode === 'event') {
      this.loadAllEvents();
    } else if (mode === 'artist') {
      this.hasSearchedArtists = false;
      this.loadAllArtists();
    } else if (mode === 'location') {
      this.hasSearchedLocations = false;
      this.loadAllLocations();
    }
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  loadAllEvents(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.currentPage = 0;
    this.hasMoreEvents = true;

    this.eventService.getAllEvents(this.currentPage, this.pageSize).subscribe({
      next: (data) => {
        this.events = data.content;
        this.hasMoreEvents = !data.last;
        this.isLoading = false;

        this.checkImagesForEvents(data.content);
      },
      error: (error) => {
        this.errorMessage = 'Fehler beim Laden der Veranstaltungen.';
        this.isLoading = false;
      }
    });
  }

  private checkImagesForEvents(events: Event[]): void {
    events.forEach(event => {
      if (!this.checkedEvents.has(event.id)) {
        this.checkedEvents.add(event.id);

        this.eventService.checkImageExists(event.id).subscribe({
          next: (exists) => {
            if (exists) {
              this.eventsWithImages.add(event.id);
            }
          },
          error: () => {
          }
        });
      }
    });
  }

  loadMoreEvents(): void {
    if (this.isLoadingMore || !this.hasMoreEvents) {
      return;
    }

    this.isLoadingMore = true;
    this.currentPage++;

    const searchObservable = this.searchMode === 'event' && this.hasSearchCriteria()
      ? this.getSearchObservable()
      : this.eventService.getAllEvents(this.currentPage, this.pageSize);

    searchObservable.subscribe({
      next: (data) => {
        this.events = [...this.events, ...data.content];
        this.hasMoreEvents = !data.last;
        this.isLoadingMore = false;

        this.checkImagesForEvents(data.content);
      },
      error: (error) => {
        this.errorMessage = 'Fehler beim Laden weiterer Veranstaltungen.';
        this.isLoadingMore = false;
      }
    });
  }

  loadAllArtists(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.artists = [];
    this.groupedArtists = [];
    this.events = [];
    this.selectedArtist = null;

    this.artistService.getAllArtists().subscribe({
      next: (data) => {
        this.artists = data;

        this.groupedArtists = data.map(artist => {
          return {artist, bands: []};
        });

        this.isLoading = false;
        if (this.groupedArtists.length === 0) {
          this.errorMessage = 'Keine Künstler gefunden';
        }
      },
      error: (error) => {
        console.error('Error loading artists:', error);
        this.errorMessage = 'Fehler beim Laden der Künstler';
        this.isLoading = false;
      }
    });
  }

  loadAllLocations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.locations = [];
    this.events = [];
    this.selectedLocation = null;

    this.locationService.getAllLocations().subscribe({
      next: (data) => {
        this.locations = data;
        this.isLoading = false;
        if (this.locations.length === 0) {
          this.errorMessage = 'Keine Orte gefunden';
        }
      },
      error: (error) => {
        console.error('Error loading locations:', error);
        this.errorMessage = 'Fehler beim Laden der Orte';
        this.isLoading = false;
      }
    });
  }

  searchEvents(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.currentPage = 0;
    this.hasMoreEvents = true;

    this.getSearchObservable().subscribe({
      next: (data: any) => {
        this.events = data.content;
        this.hasMoreEvents = !data.last;
        this.isLoading = false;

        this.checkImagesForEvents(data.content);

        if (this.events.length === 0) {
          this.errorMessage = 'Keine Veranstaltungen gefunden';
        }
      },
      error: (error: any) => {
        this.errorMessage = 'Fehler bei der Suche.';
        this.isLoading = false;
      }
    });
  }

  searchArtists(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.artists = [];
    this.groupedArtists = [];
    this.events = [];
    this.selectedArtist = null;
    this.hasSearchedArtists = true;

    const searchObservable = (!this.searchArtistName || this.searchArtistName.trim() === '')
      ? this.artistService.getAllArtists()
      : this.artistService.searchArtists(this.searchArtistName, true);

    searchObservable.subscribe({
      next: (data) => {
        this.artists = data;
        const searchLower = this.searchArtistName.toLowerCase();

        const artistMap = new Map();
        data.forEach(a => artistMap.set(a.id, a));

        const directMatches: Artist[] = [];
        const allBands: Artist[] = [];

        if (!this.searchArtistName || this.searchArtistName.trim() === '') {
          directMatches.push(...data);
        } else {
          data.forEach(artist => {
            const matchesDirectly = artist.name.toLowerCase().includes(searchLower);
            if (matchesDirectly) {
              directMatches.push(artist);
            }
          });
        }

        data.forEach(artist => {
          if (artist.isBand) {
            allBands.push(artist);
          }
        });

        this.groupedArtists = directMatches.map(artist => {
          const relatedBands: Artist[] = [];

          if (!artist.isBand) {
            allBands.forEach(band => {
              if (band.memberIds?.includes(artist.id)) {
                if (this.searchArtistName && !directMatches.find(a => a.id === band.id)) {
                  relatedBands.push(band);
                }
              }
            });
          }

          return {artist, bands: relatedBands};
        });

        this.isLoading = false;

        if (this.groupedArtists.length === 0) {
          this.errorMessage = 'Keine Künstler gefunden';
        }
      },
      error: (error) => {
        this.errorMessage = 'Fehler bei der Künstlersuche';
        this.isLoading = false;
      }
    });
  }

  selectArtist(artist: Artist): void {
    this.selectedArtist = artist;
    this.hasSearchedArtists = true;
    this.artistResultsCollapsed = true;
    this.isLoading = true;
    this.errorMessage = '';
    this.hasMoreEvents = false;

    this.artistService.getEventsByArtist(artist.id).subscribe({
      next: (data) => {
        this.events = data;
        this.isLoading = false;

        this.checkImagesForEvents(data);

        if (this.events.length === 0) {
          this.errorMessage = `Keine Veranstaltungen für ${artist.name} gefunden`;
        }
      },
      error: (error) => {
        this.errorMessage = 'Fehler beim Laden der Veranstaltungen';
        this.isLoading = false;
      }
    });
  }

  searchLocations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.locations = [];
    this.events = [];
    this.selectedLocation = null;
    this.hasSearchedLocations = true;

    const hasSearchCriteria = this.searchLocationName || this.searchLocationCity ||
      this.searchLocationZip || this.searchLocationStreet;

    const searchObservable = !hasSearchCriteria
      ? this.locationService.getAllLocations()
      : this.locationService.searchLocations(
        this.searchLocationName || undefined,
        this.searchLocationStreet || undefined,
        this.searchLocationCity || undefined,
        this.searchLocationZip ? parseInt(this.searchLocationZip) : undefined
      );

    searchObservable.subscribe({
      next: (data) => {
        this.locations = data;
        this.isLoading = false;

        if (this.locations.length === 0) {
          this.errorMessage = 'Keine Orte gefunden';
        }
      },
      error: (error) => {
        this.errorMessage = 'Fehler bei der Ortssuche';
        this.isLoading = false;
      }
    });
  }

  selectLocation(location: Location): void {
    this.selectedLocation = location;
    this.hasSearchedLocations = true;
    this.locationResultsCollapsed = true;
    this.isLoading = true;
    this.errorMessage = '';
    this.hasMoreEvents = false;

    this.locationService.getEventsByLocation(location.id).subscribe({
      next: (data) => {
        this.events = data;
        this.isLoading = false;

        this.checkImagesForEvents(data);

        if (this.events.length === 0) {
          this.errorMessage = `Keine Veranstaltungen in ${location.city} gefunden`;
        }
      },
      error: (error) => {
        this.errorMessage = 'Fehler beim Laden der Veranstaltungen';
        this.isLoading = false;
      }
    });
  }

  onReset(): void {
    this.resetSearchFields();

    if (this.searchMode === 'event') {
      this.loadAllEvents();
    } else if (this.searchMode === 'artist') {
      this.hasSearchedArtists = false;
      this.loadAllArtists();
    } else if (this.searchMode === 'location') {
      this.hasSearchedLocations = false;
      this.loadAllLocations();
    }
  }

  toggleArtistResults(): void {
    this.artistResultsCollapsed = !this.artistResultsCollapsed;
  }

  toggleLocationResults(): void {
    this.locationResultsCollapsed = !this.locationResultsCollapsed;
  }

  resetSearchFields(): void {
    this.searchTitle = '';
    this.searchType = '';
    this.searchDuration = null;
    this.searchDateFrom = '';
    this.searchDateTo = '';
    this.searchPriceMin = null;
    this.searchPriceMax = null;

    this.searchArtistName = '';
    this.artists = [];
    this.groupedArtists = [];
    this.selectedArtist = null;
    this.artistResultsCollapsed = false;

    this.searchLocationName = '';
    this.searchLocationCity = '';
    this.searchLocationZip = '';
    this.searchLocationStreet = '';
    this.locations = [];
    this.selectedLocation = null;
    this.locationResultsCollapsed = false;

    this.errorMessage = '';

    this.priceError = '';
    this.dateError = '';
    this.zipError = '';
  }

  getMonth(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString('de-AT', {month: 'short'}).toUpperCase();
  }

  getDay(dateTime: string): string {
    const date = new Date(dateTime);
    return date.getDate().toString();
  }

  formatDate(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  editEvent(eventId: number, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.router.navigate(['/events', eventId, 'edit']);
  }

  deleteEvent(eventId: number): void {
    this.eventService.deleteEvent(eventId).subscribe({
      next: () => {
        this.toastr.success('Veranstaltung wurde gelöscht', 'Erfolg');
        this.loadAllEvents();
      },
      error: (err) => {
        this.toastr.error(
          err.error?.message || 'Fehler beim Löschen',
          'Fehler'
        );
      }
    });
  }

  navigateToCreateEvent(): void {
    this.router.navigate(['/events/new']);
  }

  private hasSearchCriteria(): boolean {
    return !!(this.searchTitle || this.searchType || this.searchDuration ||
      this.searchDateFrom || this.searchDateTo || this.searchPriceMin || this.searchPriceMax);
  }

  private getSearchObservable(): any {
    let dateFrom: string | undefined = undefined;
    let dateTo: string | undefined = undefined;

    if (this.searchDateFrom) {
      dateFrom = this.searchDateFrom + 'T00:00:00';
    }
    if (this.searchDateTo) {
      dateTo = this.searchDateTo + 'T23:59:59';
    }

    const priceMinCents = this.searchPriceMin ? Math.round(this.searchPriceMin * 100) : undefined;
    const priceMaxCents = this.searchPriceMax ? Math.round(this.searchPriceMax * 100) : undefined;

    return this.eventService.searchEvents(
      this.searchTitle || undefined,
      this.searchType || undefined,
      this.searchDuration || undefined,
      dateFrom,
      dateTo,
      undefined,
      priceMinCents,
      priceMaxCents,
      this.currentPage,
      this.pageSize
    );
  }
}
