export type ApiSeatStatus = 'FREE' | 'RESERVED' | 'SOLD';

export interface ApiSeatDto {
  id: number;
  rowNumber: number;
  seatNumber: number;
  status: ApiSeatStatus;
  priceCategory: 'free' | 'cheap' | 'middle' | 'expensive';
}
