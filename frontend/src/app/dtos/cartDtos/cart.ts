import {PaymentDetailDto} from "../paymentDtos/payment-detail";
import {PaymentMethod} from "../../types/payment-method";

export type CartItemType = 'MERCHANDISE' | 'REWARD' | 'TICKET';

export interface CartItemDto {
  id: number;
  type: CartItemType;

  merchandiseId: number | null;
  name: string | null;
  unitPrice: number | null;
  quantity: number | null;
  remainingQuantity: number | null;
  hasImage: boolean | null;

  ticketId: number | null;
  ticketCount: number | null;
  eventId: number | null;
  eventTitle: string | null;
  rowNumber?: number | null;
  seatNumber?: number | null;
}

export interface CartDto {
  id: number;
  items: CartItemDto[];
  total: number;
}

export interface CartAddMerchandiseItemDto {
  merchandiseId: number;
  quantity: number;
  redeemedWithPoints: boolean;
}

export interface CartUpdateItemDto {
  quantity: number;
}

export interface CartCheckoutRequestDto {
  paymentMethod: PaymentMethod;
  paymentDetail: PaymentDetailDto;
}

export interface CartCheckoutResultDto {
  merchandiseInvoiceId: number | null;
  rewardInvoiceId: number | null;
  ticketInvoiceId: number | null;
}

