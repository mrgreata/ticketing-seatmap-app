export interface UserRegisterDto {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface UserRegisterResponseDto {
  user: {
    id: number;
    email: string;
    userRole: string;
  };
  token: string;
}
