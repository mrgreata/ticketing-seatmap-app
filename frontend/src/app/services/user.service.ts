import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../global/globals';
import { Observable } from 'rxjs';
import {
  DetailedUserDto,
  RewardPointsDto,
  SimpleUserDto,
  TotalCentsSpentDto,
  UserUpdateDto
} from '../dtos/user/user.dto';
import {UserRegisterDto, UserRegisterResponseDto} from '../dtos/user/user-registration.dto'


@Injectable({
  providedIn: 'root',
})
export class UserService {

  private userBaseUri = this.globals.backendUri + '/users';

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) {}

  /**
   * Sends a registration request to the backend.
   */
  registerUser(dto: UserRegisterDto): Observable<UserRegisterResponseDto> {
    return this.httpClient.post<UserRegisterResponseDto>(
      `${this.userBaseUri}/registration`,
      dto
    );
  }

  /**
   * Backend: GET /api/v1/users/me
   */
  getMe(): Observable<SimpleUserDto> {
    return this.httpClient.get<SimpleUserDto>(`${this.userBaseUri}/me`);
  }

  /**
   * Backend: GET /api/v1/users/me/detailed
   */
  getMeDetailed(): Observable<DetailedUserDto> {
    return this.httpClient.get<DetailedUserDto>(`${this.userBaseUri}/me/detailed`)
  }

  /**
   * Backend: PUT /api/v1/users/me/password
   */
  updateProfile(dto: UserUpdateDto): Observable<DetailedUserDto> {
    return this.httpClient.put<DetailedUserDto>(`${this.userBaseUri}/me`, dto);
  }

  /**
   * Backend: DELETE /api/v1/users/me
   */
  deleteAccount(): Observable<void> {
    return this.httpClient.delete<void>(`${this.userBaseUri}/me`);
  }

  /**
  * Backend: GET /api/v1/users/me/reward-points
  */
  getMyRewardPoints(): Observable<RewardPointsDto> {
    return this.httpClient.get<RewardPointsDto>(`${this.userBaseUri}/me/reward-points`);
  }

  getMyTotalCentsSpent(): Observable<TotalCentsSpentDto> {
    return this.httpClient.get<TotalCentsSpentDto>(`${this.userBaseUri}/me/total-cents-spent`)
  }
}
