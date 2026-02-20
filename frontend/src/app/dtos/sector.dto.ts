import { ApiSeatDto } from './seat.dto';

export interface SectorDto {
  id: number;
  name: string;
  seats: ApiSeatDto[];
}
