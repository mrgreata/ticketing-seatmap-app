import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SimpleReservation,
  TicketCancelled,
  TicketCreate,
  TicketPurchased,
  TicketReserved
} from '../dtos/ticket';
import { Globals } from "../global/globals";

@Injectable({
  providedIn: 'root'
})
export class TicketService {

  private baseUri: string = this.globals.backendUri;

  /**
   * Creates an instance of TicketService
   * @param http - The HttpClient for making HTTP requests
   * @param globals - The Globals service providing backend URI configuration
   */
  constructor(
    private http: HttpClient,
    private globals: Globals
  ) {}

  // ---------------------------------------------------------------
  // Ticket purchase methods
  // ---------------------------------------------------------------

  /**
   * Purchases tickets by their IDs
   * @param ticketIds - Array of ticket IDs to purchase
   * @returns Observable of TicketPurchased array
   */
  purchaseTickets(ticketIds: number[]): Observable<TicketPurchased[]> {
    return this.http.patch<TicketPurchased[]>(`${this.baseUri}/tickets/purchasing`, ticketIds);
  }

  /**
   * Creates new tickets
   * @param tickets - Array of TicketCreate DTOs
   * @returns Observable of TicketPurchased array
   */
  createTicket(tickets: TicketCreate[]): Observable<TicketPurchased[]> {
    return this.http.post<TicketPurchased[]>(`${this.baseUri}/tickets`, tickets);
  }

  // ---------------------------------------------------------------
  // Ticket reservation methods
  // ---------------------------------------------------------------

  /**
   * Reserves tickets by their IDs
   * @param ticketIds - Array of ticket IDs to reserve
   * @returns Observable of reservation response
   */
  reserveTickets(ticketIds: number[]): Observable<any> {
    return this.http.patch(
      `${this.baseUri}/reservations`,
      ticketIds
    );
  }

  /**
   * Retrieves all reservations for the current user
   * @returns Observable of TicketReserved array
   */
  getMyReservations(): Observable<TicketReserved[]> {
    return this.http.get<TicketReserved[]>(
      `${this.baseUri}/reservations/my`
    );
  }

  /**
   * Retrieves a specific reservation by its ID
   * @param id - The ID of the reservation to retrieve
   * @returns Observable of SimpleReservation
   */
  getReservationById(id: number): Observable<SimpleReservation> {
    return this.http.get<SimpleReservation>(
      `${this.baseUri}/reservations/${id}`
    );
  }

  // ---------------------------------------------------------------
  // Ticket cancellation methods
  // ---------------------------------------------------------------

  /**
   * Cancels purchased tickets by their IDs
   * @param ticketIds - Array of ticket IDs to cancel
   * @returns Observable of cancellation response
   */
  cancelTickets(ticketIds: number[]): Observable<any> {
    console.log(ticketIds);
    return this.http.delete(
      `${this.baseUri}/tickets`,
      { body: ticketIds }
    );
  }

  /**
   * Cancels reservations by their IDs
   * @param ids - Array of reservation IDs to cancel
   * @returns Observable of cancellation response
   */
  cancelReservation(ids: number[]): Observable<any> {
    return this.http.patch(`${this.baseUri}/reservations/cancellation`, ids);
  }

  // ---------------------------------------------------------------
  // User ticket retrieval methods
  // ---------------------------------------------------------------

  /**
   * Retrieves all purchased tickets for the current user
   * @returns Observable of TicketPurchased array
   */
  getMyTickets(): Observable<TicketPurchased[]> {
    return this.http.get<TicketPurchased[]>(
      `${this.baseUri}/tickets/my`
    );
  }

  /**
   * Retrieves all cancelled tickets for the current user
   * @returns Observable of TicketCancelled array
   */
  getMyCancelledTickets(): Observable<TicketCancelled[]> {
    return this.http.get<TicketCancelled[]>(
      `${this.baseUri}/tickets/cancelled/my`
    );
  }
}
