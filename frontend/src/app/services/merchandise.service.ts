import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Merchandise } from '../dtos/merchandiseDtos/merchandise';
import { MerchandisePurchaseRequestDto } from "../dtos/merchandiseDtos/merchandise-purchase";
import { MerchandiseCreateDto } from '../dtos/merchandiseDtos/merchandise-create';
import { RewardRedeemRequestDto } from "../dtos/merchandiseDtos/reward-redeem";

/**
 * Service responsible for interacting with the merchandise and reward
 * functionality of the backend.
 */
@Injectable({
  providedIn: 'root'
})
export class MerchandiseService {

  /**
   * Base URL for all merchandise-related backend endpoints.
   */
  private readonly baseUrl = `http://localhost:8080/api/v1/merchandise`;

  /**
   * Creates a new MerchandiseService.
   *
   * @param http Angular HttpClient used for REST communication
   */
  constructor(private http: HttpClient) {
  }

  /**
   * Retrieves all available merchandise items.
   *
   * This includes both regular merchandise and reward items,
   * depending on backend filtering rules.
   *
   * @returns Observable emitting a list of merchandise items
   */
  getAllMerchandise(): Observable<Merchandise[]> {
    return this.http.get<Merchandise[]>(this.baseUrl);
  }


  /**
   * Purchases one or more merchandise items.
   *
   * The backend handles stock validation, reward point calculation,
   * and invoice creation.
   *
   * @param request Purchase request containing merchandise IDs and quantities
   * @returns Observable completing when the purchase is successful
   */
  purchase(request: MerchandisePurchaseRequestDto) {
    return this.http.post(`${this.baseUrl}/purchase`, request);
  }

  /**
   * Creates a new merchandise item.
   *
   * This operation is restricted to administrators.
   *
   * @param dto Data transfer object containing the merchandise details
   * @returns Observable emitting the created merchandise item
   */
  createMerchandise(dto: MerchandiseCreateDto) {
    return this.http.post<Merchandise>(this.baseUrl, dto);
  }


  /**
   * Retrieves all merchandise items that are redeemable as rewards.
   *
   * Reward items can be exchanged for user reward points instead
   * of being purchased with money.
   *
   * @returns Observable emitting a list of reward merchandise items
   */
  getRewardMerchandise() {
    return this.http.get<Merchandise[]>(`${this.baseUrl}/rewards`);
  }

  /**
   * Deletes a merchandise item.
   *
   * This operation is restricted to administrators and typically
   * marks the item as deleted rather than physically removing it.
   *
   * @param id Identifier of the merchandise item to delete
   * @returns Observable completing when the item has been deleted
   */
  deleteMerchandise(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Retrieves a single merchandise item by its identifier.
   *
   * @param id Identifier of the merchandise item
   * @returns Observable emitting the merchandise item
   */
  getMerchandiseById(id: number) {
    return this.http.get<Merchandise>(`${this.baseUrl}/${id}`);
  }

  /**
   * Redeems a reward merchandise item using reward points.
   *
   * The backend validates the user's available points and updates
   * the reward point balance accordingly.
   *
   * @param request Reward redemption request
   * @returns Observable completing when the redemption is successful
   */
  redeemRewards(request: RewardRedeemRequestDto) {
    return this.http.post(`${this.baseUrl}/rewards/redeem`, request);
  }

  /**
   * Uploads an image for a merchandise item.
   *
   * The image is sent as multipart/form-data and stored by the backend.
   *
   * @param id Identifier of the merchandise item
   * @param file Image file to upload
   * @returns Observable completing when the image upload is successful
   */
  uploadImage(id: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.post<void>(`${this.baseUrl}/${id}/image`, formData);
  }

  /**
   * Returns the URL used to retrieve the image of a merchandise item.
   *
   * This URL can be used directly as the `src` attribute of an `<img>` tag.
   *
   * @param id Identifier of the merchandise item
   * @returns Image URL for the merchandise item
   */
  getImageUrl(id: number): string {
    return `${this.baseUrl}/${id}/image`;
  }

  /**
   * Validates an image file before upload.
   *
   * Ensures that the file has an allowed MIME type and does not exceed
   * the maximum file size.
   *
   * @param file Image file to validate
   * @returns Validation error message, or `null` if the file is valid
   */
  validateImageFile(file: File): string | null {
    const allowed = ['image/png', 'image/jpeg', 'image/webp'];
    if (!allowed.includes(file.type)) {
      return 'Ungültiger Dateityp. Erlaubt: PNG, JPG, WEBP.';
    }
    const maxBytes = 3 * 1024 * 1024;
    if (file.size > maxBytes) {
      return 'Bild ist zu groß (max. 3MB).';
    }
    return null;
  }
}
