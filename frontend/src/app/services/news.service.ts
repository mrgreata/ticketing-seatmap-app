import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, map, forkJoin, catchError, of} from 'rxjs';
import { SimpleNewsItem } from '../dtos/newsDtos/simple-news-item';
import { DetailedNewsItem } from '../dtos/newsDtos/detailed-news-item';
import {AuthService} from "./auth.service";
import {CreateNewsItem} from "../dtos/newsDtos/create-news-item";
import {UpdateNewsItem} from "../dtos/newsDtos/update-news-item";
import {Page} from "../dtos/page";

/**
 * Service for managing news operations including creation, retrieval, update,
 * deletion, and image handling. Provides methods for both simple and detailed
 * news items, as well as categorized views based on user authentication.
 */
@Injectable({
  providedIn: 'root'
})
export class NewsService {

  private baseUri = '/api/v1/news';
  private defaultPageSize = 12;

  /**
   * Creates an instance of NewsService.
   *
   * @param http - HttpClient for making HTTP requests
   * @param authService - Service for authentication and authorization checks
   */
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Retrieves all news items (that are published) available in the system.
   * Transforms image data to data URLs and formats dates for display.
   *
   * @param page - Page number (0-based)
   * @param size - Page size
   * @returns Observable emitting an array of SimpleNewsItem objects
   * @throws Observable emitting a Page of SimpleNewsItem objects
   */
  getAll(page: number = 0, size: number = this.defaultPageSize): Observable<Page<SimpleNewsItem>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<SimpleNewsItem>>(this.baseUri, { params }).pipe(
      map(page => this.mapPage(page)),
      catchError(err => {
        console.error('Error loading all news:', err);
        return of(this.createEmptyPage());
      })
    );
  }

  /**
   * Retrieves a page of unpublished news items.
   * Transforms image data to data URLs and formats dates for display.
   *
   * @param page - Page number (0-based)
   * @param size - Page size
   * @returns Observable emitting a Page of SimpleNewsItem objects
   */
  getUnpublished(page: number = 0, size: number = this.defaultPageSize): Observable<Page<SimpleNewsItem>> {
    if (!this.authService.isAdmin()) {
      console.log('Admin not logged in, returning empty unpublished');
      return of(this.createEmptyPage());
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<SimpleNewsItem>>(`${this.baseUri}/unpublished`, { params }).pipe(
      map(page => this.mapPage(page)),
      catchError(err => {
        console.error('Error loading unpublished news:', err);
        return of(this.createEmptyPage());
      })
    );
  }

  /**
   * Retrieves a page of unread news items for the current user.
   * Requires user authentication.
   *
   * @param page - Page number (0-based)
   * @param size - Page size
   * @returns Observable emitting a Page of SimpleNewsItem objects
   */
  getUnread(page: number = 0, size: number = this.defaultPageSize): Observable<Page<SimpleNewsItem>> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, returning empty unread');
      return of(this.createEmptyPage());
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<SimpleNewsItem>>(`${this.baseUri}/unread`, { params }).pipe(
      map(page => this.mapPage(page)),
      catchError(err => {
        console.error('Error loading unread news:', err);
        if (err.status === 401 || err.status === 403) {
          return of(this.createEmptyPage());
        }
        return of(this.createEmptyPage());
      })
    );
  }

  /**
   * Retrieves a page of read news items for the current user.
   * Requires user authentication.
   *
   * @param page - Page number (0-based)
   * @param size - Page size
   * @returns Observable emitting a Page of SimpleNewsItem objects
   */
  getRead(page: number = 0, size: number = this.defaultPageSize): Observable<Page<SimpleNewsItem>> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, returning empty read');
      return of(this.createEmptyPage());
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<SimpleNewsItem>>(`${this.baseUri}/read`, { params }).pipe(
      map(page => this.mapPage(page)),
      catchError(err => {
        console.error('Error loading read news:', err);
        if (err.status === 401 || err.status === 403) {
          return of(this.createEmptyPage());
        }
        return of(this.createEmptyPage());
      })
    );
  }

  /**
   * Retrieves detailed information about a specific news item by ID.
   * Includes image data converted to data URL.
   *
   * @param id - Unique identifier of the news item
   * @returns Observable emitting DetailedNewsItem with image URL
   * @throws Propagates HTTP errors from the server
   */
  getById(id: number): Observable<DetailedNewsItem> {
    return this.http.get<DetailedNewsItem>(`${this.baseUri}/${id}`).pipe(
      map(item => ({
        ...item,
        imageUrl: item.imageData ? this.byteArrayToDataUrl(item.imageData) : null
      }))
    );
  }

  /**
   * Marks a specific news item as read for the current user.
   * Requires user authentication; does nothing if not logged in.
   *
   * @param id - Unique identifier of the news item to mark as read
   * @returns Observable that completes when operation is done
   */
  markAsRead(id: number): Observable<void> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, skipping mark as read');
      return of(void 0);
    }

    return this.http.post<void>(`${this.baseUri}/${id}/mark-read`, {}).pipe(
      catchError(err => {
        console.error('Error marking news as read:', err);
        if (err.status === 401 || err.status === 403) {
          return of(void 0);
        }
        return of(void 0);
      })
    );
  }

  /**
   * Retrieves first page of news items categorized by read/unread status.
   * Returns all news as "unread" for unauthenticated users.
   *
   * @returns Observable emitting object with unread and read news pages
   */
  getNewsCategories(): Observable<{
    unread: Page<SimpleNewsItem>,
    read: Page<SimpleNewsItem>
  }> {
    const page = 0;
    const size = this.defaultPageSize;

    if (this.authService.isLoggedIn() && this.authService.isUser()) {
      console.log('User is logged in, loading categorized news');
      return forkJoin({
        unread: this.getUnread(page, size),
        read: this.getRead(page, size)
      });
    } else if (this.authService.isLoggedIn() && this.authService.isAdmin()) {
      console.log('Admin is logged in, loading categorized news');
      return forkJoin({
        unread: this.getAll(page, size),
        read: this.getUnpublished(page, size)
      });
    } else {
      console.log('User not logged in, loading all news as unread');
      return this.getAll(page, size).pipe(
        map(allNews => ({
          unread: allNews,
          read: this.createEmptyPage()
        }))
      );
    }
  }

  /**
   * Convenience method to load first page of news categorized for display purposes.
   * Returns news with more intuitive property names (current/alreadySeen).
   *
   * @returns Observable emitting object with current (unread) and alreadySeen (read) news pages
   */
  loadNewsBasedOnAuth(): Observable<{
    current: Page<SimpleNewsItem>,
    alreadySeen: Page<SimpleNewsItem>
  }> {
    return this.getNewsCategories().pipe(
      map(({ unread, read }) => ({
        current: unread,
        alreadySeen: read
      }))
    );
  }

  /**
   * Creates a new news item in the system.
   * Requires ADMIN role; throws error if user is not authorized.
   *
   * @param newsData - Data for creating the news item
   * @returns Observable emitting the created DetailedNewsItem
   * @throws Error if user is not ADMIN
   * @throws Propagates HTTP errors from the server
   */
  createNews(newsData: CreateNewsItem): Observable<DetailedNewsItem> {
    if (this.authService.getUserRole() !== 'ADMIN') {
      throw new Error('Nur Administratoren können News erstellen');
    }

    const dataToSend = {
      ...newsData,
      publishedAt: newsData.publishedAt || new Date().toISOString()
    };

    return this.http.post<DetailedNewsItem>(this.baseUri, dataToSend).pipe(
      map(item => ({
        ...item,
        imageUrl: item.imageData ? this.byteArrayToDataUrl(item.imageData) : null
      }))
    );
  }

  /**
   * Updates an existing news item.
   * Requires ADMIN role; throws error if user is not authorized.
   *
   * @param newsData - Updated data for the news item including ID
   * @returns Observable emitting the updated DetailedNewsItem
   * @throws Error if user is not ADMIN
   * @throws Propagates HTTP errors from the server
   */
  updateNews(newsData: UpdateNewsItem): Observable<DetailedNewsItem> {
    if (this.authService.getUserRole() !== 'ADMIN') {
      throw new Error('Nur Administratoren können News bearbeiten');
    }

    return this.http.put<DetailedNewsItem>(
      `${this.baseUri}/${newsData.id}`,
      newsData
    ).pipe(
      map(item => ({
        ...item,
        imageUrl: item.imageData
          ? this.byteArrayToDataUrl(item.imageData)
          : null
      }))
    );
  }

  /**
   * Deletes a news item from the system.
   * Requires ADMIN role; throws error if user is not authorized.
   *
   * @param id - Unique identifier of the news item to delete
   * @returns Observable that completes when deletion is successful
   * @throws Error if user is not ADMIN
   * @throws Propagates HTTP errors from the server
   */
  deleteNews(id: number): Observable<void> {
    if (this.authService.getUserRole() !== 'ADMIN') {
      throw new Error("Nur Admins können News löschen");
    }

    return this.http.delete<void>(`${this.baseUri}/${id}`);
  }

  /**
   * Uploads an image for a specific news item.
   *
   * @param newsId - Unique identifier of the news item
   * @param image - File object containing the image
   * @returns Observable that completes when upload is successful
   */
  uploadImage(newsId: number, image: File): Observable<void> {
    const formData = new FormData();
    formData.append('image', image);
    return this.http.post<void>(`${this.baseUri}/${newsId}/image`, formData);
  }

  /**
   * Deletes the image of a specific event.
   * Requires administrator privileges.
   *
   * @param newsId - The ID of the event whose image should be deleted
   * @returns Observable that completes when the image is deleted
   * @throws NotFoundException if the event does not exist or has no image
   */
  deleteImage(newsId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUri}/${newsId}/image`);
  }

  /**
   * Gets the URL for accessing a news item's image.
   *
   * @param newsId - Unique identifier of the news item
   * @returns URL string for the image endpoint
   */
  getImageUrl(newsId: number): string {
    return `${this.baseUri}/${newsId}/image`;
  }


  /**
   * Validates an image file before upload.
   *
   * @param file - File to validate
   * @returns Error message string if validation fails, null if valid
   */
  validateImageFile(file: File): string | null {
    if (!file.type.match(/^image\/(jpeg|jpg|png|gif)$/)) {
      return 'Nur JPG, PNG oder GIF Dateien sind erlaubt';
    }

    if (file.size > 3 * 1024 * 1024) {
      return 'Bild darf maximal 3 MB groß sein';
    }

    return null;
  }

  /**
   * Converts a byte array to a data URL for image display.
   *
   * @param byteArray - Array of byte values representing image data
   * @returns Data URL string or empty string if input is empty
   */
  private byteArrayToDataUrl(byteArray: number[]): string {
    if (!byteArray || byteArray.length === 0) return '';
    const binary = byteArray.map(b => String.fromCharCode(b)).join('');
    return 'data:image/jpeg;base64,' + btoa(binary);
  }

  /**
   * Transforms a page by adding image URLs and formatted dates to items.
   *
   * @param page - Page to transform
   * @returns Transformed Page with imageUrl and formattedDate properties
   */
  private mapPage(page: Page<SimpleNewsItem>): Page<SimpleNewsItem> {
    const mappedContent = page.content.map(item => ({
      ...item,
      imageUrl: item.imageData ? this.byteArrayToDataUrl(item.imageData) : null,
      formattedDate: this.formatDate(item.publishedAt)
    }));

    return {
      ...page,
      content: mappedContent
    };
  }

  /**
   * Formats a date string to German locale format (DD.MM.YYYY).
   *
   * @param dateString - ISO date string to format
   * @returns Formatted date string or original string if formatting fails
   */
  private formatDate(dateString: string): string {
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

  /**
   * Creates an empty page.
   *
   * @returns Empty Page
   */
  private createEmptyPage(): Page<SimpleNewsItem> {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: this.defaultPageSize,
      number: 0,
      first: true,
      last: true,
      empty: true
    };
  }
}
