import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiSeatmapDto } from '../dtos/seatmap.dto';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SeatmapService {

  private baseUrl = environment.backendUrl ?? 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  getSeatmap(eventId: number): Observable<ApiSeatmapDto> {
    return this.http.get<ApiSeatmapDto>(`${this.baseUrl}/events/${eventId}/seatmap`);
  }
}
