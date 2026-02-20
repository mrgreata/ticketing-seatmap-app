export type ApiSeatStatus = 'FREE' | 'RESERVED' | 'SOLD';

export interface ApiSeatDto {
  id: number;
  rowNumber: number;
  seatNumber: number;
  status: ApiSeatStatus;
  priceCategory: string; // wichtig: Backend kann Labels liefern
  sectorId?: number;
}

export type ApiStagePosition = 'TOP' | 'BOTTOM' | 'LEFT' | 'RIGHT' | 'CENTER';

export interface ApiSeatmapDto {
  stageWidthPx: number;
  stageHeightPx: number;
  eventId: number;
  seats: ApiSeatDto[];
  stagePosition: ApiStagePosition;
  stageLabel: string;

  stageRowStart?: number;
  stageRowEnd?: number;
  stageColStart?: number;
  stageColEnd?: number;

  runwayWidthPx?: number;     // schmal (z.B. 140)
  runwayLengthPx?: number;    // lang (z.B. 420)
  runwayOffsetPx?: number;
}
