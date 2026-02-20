export interface CreateNewsItem {
  title: string;
  publishedAt?: string;
  summary: string;
  text: string;
  imageData?: number[] | null;
}
