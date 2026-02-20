export interface Merchandise {
  id: number;
  description: string;
  name: string;
  unitPrice: number;
  rewardPointsPerUnit: number;
  remainingQuantity: number;
  redeemableWithPoints: boolean;
  hasImage: boolean;
  pointsPrice: number | null;
}
