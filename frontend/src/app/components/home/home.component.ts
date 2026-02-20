import { Component, OnInit } from '@angular/core';
import { NewsService } from '../../services/news.service';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { jwtDecode } from "jwt-decode";
import {TopTenEvent} from "../../dtos/top-ten-event";
import {UserService} from "../../services/user.service";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: false
})
export class HomeComponent implements OnInit {
  news: any[] = [];
  events: any[] = [];
  isLoadingNews = true;
  isLoadingEvents = true;
  errorNews: string | null = null;
  errorEvents: string | null = null;

  isAuthenticated = false;
  isAdmin = false;
  isUser = false;
  userRole: string | null = null;
  userName: string | null = null;

  topTenEvents: TopTenEvent[] = [];
  topTenMonth: number = new Date().getMonth() + 1;
  topTenYear: number = new Date().getFullYear();
  topTenType: string = '';
  showTopTen: boolean = true;
  eventTypes: string[] = ['Theater', 'Konzert', 'Oper', 'Festival', 'Kino'];

  constructor(
    private newsService: NewsService,
    private eventService: EventService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private toastr: ToastrService
  ) { }

  ngOnInit() {
    this.checkAuthStatus();
    this.loadNews();
    this.loadEvents();
    this.loadTopTen();
  }

  toggleTopTen(): void {
    this.showTopTen = !this.showTopTen;
    if (this.showTopTen && this.topTenEvents.length === 0) {
      this.loadTopTen();
    }
  }

  loadTopTen(): void {
    this.eventService.getTopTen(
      this.topTenMonth,
      this.topTenYear,
      this.topTenType || undefined
    ).subscribe({
      next: (data) => {
        this.topTenEvents = data;
      },
      error: (error) => {
        console.error('Error loading top ten:', error);
        this.toastr.error(
          error.error?.message || 'Fehler beim Laden von Top Ten',
          'Fehler'
        );
      }
    });
  }

  onTopTenFilterChange(): void {
    this.loadTopTen();
  }

  getBarWidth(ticketsSold: number): number {
    if (this.topTenEvents.length === 0) return 0;
    const maxTickets = Math.max(...this.topTenEvents.map(e => e.ticketsSold));
    return (ticketsSold / maxTickets) * 100;
  }

  openSeatmapFromTopTen(eventId: number): void {
    this.router.navigate(['/seatmap'], {
      state: { eventId }
    });
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isAdmin = this.userRole == 'ADMIN';
    this.isUser = this.userRole == 'USER';

    if (!this.isAuthenticated) {
      this.userName = null;
      return;
    }

    this.loadUserName();
  }

  private loadUserName() {
    this.userService.getMeDetailed().subscribe({
      next: (user) => {
        this.userName =
          user.firstName ??
          user.lastName ??
          user.email ??
          'Benutzer';
      },
      error: (err) => {
        console.error('Error loading user name:', err);
        this.userName = 'Benutzer';
      }
    });
  }

  loadNews(): void {
    this.isLoadingNews = true;
    this.errorNews = null;

    if (this.isAuthenticated && this.isUser) {
      this.newsService.loadNewsBasedOnAuth().subscribe({
        next: ({ current }) => {
          this.news = current.content.slice(0, 8);
          this.isLoadingNews = false;
        },
        error: (err) => {
          console.error('Error loading news for homepage:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von News',
            'Fehler'
          );
          this.loadPublicNews();
        }
      });
    } else {
      this.loadPublicNews();
    }
  }

  loadPublicNews(): void {
    this.newsService.getAll().subscribe({
      next: (allNews) => {
        this.news = allNews.content.slice(0, 8);
        this.isLoadingNews = false;
      },
      error: (err) => {
        console.error('Error loading public news for homepage:', err);
        this.toastr.error(
          err.error?.message || 'Fehler beim Laden von öffentlichen News',
          'Fehler'
        );
        this.errorNews = 'News konnten nicht geladen werden.';
        this.isLoadingNews = false;
      }
    });
  }


  loadEvents(): void {
    this.isLoadingEvents = true;
    this.errorEvents = null;

    this.eventService.getAllEvents(0, 8).subscribe({
      next: (data) => {
        this.events = data.content.slice(0, 8);
        this.isLoadingEvents = false;

        this.events.forEach(event => {
          this.eventService.checkImageExists(event.id).subscribe({
            next: (exists) => {
              if (exists) {
                event.hasImage = true;
              }
            },
            error: () => {
              event.hasImage = false;
            }
          });
        });
      },
      error: (err) => {
        console.error('Error loading events for homepage:', err);
        this.toastr.error(
          err.error?.message || 'Fehler beim Laden von Veranstaltungen',
          'Fehler'
        );
        this.isLoadingEvents = false;
      }
    });
  }

  getMonthFromDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('de-DE', { month: 'short' });
  }

  getDayFromDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.getDate().toString();
  }

  getEventMonth(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString('de-AT', { month: 'short' }).toUpperCase();
  }

  getEventDay(dateTime: string): string {
    const date = new Date(dateTime);
    return date.getDate().toString();
  }

  formatEventDate(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleDateString('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getNewsImageUrl(newsId: number): string {
    return this.newsService.getImageUrl(newsId);
  }

  getEventImageUrl(event: any): string {
    if (event.hasImage) {
      return this.eventService.getImageUrl(event.id);
    }
    return this.getPlaceholderImage(event);
  }

  private getPlaceholderImage(event: any): string {
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

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = 'assets/placeholder-news-image.jpg';
  }

}
