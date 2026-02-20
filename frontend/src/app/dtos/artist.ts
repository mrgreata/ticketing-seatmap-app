export interface Artist {
  id: number;
  name: string;
  isBand?: boolean;
  memberIds?: number[];
  members?: Artist[];
  bandsWhereMember?: Artist[];
}
