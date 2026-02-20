import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {Location} from '../dtos/location';
import {Event} from '../dtos/event';

/**
 * Service for managing location/venue-related operations.
 * Provides functionality for location search, retrieval, and querying events by location.
 * Handles communication with the backend locations API endpoint.
 */
@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private baseUri: string = this.globals.backendUri + '/locations';

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) {
  }

  /**
   * Retrieves all locations/venues from the system.
   *
   * @returns Observable of all locations as an array
   */
  getAllLocations(): Observable<Location[]> {
    return this.httpClient.get<Location[]>(this.baseUri);
  }

  /**
   * Searches for locations using multiple filter criteria.
   * All parameters are optional and can be combined for advanced filtering.
   * Search supports partial matching for text fields.
   *
   * @param name - Optional venue name search term (supports partial matching)
   * @param street - Optional street address search term (supports partial matching)
   * @param city - Optional city name search term (supports partial matching)
   * @param zipCode - Optional postal/ZIP code for exact matching
   * @returns Observable of locations matching the search criteria
   */
  searchLocations(name?: string, street?: string, city?: string, zipCode?: number): Observable<Location[]> {
    let params = new HttpParams();

    if (name) params = params.set('name', name);

    if (street) params = params.set('street', street);

    if (city) params = params.set('city', city);

    if (zipCode) params = params.set('zipCode', zipCode.toString());

    return this.httpClient.get<Location[]>(`${this.baseUri}/search`, {params});
  }

  /**
   * Retrieves detailed information for a specific location by its ID.
   * Includes venue details such as name, address, and hall/sector information.
   *
   * @param id - The unique identifier of the location
   * @returns Observable of the location details
   * @throws NotFoundException if the location with the given ID does not exist
   */
  getLocationById(id: number): Observable<Location> {
    return this.httpClient.get<Location>(`${this.baseUri}/${id}`);
  }

  /**
   * Retrieves all events scheduled at a specific location/venue.
   * Useful for displaying venue-specific event listings.
   *
   * @param locationId - The unique identifier of the location
   * @returns Observable of all events at the specified location
   * @throws NotFoundException if the location does not exist
   */
  getEventsByLocation(locationId: number): Observable<Event[]> {
    return this.httpClient.get<Event[]>(`${this.baseUri}/${locationId}/events`);
  }
}
