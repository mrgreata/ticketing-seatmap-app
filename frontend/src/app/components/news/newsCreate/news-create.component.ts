import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import { CommonModule } from '@angular/common';
import { NewsService } from '../../../services/news.service';
import { AuthService } from '../../../services/auth.service';
import { CreateNewsItem } from '../../../dtos/newsDtos/create-news-item';
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-news-create',
  standalone: true,
  templateUrl: './news-create.component.html',
  styleUrls: ['./news-create.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FormsModule]
})
export class NewsCreateComponent implements OnInit {
  newsData: CreateNewsItem = {
    title: '',
    summary: '',
    text: '',
    publishedAt: new Date().toISOString().split('T')[0]
  };

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  isUploading = false;
  isLoading = false;
  error: string | null = null;
  successMessage: string | null = null;
  imageValidationError: string | null = null;


  isAuthenticated = false;
  isAdmin = false;
  userRole: string | null = null;

  constructor(
    private newsService: NewsService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.checkAuthStatus();
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isAdmin = this.userRole === 'ADMIN';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.selectedFile = null;
    this.previewUrl = null;
    this.imageValidationError = null;

    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const validationError = this.newsService.validateImageFile(file);
      if (validationError) {
        this.toastr.error(validationError, 'Ungültiges Bild');
        input.value = null;
        return;
      }

      this.selectedFile = file;

      const reader = new FileReader();
      reader.onload = (e: ProgressEvent<FileReader>) => {
        this.previewUrl = e.target?.result as string;
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  removeImage(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.newsData.imageData = null;
  }

  async onSubmit(): Promise<void> {
    if (!this.newsData.title || !this.newsData.summary || !this.newsData.text) {
      this.error = 'Bitte füllen Sie alle Pflichtfelder aus';
      return;
    }

    this.isLoading = true;
    this.successMessage = null;

    try {
      if (this.newsData.publishedAt) {
        const date = new Date(this.newsData.publishedAt);
        if (!isNaN(date.getTime())) {
          this.newsData.publishedAt = date.toISOString();
        }
      }

      this.newsService.createNews(this.newsData).subscribe({
        next: (createdNews) => {
          const navigateToDetail = () => {
            this.toastr.success(
              `Veranstaltung "${createdNews.title}" wurde erfolgreich erstellt!`,
              'Erfolg',
              {timeOut: 3000}
            );
            this.isLoading = false;
            setTimeout(() => {
              this.router.navigate(['/news/detail', createdNews.id]);
            }, 500);
          };

          if (this.selectedFile) {
            this.newsService.uploadImage(createdNews.id, this.selectedFile).subscribe( {
              next: () => {
                navigateToDetail();
              },
              error: (error) => {
                console.error('Error uploading news image :', error);
                this.isLoading = false;
                this.toastr.error(
                  error.error?.message || 'Newsbild konnte nicht hochgeladen werden',
                  'Fehler',
                  {timeOut: 5000}
                );

              }
            })
          } else {
            navigateToDetail();
          }
        },
        error: (err) => {
          this.toastr.error(
            err.error?.message || 'Fehler beim Erstellen der News',
            'Fehler',
            {timeOut: 5000}
          );
          this.isLoading = false;
        }
      });
    } catch (error: any) {
      this.toastr.error(
        error.error?.message || 'Ein Fehler ist aufgetreten',
        'Fehler',
        {timeOut: 5000}
      );
      this.isLoading = false;
    }
  }

  goBack(): void {
    this.router.navigate(['/news/home']);
  }
}
