import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {catchError, map, Observable, of} from 'rxjs';
import {Globals} from '../global/globals';
import {Event} from '../dtos/event';
import {EventDetail} from '../dtos/event-detail';
import {TopTenEvent} from '../dtos/top-ten-event';
import {Page} from '../dtos/page';

/**
 * Service for managing event-related operations.
 * Provides functionality for event creation, search, updates, and retrieval of event data.
 * Handles communication with the backend events API endpoint.
 */
@Injectable({
  providedIn: 'root'
})
export class EventService {
  private baseUri: string = this.globals.backendUri + '/events';
  private imageLoadTimestamp: number = new Date().getTime();

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) {
  }

  /**
   * Retrieves all events from the system with pagination.
   *
   * @param page - Page number (0-indexed)
   * @param size - Number of events per page
   * @returns Observable of paginated events
   */
  getAllEvents(page: number = 0, size: number = 12): Observable<Page<Event>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.httpClient.get<Page<Event>>(this.baseUri, {params});
  }

  /**
   * Searches for events using multiple filter criteria with pagination.
   * All parameters are optional and can be combined for advanced filtering.
   *
   * @param title - Optional title search term (supports partial matching)
   * @param type - Optional event type filter (e.g., 'concert', 'theater', 'opera', 'festival')
   * @param duration - Optional duration in minutes (applies ±30 minute tolerance)
   * @param dateFrom - Optional start date filter in ISO 8601 format (e.g., '2026-01-19T12:00:00')
   * @param dateTo - Optional end date filter in ISO 8601 format
   * @param locationId - Optional location ID to filter events by venue
   * @param priceMin - Optional minimum price in cents (e.g., 1000 for €10.00)
   * @param priceMax - Optional maximum price in cents
   * @param page - Page number (0-indexed)
   * @param size - Number of events per page
   * @returns Observable of paginated events matching the search criteria
   */
  searchEvents(
    title?: string,
    type?: string,
    duration?: number,
    dateFrom?: string,
    dateTo?: string,
    locationId?: number,
    priceMin?: number,
    priceMax?: number,
    page: number = 0,
    size: number = 12
  ): Observable<Page<Event>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (title) {
      params = params.set('title', title);
    }

    if (type) {
      params = params.set('type', type);
    }

    if (duration) {
      params = params.set('duration', duration.toString());
    }

    if (dateFrom) {
      params = params.set('dateFrom', dateFrom);
    }

    if (dateTo) {
      params = params.set('dateTo', dateTo);
    }

    if (locationId) {
      params = params.set('locationId', locationId.toString());
    }

    if (priceMin) {
      params = params.set('priceMin', priceMin.toString());
    }

    if (priceMax) {
      params = params.set('priceMax', priceMax.toString());
    }

    return this.httpClient.get<Page<Event>>(`${this.baseUri}/search`, {params});
  }

  /**
   * Retrieves detailed information for a specific event by its ID.
   *
   * @param id - The unique identifier of the event
   * @returns Observable of the event details
   * @throws NotFoundException if the event with the given ID does not exist
   */
  getEventById(id: number): Observable<EventDetail> {
    return this.httpClient.get<EventDetail>(`${this.baseUri}/${id}`);
  }

  /**
   * Retrieves the top ten events by ticket sales for a specific month and optional category.
   * Used for displaying popular events and sales statistics.
   *
   * @param month - The month number (1-12) for which to retrieve top events
   * @param year - The year for which to retrieve top events
   * @param type - Optional event type to filter by category (e.g., 'concert', 'theater')
   * @returns Observable of the top ten events with sales information
   */
  getTopTen(month: number, year: number, type?: string): Observable<TopTenEvent[]> {
    let params = new HttpParams()
      .set('month', month.toString())
      .set('year', year.toString());

    if (type) {
      params = params.set('type', type);
    }

    return this.httpClient.get<TopTenEvent[]>(`${this.baseUri}/top-ten`, {params});
  }

  /**
   * Creates a new event in the system.
   * Requires administrator privileges.
   *
   * @param event - The event data to create
   * @param event.title - The title/name of the event
   * @param event.type - The event type/category
   * @param event.durationMinutes - Optional duration in minutes
   * @param event.description - Optional detailed description
   * @param event.dateTime - The date and time of the event
   * @param event.locationId - The ID of the location/venue where the event takes place
   * @param event.artistIds - Optional array of artist IDs performing at the event
   * @returns Observable of the created event with assigned ID
   * @throws ValidationException if the event data is invalid
   * @throws NotFoundException if the referenced location or artists do not exist
   */
  createEvent(event: {
    title: string;
    type: string;
    durationMinutes?: number;
    description?: string;
    dateTime: string;
    locationId: number;
    artistIds?: number[];
  }): Observable<Event> {
    return this.httpClient.post<Event>(this.baseUri, event);
  }

  /**
   * Updates an existing event with new data.
   * Requires administrator privileges.
   *
   * @param id - The unique identifier of the event to update
   * @param event - The updated event data
   * @param event.id - Must match the id parameter
   * @param event.title - The updated title/name of the event
   * @param event.type - The updated event type/category
   * @param event.durationMinutes - Optional updated duration in minutes
   * @param event.description - Optional updated description
   * @param event.dateTime - The updated date and time
   * @param event.locationId - The updated location/venue ID
   * @param event.artistIds - Optional updated array of artist IDs
   * @returns Observable of the updated event
   * @throws ValidationException if the event data is invalid
   * @throws NotFoundException if the event, location, or artists do not exist
   * @throws ConflictException if tickets have already been sold and the update would conflict
   */
  updateEvent(id: number, event: {
    id: number;
    title: string;
    type: string;
    durationMinutes?: number;
    description?: string;
    dateTime: string;
    locationId: number;
    artistIds?: number[];
  }): Observable<EventDetail> {
    return this.httpClient.put<EventDetail>(`${this.baseUri}/${id}`, event);
  }

  /**
   * Deletes an event from the system.
   * Requires administrator privileges.
   *
   * @param id - The unique identifier of the event to delete
   * @returns Observable that completes when the event is deleted
   * @throws NotFoundException if the event does not exist
   * @throws ConflictException if tickets have already been sold for this event
   */
  deleteEvent(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.baseUri}/${id}`);
  }

  /**
   * Uploads an image for a specific event.
   * The image will be used as the event's promotional/display image.
   *
   * @param eventId - The ID of the event to upload the image for
   * @param image - The image file to upload (supported formats: JPG, PNG)
   * @returns Observable that completes when the upload is successful
   * @throws ValidationException if the file format is not supported
   * @throws NotFoundException if the event does not exist
   */
  uploadImage(eventId: number, image: File): Observable<void> {
    const formData = new FormData();
    formData.append('image', image);
    return this.httpClient.post<void>(`${this.baseUri}/${eventId}/image`, formData);
  }

  /**
   * Deletes the image of a specific event.
   * Requires administrator privileges.
   *
   * @param eventId - The ID of the event whose image should be deleted
   * @returns Observable that completes when the image is deleted
   * @throws NotFoundException if the event does not exist or has no image
   */
  deleteImage(eventId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.baseUri}/${eventId}/image`);
  }

  /**
   * Constructs the URL for retrieving an event's image with cache busting.
   * Can be used directly in image src attributes.
   *
   * @param eventId - The ID of the event
   * @param bustCache - Optional: Force a new timestamp (default: false)
   * @returns The full URL to the event's image resource
   */
  getImageUrl(eventId: number, bustCache: boolean = false): string {
    const timestamp = bustCache ? new Date().getTime() : this.imageLoadTimestamp;
    return `${this.baseUri}/${eventId}/image?t=${timestamp}`;
  }

  /**
   * Invalidates the image cache for a specific event.
   * Call this after uploading a new image to force browser reload.
   */
  refreshImageCache(): void {
    this.imageLoadTimestamp = new Date().getTime();
  }

  /**
   * Checks if an event has an associated image without loading the full image data.
   *
   * @param eventId - The ID of the event to check
   * @returns Observable<boolean> - true if the event has an image, false otherwise
   */
  checkImageExists(eventId: number): Observable<boolean> {
    return this.httpClient.get(`${this.baseUri}/${eventId}/image`, {
      observe: 'response',
      responseType: 'blob'
    }).pipe(
      map((response) => response.status === 200),
      catchError(() => of(false))
    );
  }
}
