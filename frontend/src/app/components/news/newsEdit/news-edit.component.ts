import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NewsService } from '../../../services/news.service';
import { AuthService } from '../../../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { UpdateNewsItem } from '../../../dtos/newsDtos/update-news-item';
import { DetailedNewsItem } from '../../../dtos/newsDtos/detailed-news-item';
import {SharedModule} from "../../../shared/shared.module";

@Component({
  selector: 'app-news-edit',
  standalone: true,
  templateUrl: './news-edit.component.html',
  styleUrls: ['./news-edit.component.scss'],
  imports: [CommonModule, RouterLink, FormsModule, SharedModule]
})
export class NewsEditComponent implements OnInit {

  newsData: UpdateNewsItem = {
    id: 0,
    title: '',
    summary: '',
    text: '',
    publishedAt: new Date().toISOString().split('T')[0]
  };

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  currentImageUrl: string | null = null;
  isUploading = false;
  isLoading = false;
  loadingNews = true;
  error: string | null = null;
  successMessage: string | null = null;

  isAuthenticated = false;
  isAdmin = false;
  userRole: string | null = null;
  imageMarkedForDeletion: boolean = false;

  constructor(
    private newsService: NewsService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.checkAuthStatus();
    this.loadNewsData();
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isAdmin = this.userRole === 'ADMIN';
  }

  private loadNewsData(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const newsId = +idParam;
        this.newsData.id = newsId;
        this.fetchNewsDetail(newsId);
      } else {
        this.error = 'Keine News-ID gefunden';
        this.loadingNews = false;
      }
    });
  }

  private fetchNewsDetail(id: number): void {
    this.newsService.getById(id).subscribe({
      next: (news: DetailedNewsItem) => {
        this.newsData.title = news.title;
        this.newsData.summary = news.summary;
        this.newsData.text = news.text;
        this.newsData.publishedAt = new Date(news.publishedAt).toISOString().split('T')[0];

        this.currentImageUrl = this.newsService.getImageUrl(news.id);
        this.previewUrl = null;

        this.imageMarkedForDeletion = false;
        this.loadingNews = false;
      },
      error: (err) => {
        console.error('Error loading news detail:', err);
        this.error = 'News konnte nicht geladen werden';
        this.loadingNews = false;
        this.toastr.error('News konnte nicht geladen werden', 'Fehler');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const validationError = this.newsService.validateImageFile(file);
      if (validationError) {
        this.error = validationError;
        return;
      }

      this.selectedFile = file;
      this.error = null;

      this.imageMarkedForDeletion = false;

      const reader = new FileReader();
      reader.onload = () => {
        this.previewUrl = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeNewImage(): void {
    this.selectedFile = null;
    this.previewUrl = null;

    const fileInput = document.getElementById('newsImage') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  confirmDeleteImage(): void {
    this.imageMarkedForDeletion = true;
    this.selectedFile = null;
    this.previewUrl = null;
    this.toastr.info('Bild wird beim Speichern gelöscht', 'Info');
  }

  async onSubmit(): Promise<void> {
    if (!this.newsData.title || !this.newsData.summary || !this.newsData.text) {
      this.error = 'Bitte füllen Sie alle Pflichtfelder aus';
      return;
    }

    this.isLoading = true;
    this.error = null;
    this.successMessage = null;

    if (this.newsData.publishedAt) {
      const date = new Date(this.newsData.publishedAt);
      if (!isNaN(date.getTime())) {
        this.newsData.publishedAt = date.toISOString();
      }
    }

    this.newsService.updateNews(this.newsData).subscribe({
      next: (updatedNews) => {
        const afterAll = () => {
          this.toastr.success(
            `News "${this.truncateText(updatedNews.title)}" wurde aktualisiert`,
            'Erfolg'
          );
          this.router.navigate(['/news/detail', updatedNews.id]);
        };

        if (this.imageMarkedForDeletion && !this.selectedFile) {
          this.newsService.deleteImage(updatedNews.id).subscribe({
            next: () => {
              afterAll();
            },
            error: (err) => {
              console.error('Error deleting image:', err);
              this.isLoading = false;
              this.toastr.error('Fehler beim Löschen des Bildes');
            }
          });
          return;
        }

        if (this.selectedFile) {
          this.uploadNewImage(updatedNews.id, afterAll);
          return;
        }

        afterAll();
      },
      error: (err) => {
        console.error('Error updating news:', err);
        this.isLoading = false;
        this.toastr.error('Fehler beim Aktualisieren der News');
      }
    });
  }

  private uploadNewImage(newsId: number, done: () => void): void {
    this.newsService.uploadImage(newsId, this.selectedFile!).subscribe({
      next: () => done(),
      error: () => {
        this.isLoading = false;
        this.toastr.error('Bild konnte nicht hochgeladen werden');
      }
    });
  }

  private truncateText(text: string, maxLength: number = 50): string {
    if (text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + '...';
  }

  goBack(): void {
    if (this.newsData.id) {
      this.router.navigate(['/news/detail', this.newsData.id]);
    } else {
      this.router.navigate(['/news/home']);
    }
  }
}
