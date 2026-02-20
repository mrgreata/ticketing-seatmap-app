import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError} from 'rxjs';
import { AuthService } from './auth.service';
import { InvoiceCreate, SimpleInvoice, DetailedInvoice } from '../dtos/invoice';

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {

  private baseUri = '/api/v1/invoices';

  /**
   * Creates an instance of InvoiceService
   * @param http - The HttpClient for making HTTP requests
   * @param authService - The AuthService for authentication checks
   */
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // -------------------------------------------------------
  // GET methods
  // -------------------------------------------------------

  /**
   * Retrieves all invoices for the currently logged-in user
   * @returns Observable of SimpleInvoice array or empty array if user is not logged in
   */
  getMyInvoices(): Observable<SimpleInvoice[]> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, returning empty invoices');
      return of([]);
    }

    return this.http.get<SimpleInvoice[]>(`${this.baseUri}/my`).pipe(
      catchError(err => {
        console.error('Error fetching invoices:', err);
        if (err.status === 401 || err.status === 403) {
          return of([]);
        }
        return of([]);
      })
    );
  }

  /**
   * Retrieves all credit invoices (cancellation invoices) for the currently logged-in user
   * @returns Observable of DetailedInvoice array
   */
  getMyCreditInvoices(): Observable<DetailedInvoice[]> {
    return this.http.get<DetailedInvoice[]>(`${this.baseUri}/my/credits`);
  }

  /**
   * Retrieves all merchandise invoices for the currently logged-in user
   * @returns Observable of SimpleInvoice array or empty array if user is not logged in
   */
  getMyMerchandiseInvoices(): Observable<SimpleInvoice[]> {
    if (!this.authService.isLoggedIn()) {
      return of([]);
    }

    return this.http.get<SimpleInvoice[]>(`${this.baseUri}/my/merchandise`).pipe(
      catchError(err => {
        console.error('Error fetching merchandise invoices:', err);
        if (err.status === 401 || err.status === 403) {
          return of([]);
        }
        return of([]);
      })
    );
  }

  /**
   * Retrieves a specific invoice by its ID
   * @param id - The ID of the invoice to retrieve
   * @returns Observable of DetailedInvoice or null if user is not logged in
   */
  getById(id: number): Observable<DetailedInvoice | null> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, skipping getById');
      return of(null);
    }

    return this.http.get<DetailedInvoice>(`${this.baseUri}/${id}`).pipe(
      catchError(err => {
        console.error(`Error loading invoice ${id}:`, err);
        if (err.status === 401 || err.status === 403) {
          return of(null);
        }
        return of(null);
      })
    );
  }

  // -------------------------------------------------------
  // CREATE methods
  // -------------------------------------------------------

  /**
   * Creates a new invoice
   * @param dto - The invoice data to create
   * @returns Observable of SimpleInvoice or null if user is not logged in
   */
  create(dto: InvoiceCreate): Observable<SimpleInvoice | null> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, skipping create invoice');
      return of(null);
    }

    return this.http.post<SimpleInvoice>(this.baseUri, dto).pipe(
      catchError(err => {
        console.error('Error creating invoice:', err);
        return of(null);
      })
    );
  }

  // -------------------------------------------------------
  // DOWNLOAD methods
  // -------------------------------------------------------

  /**
   * Downloads an invoice PDF by its ID
   * @param id - The ID of the invoice to download
   * @returns Observable of Blob or null if user is not logged in
   */
  downloadPdf(id: number): Observable<Blob | null> {
    if (!this.authService.isLoggedIn()) {
      console.log('User not logged in, skipping pdf download');
      return of(null);
    }

    return this.http.get(`${this.baseUri}/${id}/download`, {
      responseType: 'blob'
    }).pipe(
      catchError(err => {
        console.error('Error downloading invoice pdf:', err);
        return of(null);
      })
    );
  }

  /**
   * Downloads a credit invoice (cancellation invoice) PDF for specific tickets
   * @param ticketIds - Array of ticket IDs to generate credit invoice for
   * @returns Observable of Blob
   */
  downloadCreditPdf(ticketIds: number[]): Observable<Blob> {
    console.log(ticketIds);
    return this.http.post(
      `${this.baseUri}/credit`,
      ticketIds,
      { responseType: 'blob' }
    );
  }

  /**
   * Downloads a credit invoice PDF by its ID
   * @param creditInvoiceId - The ID of the credit invoice to download
   * @returns Observable of Blob
   */
  downloadCreditPdfById(creditInvoiceId: number): Observable<Blob> {
    return this.http.get(`${this.baseUri}/credit/${creditInvoiceId}/download`,
      { responseType: 'blob' }
    );
  }

  // -------------------------------------------------------
  // OPEN/DISPLAY methods
  // -------------------------------------------------------

  /**
   * Opens an invoice PDF in a new browser tab
   * @param id - The ID of the invoice to open
   */
  openPdf(id: number): void {
    this.downloadPdf(id).subscribe(blob => {
      if (!blob) return;
      const url = window.URL.createObjectURL(blob);
      window.open(url);
    });
  }

}
