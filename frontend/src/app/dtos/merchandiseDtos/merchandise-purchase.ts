import { PaymentMethod } from '../../types/payment-method';
import { PaymentDetailDto } from '../paymentDtos/payment-detail';

export interface MerchandisePurchaseItemDto {
  merchandiseId: number;
  quantity: number;
}

export interface MerchandisePurchaseRequestDto {
  items: MerchandisePurchaseItemDto[];
  paymentMethod: PaymentMethod;
  paymentDetail: PaymentDetailDto;
}
