export interface DetailedNewsItem {
  id: number;
  title: string;
  publishedAt: string;
  summary: string;
  text: string;
  imageData: number[] | null;
  imageUrl?: string | null;
}
