export interface MerchandiseCreateDto {
  description: string;
  name: string;
  unitPrice: number;
  rewardPointsPerUnit: number;
  remainingQuantity: number;
  redeemableWithPoints: boolean;
  pointsPrice: number | null;
}
