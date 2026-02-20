export interface InvoiceCreate {
  invoiceNumber: string;
  invoiceDate: string;
  userId: number;
  userName: string;
  userAddress: string;
  eventDate: string;
  ticketIds: number[];
}

export interface SimpleInvoice {
  id: number;
  invoiceNumber: string;
  userId: number;
}

export interface InvoiceTicketSummary {
  eventName: string;
  eventDate: string;
}

export interface DetailedInvoice {
  id: number;
  invoiceNumber: string;
  userId: number;
  invoiceCancellationDate: string;
  tickets: InvoiceTicketSummary[];
}


