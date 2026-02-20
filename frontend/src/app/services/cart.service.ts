import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Globals} from "../global/globals";
import {
  CartDto,
  CartAddMerchandiseItemDto,
  CartCheckoutRequestDto,
  CartUpdateItemDto,
  CartCheckoutResultDto,
} from "../dtos/cartDtos/cart";

/**
 * Service responsible for managing the authenticated user's shopping cart.
 */
@Injectable({ providedIn: 'root'})
export class CartService {

  /**
   * Base URI for all cart-related backend endpoints.
   */
  private readonly baseUri: string


  /**
   * Creates a new CartService.
   *
   * @param http Angular HttpClient used for REST communication
   * @param globals Global configuration providing the backend base URI
   */
  constructor(private http: HttpClient, private globals: Globals) {
    this.baseUri = this.globals.backendUri + '/cart';
  }


  /**
   * Retrieves the current cart of the authenticated user.
   *
   * @returns Observable emitting the user's cart including all items and tickets
   */
  getMyCart(): Observable<CartDto> {
    return this.http.get<CartDto>(this.baseUri);
  }


  /**
   * Adds a merchandise item to the cart.
   *
   * If the item already exists in the cart, the backend is responsible
   * for updating the quantity accordingly.
   *
   * @param dto Data transfer object describing the merchandise item to add
   * @returns Observable emitting the updated cart
   */
  addItem(dto: CartAddMerchandiseItemDto): Observable<CartDto> {
    return this.http.post<CartDto>(`${this.baseUri}/items`, dto);
  }

  /**
   * Updates an existing cart item (e.g. quantity change).
   *
   * @param cartItemId Identifier of the cart item to update
   * @param dto Data transfer object containing the updated cart item data
   * @returns Observable emitting the updated cart
   */
  updateItem(cartItemId: number, dto: CartUpdateItemDto): Observable<CartDto> {
    return this.http.patch<CartDto>(`${this.baseUri}/items/${cartItemId}`, dto);
  }

  /**
   * Removes a merchandise item from the cart.
   *
   * @param cartItemId Identifier of the cart item to remove
   * @returns Observable completing when the item has been removed
   */
  removeItem(cartItemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUri}/items/${cartItemId}`);
  }

  /**
   * Performs the checkout operation for the current cart.
   *
   * This finalizes the purchase, creates invoices, deducts stock,
   * and clears the cart on success.
   *
   * @param dto Checkout request containing payment and checkout options
   * @returns Observable emitting the checkout result
   */
  checkout(dto: CartCheckoutRequestDto): Observable<CartCheckoutResultDto> {
    return this.http.post<CartCheckoutResultDto>(`${this.baseUri}/checkout`, dto);
  }

  /**
   * Adds one or more tickets to the cart.
   *
   * Tickets are identified by their unique ticket IDs and are typically
   * either free or reserved tickets.
   *
   * @param ticketIds List of ticket IDs to add to the cart
   * @returns Observable emitting the updated cart
   */
  addTickets(ticketIds: number[]): Observable<CartDto> {
    return this.http.post<CartDto>(`${this.baseUri}/tickets`, ticketIds);
  }

  /**
   * Removes a ticket from the cart.
   *
   * @param ticketId Identifier of the ticket to remove
   * @returns Observable completing when the ticket has been removed
   */
  removeTicket(ticketId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUri}/tickets/${ticketId}`);
  }
}
