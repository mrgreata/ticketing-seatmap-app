import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../dtos/page.dto';
import {
  DetailedUserDto,
  UserCreateDto,
  UserLockUpdateDto
} from '../dtos/user/user.dto';


@Injectable({
  providedIn: 'root'
})
export class AdminUserService {

  private readonly baseUrl = '/api/v1/admin/users';

  constructor(private http: HttpClient) {}

  /**
   * Fetches users with pagination, lock filter and optional search.
   *
   * @param params.locked whether to fetch locked or active users
   * @param params.page page index (0-based)
   * @param params.size page size
   * @param params.search optional search term (name or email)
   */
  getUsers(params: {
    locked: boolean;
    page: number;
    size: number;
    search?: string | null;
  }): Observable<Page<DetailedUserDto>> {

    let httpParams = new HttpParams()
    .set('locked', params.locked)
    .set('page', params.page)
    .set('size', params.size);

    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }

    return this.http.get<Page<DetailedUserDto>>(
      this.baseUrl,
      { params: httpParams }
    );
  }

  /**
   * Updates the lock state of a user account.
   *
   * @param id user ID
   * @param locked true to lock, false to unlock
   */
  updateLockState(
    id: number,
    locked: boolean,
    adminLocked: boolean
  ): Observable<void> {
    console.log("AdminUserService.updateLockState called with id: " + id + " and adminLocked: " + adminLocked);
    const body: UserLockUpdateDto = { locked,adminLocked };
    return this.http.patch<void>(
      `${this.baseUrl}/${id}/lock-state`,
      body
    );
  }

  /**
   * Triggers a password reset for the given user.
   * A reset link will be sent via email.
   */
  triggerPasswordReset(id: number): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${id}/password-reset`,
      {}
    );
  }

  /**
   * Creates a new user account (admin operation).
   */
  createUser(dto: UserCreateDto): Observable<void> {
    return this.http.post<void>(this.baseUrl, dto);
  }

  /**
   * Updates the role of a user account.
   *
   * @param id user ID
   * @param role new role to assign (ROLE_USER or ROLE_ADMIN)
   */
  updateUserRole(
    id: number,
    role: 'ROLE_USER' | 'ROLE_ADMIN'
  ): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/${id}/role`,
      { role }
    );
  }
}
