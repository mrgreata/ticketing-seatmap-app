import { Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import { NewsService } from '../../../services/news.service';
import { DetailedNewsItem } from '../../../dtos/newsDtos/detailed-news-item';
import {AuthService} from "../../../services/auth.service";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-news-detail',
  standalone: false,
  templateUrl: './news-detail.component.html',
  styleUrls: ['./news-detail.component.scss']
})
export class NewsDetailComponent implements OnInit {

  newsId!: number;
  newsItem: DetailedNewsItem | null = null;
  isLoading = true;
  error: string | null = null;

  isAuthenticated = false;
  isAdmin = false;
  isUser = false;
  userRole: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private newsService: NewsService,
    private authService: AuthService,
    private toastr: ToastrService
  ) { }

  ngOnInit() {
    this.checkAuthStatus();
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.newsId = +idParam;
        this.loadNewsDetail();
      }
    });
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isUser = this.userRole === 'USER';
    this.isAdmin = this.userRole === 'ADMIN';
  }

  loadNewsDetail(): void {
    this.isLoading = true;
    this.error = null;

    this.newsService.getById(this.newsId).subscribe({
      next: (news) => {
        this.newsItem = news;
        this.isLoading = false;

        // mark news as read
        if (this.isAuthenticated && this.isUser) {
          this.markAsRead();
        }
      },
      error: (err) => {
        console.error('Error loading news detail:', err);
        this.error = 'News konnte nicht geladen werden. ';
        this.isLoading = false;
      }
    });
  }

  private markAsRead(): void {
    this.newsService.markAsRead(this.newsId).subscribe({
      next: () => {
        console.log(`News ${this.newsId} marked as read`);
      },
      error: (err) => {
        console.error('Error marking news as read:', err);
      }
    });
  }

  editNews(): void {
    this.router.navigate(['admin/news/edit', this.newsId]);
  }

  confirmDelete(): void {
    if (!this.newsItem) {
      return;
    }

    this.newsService.deleteNews(this.newsId).subscribe({
      next: () => {
        this.toastr.success(
          `News "${this.newsItem.title}" wurde erfolgreich gelöscht`,
          'Erfolg',
          { timeOut: 2000 }
        );

        this.router.navigate(['/news/home']);
      },
      error: (err) => {
        this.toastr.error(
          err.error?.message || 'Fehler beim Löschen der News',
          'Fehler',
          { timeOut: 5000 }
        );
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/news/home']);
  }

  formatDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  formatText(text: string): string {
    if (!text) return '';
    const safeText = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    return safeText.replace(/\n/g, '<br>');
  }

  getNewsImageUrl(newsId: number): string {
    return this.newsService.getImageUrl(newsId);
  }

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = 'assets/placeholder-news-image.jpg';
  }
}
