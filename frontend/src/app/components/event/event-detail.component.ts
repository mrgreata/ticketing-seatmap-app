import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EventService} from '../../services/event.service';
import {EventDetail} from '../../dtos/event-detail';
import {AuthService} from '../../services/auth.service';
import {ToastrService} from 'ngx-toastr';

@Component({
  selector: 'app-event-detail',
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss'],
  standalone: false
})
export class EventDetailComponent implements OnInit {

  event: EventDetail | null = null;
  loading = true;
  imageError = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const eventId = +idParam;
        this.loadEvent(eventId);
      } else {
        this.toastr.error('Keine Veranstaltung gefunden', 'Fehler');
        this.loading = false;
      }
    });
  }

  loadEvent(id: number): void {
    this.loading = true;

    this.eventService.getEventById(id).subscribe({
      next: (event) => {
        this.event = event;
        this.loading = false;
      },
      error: (err) => {
        this.toastr.error('Veranstaltung konnte nicht geladen werden', 'Fehler');
        this.loading = false;
      }
    });
  }

  getEventImageUrl(): string {
    if (!this.event || this.imageError) {
      return this.getPlaceholderImage();
    }
    return this.eventService.getImageUrl(this.event.id);
  }

  private getPlaceholderImage(): string {
    if (!this.event) {
      return 'assets/placeholder_concert.jpg';
    }

    const type = this.event.type?.toLowerCase() || '';

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

  onImageError(event: Event): void {
    this.imageError = true;
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.getPlaceholderImage();
  }

  private truncateText(text: string, maxLength: number = 50): string {
    if (text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + '...';
  }

  getTruncatedTitle(maxLength: number = 30): string {
    return this.event ? this.truncateText(this.event.title, maxLength) : '';
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  editEvent(): void {
    if (this.event) {
      this.router.navigate(['/events', this.event.id, 'edit']);
    }
  }

  deleteEvent(): void {
    if (!this.event) return;

    const eventTitle = this.truncateText(this.event.title, 40);

    this.eventService.deleteEvent(this.event.id).subscribe({
      next: () => {
        this.toastr.success(
          `Veranstaltung "${eventTitle}" wurde erfolgreich gelöscht`,
          'Erfolg',
          { timeOut: 2000 }
        );

        setTimeout(() => {
          this.router.navigate(['/events']);
        }, 500);
      },
      error: (err) => {
        this.toastr.error(
          err.error?.message || 'Fehler beim Löschen der Veranstaltung',
          'Fehler',
          { timeOut: 5000 }
        );
      }
    });
  }

  formatDate(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString('de-AT', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  formatTime(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleTimeString('de-AT', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  openSeatmap(): void {
    if (!this.event) {
      return;
    }

    this.router.navigate(['/seatmap'],
      {
        state: { eventId: this.event!.id }
      });
  }
}
