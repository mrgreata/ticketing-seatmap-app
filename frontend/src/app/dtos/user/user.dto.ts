export interface SimpleUserDto {
  id: number;
  email: string;
  userRole: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface DetailedUserDto extends SimpleUserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  address?: string;
  locked: boolean;
  userRole: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface UserCreateDto {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  userRole: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface UserUpdateDto {
  email: string;
  firstName: string;
  lastName: string;
  address: string;
}

export interface RewardPointsDto {
  rewardPoints: number;
}

export interface TotalCentsSpentDto {
  totalCentsSpent: number;
}

export interface UserLockUpdateDto {
  locked: boolean;
  adminLocked: boolean;
}

