import { Component, OnInit } from '@angular/core';
import { NewsService } from '../../../services/news.service';
import {Router} from '@angular/router';
import { AuthService } from "../../../services/auth.service";
import { jwtDecode } from "jwt-decode";
import { ToastrService } from "ngx-toastr";
import { Page } from "../../../dtos/page";
import { SimpleNewsItem } from '../../../dtos/newsDtos/simple-news-item';
import { catchError, of, map } from 'rxjs';

@Component({
  selector: 'app-news-home',
  standalone: false,
  templateUrl: './news-home.component.html',
  styleUrls: ['./news-home.component.scss']
})
export class NewsHomeComponent implements OnInit {

  currentNewsPage: Page<SimpleNewsItem> | null = null;
  alreadySeenPage: Page<SimpleNewsItem> | null = null;

  currentNews: SimpleNewsItem[] = [];
  alreadySeen: SimpleNewsItem[] = [];

  isLoading = true;
  error: string | null = null;

  isAuthenticated = false;
  isAdmin = false;
  isUser = false;
  userRole: string | null = null;
  userName: string | null = null;

  currentPageCurrentNews = 0;
  currentPageAlreadySeen = 0;
  pageSize = 12;
  hasMoreCurrentNews = true;
  hasMoreAlreadySeen = true;
  isLoadingMoreCurrentNews = false;
  isLoadingMoreAlreadySeen = false;

  constructor(
    private newsService: NewsService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.checkAuthStatus();
    this.loadNews();
  }

  private checkAuthStatus(): void {
    this.isAuthenticated = this.authService.isLoggedIn();
    this.userRole = this.authService.getUserRole();
    this.isAdmin = this.userRole == 'ADMIN';
    this.isUser = this.userRole == 'USER';

    if (this.isAuthenticated) {
      const token = this.authService.getToken();
      if (token) {
        try {
          const decoded: any = jwtDecode(token);
          this.userName = decoded.sub || 'Benutzer';
        } catch (e) {
          this.toastr.error(
            e.error?.message || 'Fehler beim Token',
            'Fehler'
          );
        }
      }
    }

    console.log('Auth Status:', {
      isAuthenticated: this.isAuthenticated,
      userRole: this.userRole,
      isAdmin: this.isAdmin,
      isRegularUser: this.isUser
    });
  }

  loadNews(): void {
    this.isLoading = true;
    this.error = null;
    this.currentPageCurrentNews = 0;
    this.currentPageAlreadySeen = 0;

    if (this.isAuthenticated) {
      this.newsService.loadNewsBasedOnAuth().pipe(
        catchError(err => {
          console.error('Error loading news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden der News',
            'Fehler'
          );
          this.error = 'News konnten nicht geladen werden.';
          this.isLoading = false;

          return this.newsService.getAll().pipe(
            map(allNews => ({
              current: {
                content: allNews.content,
                totalElements: allNews.totalElements,
                totalPages: allNews.totalPages,
                size: allNews.size,
                number: allNews.number,
                first: allNews.first,
                last: allNews.last,
                empty: allNews.empty
              },
              alreadySeen: {
                content: [],
                totalElements: 0,
                totalPages: 0,
                size: this.pageSize,
                number: 0,
                first: true,
                last: true,
                empty: true
              }
            })),
            catchError(fallbackErr => {
              console.error('Fallback also failed:', fallbackErr);
              this.toastr.error(
                err.error?.message || 'Fehler beim Fallback',
                'Fehler'
              );
              return of({
                current: this.createEmptyPage(),
                alreadySeen: this.createEmptyPage()
              });
            })
          );
        })
      ).subscribe({
        next: ({ current, alreadySeen }) => {
          this.currentNewsPage = current;
          this.alreadySeenPage = alreadySeen;
          this.currentNews = current.content;
          this.alreadySeen = alreadySeen.content;
          this.hasMoreCurrentNews = !current.last;
          this.hasMoreAlreadySeen = !alreadySeen.last;
          this.isLoading = false;
          console.log(`Loaded ${current.content.length} current news and ${alreadySeen.content.length} already seen`);
        },
        error: (err) => {
          console.error('Unexpected error:', err);
          this.toastr.error(
            err.error?.message || 'Ein unerwarteter Fehler ist aufgetreten',
            'Fehler'
          );
          this.error = 'Ein unerwarteter Fehler ist aufgetreten.';
          this.isLoading = false;
        }
      });
    } else if (this.isAuthenticated && this.isAdmin) {
      this.loadNewsForAdmin();
    } else {
      this.loadAllNews();
    }
  }

  loadNewsForAdmin(): void {
    this.isLoading = true;
    this.error = null;
    this.currentPageCurrentNews = 0;
    this.currentPageAlreadySeen = 0;

    this.newsService.getAll(this.currentPageCurrentNews, this.pageSize).subscribe({
      next: (publishedPage) => {
        this.currentNewsPage = publishedPage;
        this.currentNews = publishedPage.content;
        this.hasMoreCurrentNews = !publishedPage.last;

        this.newsService.getUnpublished(this.currentPageAlreadySeen, this.pageSize).subscribe({
          next: (unpublishedPage) => {
            this.alreadySeenPage = unpublishedPage;
            this.alreadySeen = unpublishedPage.content;
            this.hasMoreAlreadySeen = !unpublishedPage.last;
            this.isLoading = false;
            console.log(`Admin loaded ${this.currentNews.length} published and ${this.alreadySeen.length} unpublished news`);
          },
          error: (err) => {
            console.error('Error loading unpublished news:', err);
            this.toastr.error(
              err.error?.message || 'Fehler beim Laden von nicht veröffentlichten News',
              'Fehler'
            );
            this.alreadySeen = [];
            this.alreadySeenPage = this.createEmptyPage();
            this.isLoading = false;
          }
        });
      },
      error: (err) => {
        console.error('Error loading published news:', err);
        this.toastr.error(
          err.error?.message || 'Fehler beim Laden von veröffentlichten News',
          'Fehler'
        );
        this.error = 'News konnten nicht geladen werden.';
        this.isLoading = false;
      }
    });
  }

  loadAllNews(): void {
    this.isLoading = true;
    this.error = null;
    this.currentPageCurrentNews = 0;

    this.newsService.getAll(this.currentPageCurrentNews, this.pageSize).subscribe({
      next: (allNewsPage) => {
        this.currentNewsPage = allNewsPage;
        this.currentNews = allNewsPage.content;
        this.alreadySeen = [];
        this.alreadySeenPage = this.createEmptyPage();
        this.hasMoreCurrentNews = !allNewsPage.last;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading public news:', err);
        this.toastr.error(
          err.error?.message || 'Fehler beim Löschen',
          'Fehler'
        );
        this.error = 'News konnten nicht geladen werden.';
        this.isLoading = false;
      }
    });
  }

  loadMoreCurrentNews(): void {
    if (this.isLoadingMoreCurrentNews || !this.hasMoreCurrentNews) {
      return;
    }

    this.isLoadingMoreCurrentNews = true;
    this.currentPageCurrentNews++;

    if (this.isAuthenticated && this.isUser) {
      this.newsService.getUnread(this.currentPageCurrentNews, this.pageSize).subscribe({
        next: (page) => {
          this.currentNewsPage = page;
          this.currentNews = [...this.currentNews, ...page.content];
          this.hasMoreCurrentNews = !page.last;
          this.isLoadingMoreCurrentNews = false;
        },
        error: (err) => {
          console.error('Error loading more unread news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von mehr News',
            'Fehler'
          );
          this.isLoadingMoreCurrentNews = false;
        }
      });
    } else if (this.isAuthenticated && this.isAdmin) {
      this.newsService.getAll(this.currentPageCurrentNews, this.pageSize).subscribe({
        next: (page) => {
          this.currentNewsPage = page;
          this.currentNews = [...this.currentNews, ...page.content];
          this.hasMoreCurrentNews = !page.last;
          this.isLoadingMoreCurrentNews = false;
        },
        error: (err) => {
          console.error('Error loading more published news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von mehr veröffentlichten News',
            'Fehler'
          );
          this.isLoadingMoreCurrentNews = false;
        }
      });
    } else {
      this.newsService.getAll(this.currentPageCurrentNews, this.pageSize).subscribe({
        next: (page) => {
          this.currentNewsPage = page;
          this.currentNews = [...this.currentNews, ...page.content];
          this.hasMoreCurrentNews = !page.last;
          this.isLoadingMoreCurrentNews = false;
        },
        error: (err) => {
          console.error('Error loading more public news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von öffentlichen News',
            'Fehler'
          );
          this.isLoadingMoreCurrentNews = false;
        }
      });
    }
  }

  loadMoreAlreadySeen(): void {
    if (this.isLoadingMoreAlreadySeen || !this.hasMoreAlreadySeen) {
      return;
    }

    this.isLoadingMoreAlreadySeen = true;
    this.currentPageAlreadySeen++;

    if (this.isAuthenticated && this.isUser) {
      this.newsService.getRead(this.currentPageAlreadySeen, this.pageSize).subscribe({
        next: (page) => {
          this.alreadySeenPage = page;
          this.alreadySeen = [...this.alreadySeen, ...page.content];
          this.hasMoreAlreadySeen = !page.last;
          this.isLoadingMoreAlreadySeen = false;
        },
        error: (err) => {
          console.error('Error loading more read news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von aktuellen News',
            'Fehler'
          );
          this.isLoadingMoreAlreadySeen = false;
        }
      });
    } else if (this.isAuthenticated && this.isAdmin) {
      this.newsService.getUnpublished(this.currentPageAlreadySeen, this.pageSize).subscribe({
        next: (page) => {
          this.alreadySeenPage = page;
          this.alreadySeen = [...this.alreadySeen, ...page.content];
          this.hasMoreAlreadySeen = !page.last;
          this.isLoadingMoreAlreadySeen = false;
        },
        error: (err) => {
          console.error('Error loading more unpublished news:', err);
          this.toastr.error(
            err.error?.message || 'Fehler beim Laden von bereits gelesenen News',
            'Fehler'
          );
          this.isLoadingMoreAlreadySeen = false;
        }
      });
    }
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

  editNews(newsId: number, event: MouseEvent): void {
    this.router.navigate(['admin/news/edit', newsId]);
  }

  deleteNews(newsId: number): void {
    this.newsService.deleteNews(newsId).subscribe({
      next: () => {
        this.toastr.success('News wurde gelöscht', 'Erfolg');
        this.loadNews();
      },
      error: (err) => {
        this.toastr.error(
          err.error?.message || 'Fehler beim Löschen',
          'Fehler'
        );
      }
    });
  }

  navigateToCreateNews() {
    this.router.navigate(['admin/news/creation']);
  }

  getNewsImageUrl(newsId: number): string {
    return this.newsService.getImageUrl(newsId);
  }

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = 'assets/placeholder-news-image.jpg';
  }

  private createEmptyPage(): Page<SimpleNewsItem> {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: this.pageSize,
      number: 0,
      first: true,
      last: true,
      empty: true
    };
  }
}
