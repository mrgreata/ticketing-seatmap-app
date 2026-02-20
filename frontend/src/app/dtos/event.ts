export interface Event {
  id: number;
  title: string;
  type: string;
  durationMinutes: number;
  dateTime: string;
  locationName: string;
  locationCity?: string;
  minPrice?: number;
  description?: string;
}
