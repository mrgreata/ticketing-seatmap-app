export interface SimpleArtist {
  id: number;
  name: string;
}

export interface SimpleLocation {
  id: number;
  zipCode: number;
  city: string;
  street: string;
  streetNumber: string;
}

export interface EventDetail {
  id: number;
  title: string;
  type: string;
  durationMinutes: number;
  description: string;
  dateTime: string;
  location: SimpleLocation;
  artists: SimpleArtist[];
  ticketCount: number;
  minPrice?: number;
}
