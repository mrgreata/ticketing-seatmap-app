import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Globals } from '../global/globals';
import { ApiSeatDto } from '../dtos/seatmap.dto';

@Injectable({
  providedIn: 'root'
})
export class SeatService {

  private baseUrl: string = this.globals.backendUri + '/seats';

  constructor(
    private http: HttpClient,
    private globals: Globals
  ) {}

  /**
   * Holt einen einzelnen Seat nach ID
   */
  getSeatById(id: number): Observable<ApiSeatDto> {
    return this.http.get<ApiSeatDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Optional: Alle Seats in einem Sector holen
   */
  getSeatsBySector(sectorId: number): Observable<ApiSeatDto[]> {
    return this.http.get<ApiSeatDto[]>(`${this.baseUrl}/sector/${sectorId}`);
  }

  /**
   * Optional: Seat erstellen (Admin)
   */
  createSeat(seat: Partial<ApiSeatDto>): Observable<ApiSeatDto> {
    return this.http.post<ApiSeatDto>(`${this.baseUrl}`, seat);
  }
}
