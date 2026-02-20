import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SeatmapService } from '../../services/seatmap.service';
import { ApiSeatmapDto, ApiSeatDto } from '../../dtos/seatmap.dto';
import { TicketCreate } from '../../dtos/ticket';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart.service';

type SeatStatus = 'free' | 'reserved' | 'sold' | 'selected';
type PriceCategory = 'free' | 'cheap' | 'middle' | 'expensive';

type Cell = Seat | null;

export interface Seat {
  id: number;
  row: number;
  number: number;
  status: SeatStatus;
  priceCategory: PriceCategory;
}

@Component({
  selector: 'app-seatmap',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seatmap.component.html',
  styleUrls: ['./seatmap.component.scss'],
})
export class SeatmapComponent implements OnInit {
  colsCount = 0;
  cells: Cell[] = [];
  private seatByKey = new Map<string, Seat>();

  // Bühne im Grid
  stageRowStart?: number;
  stageRowEnd?: number;
  stageColStart?: number;
  stageColEnd?: number;

  eventIdForTickets?: number;

  rows: (Seat | null)[][] = [];
  selectedSeats = new Set<string>();

  isLoading = false;
  error?: string;

  currentEventId = 1;

  stagePosition: 'TOP' | 'BOTTOM' | 'LEFT' | 'RIGHT' | 'CENTER' = 'BOTTOM';
  stageLabel = 'BÜHNE';

  stageHeightPx?: number;
  stageWidthPx?: number;

  runwayWidthPx?: number;
  runwayLengthPx?: number;
  runwayOffsetPx?: number;

  constructor(
    private seatmapService: SeatmapService,
    private ticketService: TicketService,
    private router: Router,
    private authService: AuthService,
    private cartService: CartService
  ) {}

  get hasStageBox(): boolean {
    return (
      this.stageRowStart != null &&
      this.stageRowEnd != null &&
      this.stageColStart != null &&
      this.stageColEnd != null
    );
  }

  get useGrid(): boolean {
    return this.hasStageBox && this.stagePosition === 'CENTER';
  }

  ngOnInit(): void {
    const state = history.state;
    let eventId = state?.eventId;

    if (!eventId) {
      const hash = window.location.hash; // "#/seatmap?eventId=123"
      const query = hash.includes('?') ? hash.split('?')[1] : '';
      const params = new URLSearchParams(query);
      const q = params.get('eventId');
      if (q) eventId = Number(q);
    }

    eventId = eventId ?? 1;
    this.eventIdForTickets = eventId;
    this.loadSeatmap(eventId);
  }

  private loadSeatmap(eventId: number): void {
    console.log('[Seatmap] loadSeatmap called with eventId =', eventId);
    this.currentEventId = eventId;

    // reset stage meta
    this.stageRowStart = undefined;
    this.stageRowEnd = undefined;
    this.stageColStart = undefined;
    this.stageColEnd = undefined;

    // reset pixel/meta
    this.stageHeightPx = undefined;
    this.stageWidthPx = undefined;
    this.runwayWidthPx = undefined;
    this.runwayLengthPx = undefined;
    this.runwayOffsetPx = undefined;

    // reset seats
    this.rows = [];
    this.colsCount = 0;
    this.cells = [];
    this.seatByKey.clear();
    this.selectedSeats.clear();

    this.isLoading = true;

    this.seatmapService.getSeatmap(eventId).subscribe({
      next: (apiSeatmap: ApiSeatmapDto) => {
        this.stagePosition = apiSeatmap.stagePosition ?? 'BOTTOM';
        this.stageLabel = apiSeatmap.stageLabel ?? 'BÜHNE';

        this.stageRowStart = apiSeatmap.stageRowStart ?? undefined;
        this.stageRowEnd = apiSeatmap.stageRowEnd ?? undefined;
        this.stageColStart = apiSeatmap.stageColStart ?? undefined;
        this.stageColEnd = apiSeatmap.stageColEnd ?? undefined;

        this.stageHeightPx = apiSeatmap.stageHeightPx;
        this.stageWidthPx = apiSeatmap.stageWidthPx;

        this.runwayWidthPx = apiSeatmap.runwayWidthPx;
        this.runwayLengthPx = apiSeatmap.runwayLengthPx;
        this.runwayOffsetPx = apiSeatmap.runwayOffsetPx ?? 0;

        // rows (non-grid layout)
        this.rows = this.buildRowsFromApiSeats(apiSeatmap.seats ?? []);

        // seatByKey (für robustes ticket building auch bei grid)
        for (const s of apiSeatmap.seats ?? []) {
          const seat: Seat = {
            id: s.id,
            row: s.rowNumber,
            number: s.seatNumber,
            status: s.status.toLowerCase() as SeatStatus,
            priceCategory: this.normalizePriceCategory(s.priceCategory as any),
          };
          this.seatByKey.set(`${seat.row}-${seat.number}`, seat);
        }

        // grid cells (CENTER + stageBox)
        if (this.useGrid) {
          const rebuilt = this.buildGridCells(apiSeatmap.seats ?? []);
          this.colsCount = rebuilt.colsCount;
          this.cells = rebuilt.cells;

          // Wenn wir irgendwann colOffset != 0 verwenden: Bühne mitziehen
          if (this.hasStageBox && rebuilt.colOffset !== 0) {
            this.stageColStart = (this.stageColStart as number) + rebuilt.colOffset;
            this.stageColEnd = (this.stageColEnd as number) + rebuilt.colOffset;
          }
        } else {
          this.colsCount = 0;
          this.cells = [];
        }

        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Saalplan konnte nicht geladen werden.';
        this.isLoading = false;
      },
    });
  }

  /**
   * FIX aus "alt":
   * - colsCount fix (deterministisch), nicht maxCol
   * - bei CENTER colOffset = 0 (damit Bühne/Seats nicht wandern)
   */
  private buildGridCells(apiSeats: ApiSeatDto[]): { colsCount: number; cells: Cell[]; colOffset: number } {
    if (!apiSeats || apiSeats.length === 0) return { colsCount: 0, cells: [], colOffset: 0 };

    const maxRow = Math.max(...apiSeats.map((s) => s.rowNumber));

    // WICHTIG: fixe Breite, damit Grid stabil ist (CSS/E2E)
    const colsCount = 23;

    // CENTER-Layouts nicht verschieben
    const colOffset = 0;
    // Wenn du später zentrieren willst, könntest du hier verwenden:
    // const colOffset = this.computeColOffset(apiSeats, colsCount);

    const seatMap = new Map<string, Seat>();
    for (const s of apiSeats) {
      const col = s.seatNumber + colOffset;
      if (col < 1 || col > colsCount) continue;

      seatMap.set(`${s.rowNumber}-${col}`, {
        id: s.id,
        row: s.rowNumber,
        number: col,
        status: s.status.toLowerCase() as SeatStatus,
        priceCategory: this.normalizePriceCategory(s.priceCategory as any),
      });
    }

    const cells: Cell[] = [];
    for (let r = 1; r <= maxRow; r++) {
      for (let c = 1; c <= colsCount; c++) {
        cells.push(seatMap.get(`${r}-${c}`) ?? null);
      }
    }

    return { colsCount, cells, colOffset };
  }

  // optional (aus alt) – aktuell nicht aktiv, aber sauber drin gelassen
  private computeColOffset(apiSeats: ApiSeatDto[], colsCount: number): number {
    const seatCols = apiSeats.map((s) => s.seatNumber);

    let minCol = seatCols.length ? Math.min(...seatCols) : 1;
    let maxCol = seatCols.length ? Math.max(...seatCols) : colsCount;

    if (this.hasStageBox) {
      minCol = Math.min(minCol, this.stageColStart as number);
      maxCol = Math.max(maxCol, this.stageColEnd as number);
    }

    const usedWidth = maxCol - minCol + 1;
    const desiredMinCol = Math.floor((colsCount - usedWidth) / 2) + 1;

    let off = desiredMinCol - minCol;

    const minAllowed = 1 - minCol;
    const maxAllowed = colsCount - maxCol;
    off = Math.max(minAllowed, Math.min(maxAllowed, off));

    return off;
  }

  private normalizePriceCategory(pc: string | null | undefined): PriceCategory {
    const v = (pc ?? '').trim().toLowerCase();

    if (v === 'free' || v === 'cheap' || v === 'middle' || v === 'expensive') return v;
    if (v === 'standard') return 'middle';
    if (v === 'premium') return 'expensive';

    if (v.includes('steh')) return 'free';
    if (v.includes('günst') || v.includes('guenst')) return 'cheap';
    if (v.includes('standard')) return 'middle';
    if (v.includes('premium')) return 'expensive';

    return 'middle';
  }

  private buildRowsFromApiSeats(apiSeats: ApiSeatDto[]): (Seat | null)[][] {
    if (!apiSeats || apiSeats.length === 0) return [];

    const byRow = new Map<number, Map<number, Seat>>();

    for (const s of apiSeats) {
      const seat: Seat = {
        id: s.id,
        row: s.rowNumber,
        number: s.seatNumber,
        status: s.status.toLowerCase() as SeatStatus,
        priceCategory: this.normalizePriceCategory(s.priceCategory as any),
      };

      if (!byRow.has(seat.row)) byRow.set(seat.row, new Map());
      byRow.get(seat.row)!.set(seat.number, seat);
    }

    const rows: (Seat | null)[][] = [];
    const sortedRowNumbers = Array.from(byRow.keys()).sort((a, b) => a - b);

    for (const r of sortedRowNumbers) {
      const seatMap = byRow.get(r)!;
      const maxInRow = Math.max(...seatMap.keys());

      const rowArr: (Seat | null)[] = [];
      for (let n = 1; n <= maxInRow; n++) rowArr.push(seatMap.get(n) ?? null);

      rows.push(rowArr);
    }

    return rows;
  }

  seatId(seat: Seat): string {
    return `${seat.row}-${seat.number}`;
  }

  toggleSeat(seat: Seat | null): void {
    if (!seat) return;
    if (seat.status === 'sold' || seat.status === 'reserved') return;

    const id = this.seatId(seat);

    if (this.selectedSeats.has(id)) {
      this.selectedSeats.delete(id);
      seat.status = 'free';
    } else {
      this.selectedSeats.add(id);
      seat.status = 'selected';
    }
  }

  stageOverlayStyle(): { [k: string]: string } {
    if (!this.hasStageBox) return {};

    const cell = 40;
    const gap = 4;

    const rs = this.stageRowStart!;
    const re = this.stageRowEnd!;
    const cs = this.stageColStart!;
    const ce = this.stageColEnd!;

    const rows = re - rs + 1;
    const cols = ce - cs + 1;

    const top = (rs - 1) * (cell + gap);
    const left = (cs - 1) * (cell + gap);

    const width = cols * cell + (cols - 1) * gap;
    const height = rows * cell + (rows - 1) * gap;

    return {
      top: `${top}px`,
      left: `${left}px`,
      width: `${width}px`,
      height: `${height}px`,
    };
  }

  private buildTicketCreates(): TicketCreate[] {
    const ticketsToCreate: TicketCreate[] = [];

    for (const seatKey of this.selectedSeats) {
      const [row, number] = seatKey.split('-').map(Number);
      const seat = this.seatByKey.get(`${row}-${number}`); // robust (grid + non-grid)
      if (!seat) continue;

      ticketsToCreate.push({
        eventId: this.eventIdForTickets!,
        seatId: seat.id,
      });
    }

    return ticketsToCreate;
  }

  onReserve(): void {
    const ticketsToCreate = this.buildTicketCreates();
    if (!ticketsToCreate.length) return;

    this.ticketService.createTicket(ticketsToCreate).subscribe({
      next: (createdTickets) => {
        const ticketIds = createdTickets.map((t) => t.id);

        this.ticketService.reserveTickets(ticketIds).subscribe({
          next: () => this.router.navigate(['/tickets/my'], { state: { tickets: createdTickets } }),
          error: (err) => console.error('Fehler beim Reservieren', err),
        });
      },
      error: (err) => console.error('Fehler beim Erstellen der Tickets', err),
    });
  }

  onAddToCart(): void {
    const ticketsToCreate = this.buildTicketCreates();
    if (!ticketsToCreate.length) return;

    this.ticketService.createTicket(ticketsToCreate).subscribe({
      next: (createdTickets) => {
        const ticketIds = createdTickets.map((t) => t.id);

        this.ticketService.reserveTickets(ticketIds).subscribe({
          next: () => {
            this.cartService.addTickets(ticketIds).subscribe({
              next: () => this.router.navigate(['/cart']),
              error: (err) => console.error('Fehler beim Warenkorb', err),
            });
          },
          error: (err) => console.error('Fehler beim Reservieren', err),
        });
      },
      error: (err) => console.error('Fehler beim Erstellen der Tickets', err),
    });
  }

  // optional legacy (falls noch tests drauf zeigen)
  onContinue(): void {
    const ticketsToCreate = this.buildTicketCreates();
    if (!ticketsToCreate.length) return;

    this.ticketService.createTicket(ticketsToCreate).subscribe({
      next: (createdTickets) => {
        this.router.navigate(['/tickets/cart'], { state: { tickets: createdTickets } });
      },
      error: (err) => console.error('Fehler beim Erstellen der Tickets', err),
    });
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  seatTooltip(seat: Seat): string {
    const cat =
      seat.priceCategory === 'free'
        ? 'Stehplatz'
        : seat.priceCategory === 'cheap'
          ? 'Günstig'
          : seat.priceCategory === 'middle'
            ? 'Standard'
            : 'Premium';

    const status =
      seat.status === 'free'
        ? 'frei'
        : seat.status === 'selected'
          ? 'ausgewählt'
          : seat.status === 'reserved'
            ? 'reserviert'
            : 'verkauft';

    return `Reihe ${seat.row}, Platz ${seat.number} (${cat}, ${status})`;
  }
}
