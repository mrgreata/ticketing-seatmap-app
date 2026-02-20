export interface Ticket {
  id: number;
  eventName: string;
  rowNumber: number;
  seatNumber: number;
  eventDate: Date;
  entryTime: string;
  reservationNumber?: string;
  reservationExpires?: Date;
  invoiceNumber?: String,
  selected:boolean
  price: number
}

export interface TicketCart {
  eventId: number;
  eventName: string;
  rowNumber: number;
  seatNumber: number;
  seatId: number;
  eventDate: Date;
  eventStarts: string;
  price: number;
  locationId: number
  selected: boolean
}

export interface TicketCreate{
  eventId: number;
  seatId: number;
}


//basically a reservation Ticket Mix for presentation reasons
export interface TicketReserved{
  ticketId: number;
  reservationId: number;
  eventName: string;
  rowNumber: number;
  seatNumber: number,
  seatId: number;
  eventDate: Date;
  entryTime: string;
  reservationNumber?: string;
  price: string;
  selected:boolean;
  reservationExpires: Date;
}

export interface TicketPurchased{
  id: number;
  eventName: string;
  rowNumber: number;
  seatNumber: number,
  seatId: number;
  eventDate: Date;
  entryTime: string;
  invoiceId: number;
  price: number;
  locationCity: string;
  selected:boolean;
}


export interface SimpleReservation {
  userId: number;
  ticketIds: number []
}

export interface TicketCancelled {
  id: number;
  eventName: string;
  eventDate: Date;
  cancellationDate: Date;
  seats: string[];
  creditInvoiceId: number;
  selected: boolean;
}
