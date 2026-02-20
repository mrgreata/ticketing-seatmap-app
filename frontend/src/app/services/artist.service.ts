import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Artist } from '../dtos/artist';

/**
 * Service for managing artist-related operations.
 * Provides functionality for artist creation, search, retrieval, and querying events by artist.
 * Supports both individual artists and bands.
 * Handles communication with the backend artists API endpoint.
 */
@Injectable({
  providedIn: 'root'
})
export class ArtistService {
  private baseUri: string = environment.backendUrl + '/artists';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves all artists and bands from the system.
   *
   * @returns Observable of all artists/bands as an array
   */
  getAllArtists(): Observable<Artist[]> {
    return this.http.get<Artist[]>(this.baseUri);
  }

  /**
   * Retrieves detailed information for a specific artist by their ID.
   * Includes information about band memberships if applicable.
   *
   * @param id - The unique identifier of the artist
   * @returns Observable of the artist details
   * @throws NotFoundException if the artist with the given ID does not exist
   */
  getArtistById(id: number): Observable<Artist> {
    return this.http.get<Artist>(`${this.baseUri}/${id}`);
  }

  /**
   * Creates a new artist or band in the system.
   *
   * @param artist - The artist data to create
   * @param artist.name - The name of the artist or band
   * @param artist.isBand - Optional flag indicating if this is a band (default: false)
   * @param artist.memberIds - Optional array of artist IDs who are members (only for bands)
   * @returns Observable of the created artist with assigned ID
   * @throws ValidationException if the artist data is invalid
   * @throws NotFoundException if referenced member IDs do not exist
   */
  createArtist(artist: { name: string; isBand?: boolean; memberIds?: number[] }): Observable<Artist> {
    return this.http.post<Artist>(this.baseUri, artist);
  }

  /**
   * Searches for artists by name with optional band inclusion.
   * Supports searching for both individual artists and band names.
   *
   * @param name - The search term for artist/band name (supports partial matching)
   * @param includeBands - If true, also returns bands where the artist is a member (default: true)
   * @returns Observable of artists matching the search criteria
   */
  searchArtists(name: string, includeBands: boolean = true): Observable<Artist[]> {
    let params = new HttpParams()
      .set('name', name)
      .set('includeBands', includeBands.toString());
    return this.http.get<Artist[]>(`${this.baseUri}/search`, { params });
  }

  /**
   * Retrieves all events featuring a specific artist or band.
   * Useful for displaying artist-specific event listings.
   *
   * @param artistId - The unique identifier of the artist or band
   * @returns Observable of all events featuring the specified artist
   * @throws NotFoundException if the artist does not exist
   */
  getEventsByArtist(artistId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUri}/${artistId}/events`);
  }
}
