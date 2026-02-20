package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Cart;
import at.ac.tuwien.sepr.groupphase.backend.entity.CartItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceMerchandiseItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.CartRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceMerchandiseItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PriceCategoryRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.CartItemType;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

@Component
@Profile("!test && !e2e") //
public class DataGenerator implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass());

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ArtistRepository artistRepository;
    private final EventRepository eventRepository;
    private final SectorRepository sectorRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReservationRepository reservationRepository;
    private final NewsItemRepository newsItemRepository;
    private final InvoiceMerchandiseItemRepository invoiceMerchandiseItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Value("${app.datagen.enabled:false}")
    private boolean enabled;

    public DataGenerator(
        UserRepository userRepository,
        LocationRepository locationRepository,
        ArtistRepository artistRepository,
        EventRepository eventRepository,
        SectorRepository sectorRepository,
        PriceCategoryRepository priceCategoryRepository,
        SeatRepository seatRepository,
        TicketRepository ticketRepository,
        MerchandiseRepository merchandiseRepository,
        InvoiceRepository invoiceRepository,
        ReservationRepository reservationRepository,
        NewsItemRepository newsItemRepository,
        InvoiceMerchandiseItemRepository invoiceMerchandiseItemRepository,
        PasswordEncoder passwordEncoder,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository
    ) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.artistRepository = artistRepository;
        this.eventRepository = eventRepository;
        this.sectorRepository = sectorRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.merchandiseRepository = merchandiseRepository;
        this.invoiceRepository = invoiceRepository;
        this.reservationRepository = reservationRepository;
        this.newsItemRepository = newsItemRepository;
        this.invoiceMerchandiseItemRepository = invoiceMerchandiseItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOGGER.info("Data generation is disabled (app.datagen.enabled=false)");
            return;
        }

        LOGGER.info("Starting comprehensive data generation...");

        invoiceMerchandiseItemRepository.deleteAll();
        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        invoiceRepository.deleteAll();
        merchandiseRepository.deleteAll();
        seatRepository.deleteAll();
        priceCategoryRepository.deleteAll();
        sectorRepository.deleteAll();
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        locationRepository.deleteAll();
        newsItemRepository.deleteAll();
        userRepository.deleteAll();
        if (cartItemRepository.existsById(1L)) {
            cartItemRepository.deleteAll();
        }
        cartRepository.deleteAll();

        List<User> users = generateUsers();
        generateCarts(users);
        List<Location> locations = generateLocations();
        List<Artist> artists = generateArtists();
        List<Event> events = generateEvents(locations, artists);
        uploadEventImages(events);
        generateSeatsAndTickets(locations, events);
        List<Merchandise> merchandise = generateMerchandise();
        uploadMerchandiseImages(merchandise);
        generateInvoicesAndReservations(users, events, merchandise);
        List<NewsItem> news = generateNews();
        uploadNewsImages(news);


        LOGGER.info("Data generation completed successfully!");
    }

    // ==================== USERS ====================

    private List<User> generateUsers() {
        LOGGER.info("Generating users...");

        // --------------------
        // Admin (NOT locked)
        // --------------------
        User admin = new User(
            "admin@email.com",
            passwordEncoder.encode("password"),
            UserRole.ROLE_ADMIN,
            "Admin",
            "User",
            "Admin Street 1, 1010 Vienna"
        );
        admin.setRewardPoints(0);
        admin.setTotalCentsSpent(0);
        admin.setLocked(false);
        admin.setLoginFailCount(0);

        List<User> users = new ArrayList<>();
        users.add(userRepository.save(admin));

        // --------------------
        // Regular users
        // --------------------
        String[] firstNames = {
            "Max", "Anna", "Peter", "Lisa", "Michael", "Sarah",
            "Thomas", "Julia", "David", "Emma", "Lukas", "Sophie",
            "Felix", "Marie"
        };

        String[] lastNames = {
            "Müller", "Schmidt", "Schneider", "Fischer", "Weber",
            "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann",
            "Koch", "Bauer", "Richter", "Klein"
        };

        for (int i = 0; i < firstNames.length; i++) {
            User user = new User(
                "user" + (i + 1) + "@email.com",
                passwordEncoder.encode("password"),
                UserRole.ROLE_USER,
                firstNames[i],
                lastNames[i],
                "Street " + (i + 1) + ", " + (1010 + ((i * 10) % 23)) + " Vienna"
            );

            user.setRewardPoints(100 + (i * 50));
            user.setTotalCentsSpent(1000 + (i * 500));

            user.setLocked(i < 2);
            user.setLoginFailCount(i < 2 ? 5 : 0);

            users.add(userRepository.save(user));
        }

        LOGGER.info(
            "Generated {} users ({} locked)",
            users.size(),
            users.stream().filter(User::isLocked).count()
        );

        return users;
    }

    // ==================== LOCATIONS ====================

    private List<Location> generateLocations() {
        LOGGER.info("Generating specialized locations for event types...");
        List<Location> locations = new ArrayList<>();

        locations.add(createLocation(1010, "Wien", "Gartenbaugasse", "19",
            "Gartenbau Kino", "BOTTOM", "LEINWAND"));
        locations.add(createLocation(1070, "Wien", "Mariahilfer Straße", "57",
            "Village Cinema", "BOTTOM", "SCREEN"));
        locations.add(createLocation(1020, "Wien", "Praterstraße", "1",
            "Cineplexx Millennium City", "BOTTOM", "LEINWAND"));
        locations.add(createLocation(5020, "Salzburg", "Rainerstraße", "1-3",
            "Das Kino Salzburg", "BOTTOM", "SCREEN"));

        locations.add(createLocation(1020, "Wien", "Donauinsel", "1",
            "Donauinsel Open Air", "CENTER", "BÜHNE"));
        locations.add(createLocation(1150, "Wien", "Hütteldorfer Straße", "1",
            "Ernst Happel Stadion", "CENTER", "STAGE"));
        locations.add(createLocation(6020, "Innsbruck", "Olympiastraße", "10",
            "Olympiahalle Innsbruck", "CENTER", "BÜHNE"));
        locations.add(createLocation(8010, "Graz", "Messeplatz", "1",
            "Stadthalle Graz", "CENTER", "STAGE"));

        locations.add(createLocation(1060, "Wien", "Linke Wienzeile", "6",
            "Theater an der Wien", "BOTTOM", "BÜHNE"));
        locations.add(createLocation(5020, "Salzburg", "Schwarzstraße", "22",
            "Landestheater Salzburg", "BOTTOM", "BÜHNE"));
        locations.add(createLocation(6020, "Innsbruck", "Rennweg", "2",
            "Tiroler Landestheater", "BOTTOM", "STAGE"));
        locations.add(createLocation(4020, "Linz", "Promenade", "39",
            "Landestheater Linz", "BOTTOM", "BÜHNE"));

        locations.add(createLocation(1010, "Wien", "Opernring", "2",
            "Wiener Staatsoper", "BOTTOM", "BÜHNE"));
        locations.add(createLocation(5020, "Salzburg", "Herbert-von-Karajan-Platz", "11",
            "Salzburger Festspielhaus", "BOTTOM", "BÜHNE"));
        locations.add(createLocation(8010, "Graz", "Kaiser-Josef-Platz", "10",
            "Oper Graz", "BOTTOM", "STAGE"));

        locations.add(createLocation(1220, "Wien", "Donauinsel", "1",
            "Donauinselfest Arena", "CENTER", "HAUPTBÜHNE"));
        locations.add(createLocation(9020, "Klagenfurt", "Messeplatz", "1",
            "Wörthersee Festivalgelände", "CENTER", "MAIN STAGE"));

        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            int variant = getVariantForIndex(i);
            String venueType = getVenueTypeForVariant(variant);

            configureStage(loc, variant);
            Location saved = locationRepository.save(loc);
            locations.set(i, saved);
            generateSectorsForLocation(saved, variant, venueType);
        }

        LOGGER.info("Generated {} locations: 4 cinemas, 4 concerts, 4 theaters, 3 operas, 2 festivals",
            locations.size());
        return locations;
    }

    private IntFunction<boolean[]> blockFixedCols(int totalCols, int... cols) {
        return (row) -> {
            boolean[] blocked = new boolean[totalCols + 1];
            for (int c : cols) {
                if (c >= 1 && c <= totalCols) {
                    blocked[c] = true;
                }
            }
            return blocked;
        };
    }


    private void generateSectorsForLocation(Location location, int variant, String venueType) {
        Sector standing = sectorRepository.save(new Sector("Stehplätze", location));
        Sector cheap    = sectorRepository.save(new Sector("Kategorie C", location));
        Sector standard = sectorRepository.save(new Sector("Kategorie B", location));
        Sector premium  = sectorRepository.save(new Sector("Kategorie A", location));

        int baseStanding;
        int baseCheap;
        int baseStandard;
        int basePremium;
        int aisleEvery;

        switch (venueType) {
            case "Kino" -> {
                baseStanding = 800;
                baseCheap = 1200;
                baseStandard = 1500;
                basePremium = 2000;
                aisleEvery = 10;
            }
            case "Musical" -> {
                baseStanding = 2000;
                baseCheap = 3000;
                baseStandard = 4500;
                basePremium = 6500;
                aisleEvery = 6;
            }
            case "Concert" -> {
                baseStanding = 2500;
                baseCheap = 4000;
                baseStandard = 6000;
                basePremium = 9000;
                aisleEvery = 6;
            }
            case "Opera" -> {
                baseStanding = 3500;
                baseCheap = 5500;
                baseStandard = 8000;
                basePremium = 12000;
                aisleEvery = 4;
            }
            case "Festival" -> {
                baseStanding = 4500;
                baseCheap = 7000;
                baseStandard = 10000;
                basePremium = 15000;
                aisleEvery = 5;
            }
            default -> {
                baseStanding = 2000;
                baseCheap = 3000;
                baseStandard = 5000;
                basePremium = 7000;
                aisleEvery = 6;
            }
        }

        final PriceCategory pcFree = priceCategoryRepository.save(new PriceCategory("Stehplatz", baseStanding, standing));
        final PriceCategory pcCheap = priceCategoryRepository.save(new PriceCategory("Günstig", baseCheap, cheap));
        final PriceCategory pcStandard = priceCategoryRepository.save(new PriceCategory("Standard", baseStandard, standard));
        final PriceCategory pcPremium = priceCategoryRepository.save(new PriceCategory("Premium", basePremium, premium));

        if (variant == 0) {
            // =========================
            // Seatmap 1: Pattern-basiert (bestehender Saalplan 1)
            // =========================
            final IntFunction<int[]> standingPattern = (row) -> switch (row) {
                case 1 -> new int[]{2, 5, 5, 2};
                case 2 -> new int[]{3, 5, 5, 3};
                case 3 -> new int[]{4, 5, 5, 4};
                default -> new int[]{4, 5, 5, 4};
            };

            final IntFunction<int[]> cheapPattern = (row) -> new int[]{5, 5, 5, 5};
            final IntFunction<int[]> standardPattern = (row) -> new int[]{5, 5, 5, 5};

            final IntFunction<int[]> premiumPattern = (row) -> switch (row) {
                case 12, 13, 14 -> new int[]{5, 5, 5, 5};
                case 15 -> new int[]{4, 5, 5, 4};
                case 16 -> new int[]{3, 5, 5, 3};
                case 17 -> new int[]{2, 5, 5, 2};
                default -> new int[]{5, 5, 5, 5};
            };

            createSeatsForSectorPattern(standing, pcFree, 1, 3, standingPattern);
            createSeatsForSectorPattern(cheap, pcCheap, 4, 6, cheapPattern);
            createSeatsForSectorPattern(standard, pcStandard, 7, 11, standardPattern);
            createSeatsForSectorPattern(premium, pcPremium, 12, 17, premiumPattern);

        } else if (variant == 1) {

            int totalCols = 23;

            Integer stageRowStart = location.getStageRowStart();
            Integer stageRowEnd = location.getStageRowEnd();
            Integer stageColStart = location.getStageColStart();
            Integer stageColEnd = location.getStageColEnd();

            IntFunction<boolean[]> blockedStage =
                blockStageBox(totalCols, stageRowStart, stageRowEnd, stageColStart, stageColEnd);

            // Catwalk: rows 9..17, cols 11..13
            IntFunction<boolean[]> blockedCatwalk =
                blockRect(totalCols, 9, 17, 11, 13);

            IntFunction<boolean[]> blocked = blockStageBox(totalCols, stageRowStart, stageRowEnd, stageColStart, stageColEnd);

            createSeatsFixedGridWithBlockedCols(standing, pcFree, 1, 3, 1, totalCols, blocked);
            createSeatsFixedGridWithBlockedCols(cheap, pcCheap, 4, 6, 1, totalCols, blocked);
            createSeatsFixedGridWithBlockedCols(standard, pcStandard, 7, 11, 1, totalCols, blocked);
            createSeatsFixedGridWithBlockedCols(premium, pcPremium, 12, 17, 1, totalCols, blocked);
        } else if (variant == 2) {
            // =========================
            // Seatmap 3: Theater / Trapez (nach hinten breiter)
            // =========================

            final IntFunction<int[]> premiumPattern = (row) -> switch (row) {
                case 1 -> new int[]{2, 4, 4, 2};
                case 2 -> new int[]{2, 5, 5, 2};
                case 3 -> new int[]{3, 5, 5, 3};
                default -> new int[]{3, 5, 5, 3};
            };

            final IntFunction<int[]> standardPattern = (row) -> switch (row) {
                case 4 -> new int[]{3, 5, 5, 3};
                case 5 -> new int[]{3, 6, 6, 3};
                case 6 -> new int[]{4, 6, 6, 4};
                default -> new int[]{4, 6, 6, 4};
            };

            final IntFunction<int[]> cheapPattern = (row) -> switch (row) {
                case 7 -> new int[]{4, 6, 6, 4};
                case 8 -> new int[]{4, 7, 7, 4};
                case 9 -> new int[]{5, 7, 7, 5};
                case 10 -> new int[]{5, 8, 8, 5};
                case 11 -> new int[]{6, 8, 8, 6};
                default -> new int[]{6, 8, 8, 6};
            };

            final IntFunction<int[]> standingPattern = (row) -> switch (row) {
                case 12 -> new int[]{6, 8, 8, 6};
                case 13 -> new int[]{6, 9, 9, 6};
                case 14 -> new int[]{6, 9, 9, 6};
                case 15 -> new int[]{5, 9, 9, 5};
                case 16 -> new int[]{4, 8, 8, 4};
                case 17 -> new int[]{3, 7, 7, 3};
                default -> new int[]{6, 9, 9, 6};
            };
            createSeatsForSectorPattern(standing, pcFree, 1, 3, standingPattern);
            createSeatsForSectorPattern(cheap, pcCheap, 4, 6, cheapPattern);
            createSeatsForSectorPattern(standard, pcStandard, 7, 11, standardPattern);
            createSeatsForSectorPattern(premium, pcPremium, 12, 17, premiumPattern);

        } else if (variant == 3) {

            int totalCols = 23;

            Integer stageRowStart = location.getStageRowStart();
            Integer stageRowEnd = location.getStageRowEnd();
            Integer stageColStart = location.getStageColStart();
            Integer stageColEnd = location.getStageColEnd();

            IntFunction<boolean[]> blockedStage =
                blockStageBox(totalCols, stageRowStart, stageRowEnd, stageColStart, stageColEnd);

            // Gänge fix (immer gleich)
            IntFunction<boolean[]> blockedAisles =
                blockFixedCols(totalCols, 4, 8, 12, 16, 20);

            // Kombinieren
            IntFunction<boolean[]> blocked = orBlocked(blockedStage, blockedAisles);

            // Statt Reihen-Bändern: radial um die Bühne
            createSeatsFixedGridRadialPricing(
                totalCols,
                1, 17,
                1, totalCols,
                blocked,
                stageRowStart, stageRowEnd,
                stageColStart, stageColEnd,
                standing, pcFree,
                cheap, pcCheap,
                standard, pcStandard,
                premium, pcPremium
            );
        }


    }

    private IntFunction<boolean[]> orBlocked(IntFunction<boolean[]> a, IntFunction<boolean[]> b) {
        return (row) -> {
            boolean[] blockedA = (a != null) ? a.apply(row) : null;
            boolean[] blockedB = (b != null) ? b.apply(row) : null;

            int len = 0;
            if (blockedA != null) {
                len = Math.max(len, blockedA.length);
            }
            if (blockedB != null) {
                len = Math.max(len, blockedB.length);
            }

            boolean[] out = new boolean[len];

            if (blockedA != null) {
                for (int i = 0; i < blockedA.length; i++) {
                    out[i] |= blockedA[i];
                }
            }
            if (blockedB != null) {
                for (int i = 0; i < blockedB.length; i++) {
                    out[i] |= blockedB[i];
                }
            }

            return out;
        };
    }

    private IntFunction<boolean[]> blockRect(
        int totalCols,
        int rowStart, int rowEnd,
        int colStart, int colEnd
    ) {
        return (row) -> {
            boolean[] blocked = new boolean[totalCols + 1];

            if (row < rowStart || row > rowEnd) {
                return blocked;
            }

            for (int c = colStart; c <= colEnd; c++) {
                if (c >= 1 && c <= totalCols) {
                    blocked[c] = true;
                }
            }

            return blocked;
        };
    }

    private IntFunction<boolean[]> blockStageBox(
        int totalCols,
        Integer stageRowStart, Integer stageRowEnd,
        Integer stageColStart, Integer stageColEnd
    ) {
        return (row) -> {
            boolean[] blocked = new boolean[totalCols + 1];

            if (stageRowStart == null || stageRowEnd == null || stageColStart == null || stageColEnd == null) {
                return blocked;
            }

            if (row >= stageRowStart && row <= stageRowEnd) {
                for (int c = stageColStart; c <= stageColEnd; c++) {
                    if (c >= 1 && c <= totalCols) {
                        blocked[c] = true;
                    }
                }
            }
            return blocked;
        };
    }

    private void createSeatsFixedGridWithBlockedCols(
        Sector sector,
        PriceCategory pc,
        int rowStart,
        int rowEnd,
        int colStart,
        int colEnd,
        IntFunction<boolean[]> blockedColsForRow
    ) {
        for (int row = rowStart; row <= rowEnd; row++) {
            boolean[] blocked = blockedColsForRow.apply(row);

            for (int col = colStart; col <= colEnd; col++) {
                if (blocked != null && col < blocked.length && blocked[col]) {
                    continue;
                }
                saveSeat(row, col, sector, pc);
            }
        }
    }

    private void saveSeat(int row, int seatNumber, Sector sector, PriceCategory pc) {
        Seat seat = new Seat();
        seat.setRowNumber(row);
        seat.setSeatNumber(seatNumber);
        seat.setSector(sector);
        seat.setPriceCategory(pc);
        seatRepository.save(seat);
    }


    private IntFunction<int[]> catwalkGroups(int leftSeats, int rightSeats) {
        return (row) -> new int[]{leftSeats, rightSeats};
    }

    private IntFunction<int[]> catwalkGaps(
        int catwalkWidth,
        int catwalkStartRow,
        int catwalkLengthRows,
        int headExtraWidth
    ) {
        return (row) -> {
            boolean inCatwalk = row >= catwalkStartRow && row < catwalkStartRow + catwalkLengthRows;
            if (!inCatwalk) {
                return new int[]{1};
            }

            boolean atHead = row == catwalkStartRow + catwalkLengthRows - 1;
            int width = catwalkWidth + (atHead ? headExtraWidth : 0);

            return new int[]{width};
        };
    }

    private IntFunction<int[]> seatmap1GapsWithCatwalk(
        int catwalkWidth,
        int catwalkStartRow,
        int catwalkLengthRows,
        int headExtraWidth
    ) {
        return (row) -> {
            int gap12 = 1;
            int gap23 = 1;
            int gap34 = 1;

            boolean inCatwalk = row >= catwalkStartRow && row < catwalkStartRow + catwalkLengthRows;
            if (inCatwalk) {
                boolean atHead = row == catwalkStartRow + catwalkLengthRows - 1;
                gap23 = catwalkWidth + (atHead ? headExtraWidth : 0);

                int extra = gap23 - 1;
                int leftExtra = extra / 2;
                int rightExtra = extra - leftExtra;

                gap12 = 1 + leftExtra;
                gap34 = 1 + rightExtra;
            }

            return new int[]{gap12, gap23, gap34};
        };
    }


    private void createSeatsForSector(Sector sector, PriceCategory priceCategory, int rowStart, int rowEnd, int seatsPerRow, int aisleEvery) {
        for (int row = rowStart; row <= rowEnd; row++) {
            for (int x = 1; x <= seatsPerRow; x++) {
                int seatNumber = x + Math.floorDiv((x - 1), aisleEvery);
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setSeatNumber(seatNumber);
                seat.setSector(sector);
                seat.setPriceCategory(priceCategory);
                seatRepository.save(seat);
            }
        }
    }

    // ==================== ARTISTS ====================

    private List<Artist> generateArtists() {
        LOGGER.info("Generating artists matching event types...");
        List<Artist> artists = new ArrayList<>();

        artists.add(createPerson("Cillian Murphy"));
        artists.add(createPerson("Emily Blunt"));
        artists.add(createPerson("Timothée Chalamet"));
        artists.add(createPerson("Zendaya"));
        artists.add(createPerson("Emma Stone"));
        artists.add(createPerson("Mark Ruffalo"));
        artists.add(createPerson("Owen Wilson"));
        artists.add(createPerson("Margot Robbie"));
        artists.add(createPerson("Ryan Gosling"));
        artists.add(createPerson("Robert Pattinson"));
        artists.add(createPerson("Zoë Kravitz"));
        artists.add(createPerson("Ralph Fiennes"));
        artists.add(createPerson("John David Washington"));
        artists.add(createPerson("Gemma Chan"));
        artists.add(createPerson("Sam Worthington"));
        artists.add(createPerson("Zoe Saldana"));
        artists.add(createPerson("Gerard Butler"));
        artists.add(createPerson("Morena Baccarin"));
        artists.add(createPerson("Chris Pratt"));

        artists.add(createPerson("James Hetfield"));
        artists.add(createPerson("Lars Ulrich"));
        Artist metallica = createBand("Metallica", new ArrayList<>());
        artists.add(metallica);

        artists.add(createPerson("Ed Sheeran"));
        artists.add(createPerson("Adele"));

        artists.add(createPerson("Chris Martin"));
        artists.add(createPerson("Jonny Buckland"));
        Artist coldplay = createBand("Coldplay", new ArrayList<>());
        artists.add(coldplay);

        artists.add(createPerson("Taylor Swift"));

        artists.add(createPerson("Till Lindemann"));
        artists.add(createPerson("Richard Kruspe"));
        Artist rammstein = createBand("Rammstein", new ArrayList<>());
        artists.add(rammstein);

        artists.add(createPerson("Andreas Gabalier"));
        artists.add(createPerson("Apache 207"));

        artists.add(createPerson("Gino Emnes"));
        artists.add(createPerson("Deborah Sasson"));
        artists.add(createPerson("Lin-Manuel Miranda"));
        artists.add(createPerson("Leslie Odom Jr."));
        artists.add(createPerson("Elaine Paige"));
        artists.add(createPerson("Sarah Brightman"));
        artists.add(createPerson("Idina Menzel"));
        artists.add(createPerson("Kristin Chenoweth"));
        artists.add(createPerson("Alfie Boe"));
        artists.add(createPerson("Samantha Barks"));

        artists.add(createPerson("Anna Netrebko"));
        artists.add(createPerson("Jonas Kaufmann"));
        artists.add(createPerson("Diana Damrau"));
        artists.add(createPerson("Plácido Domingo"));
        artists.add(createPerson("Renée Fleming"));
        artists.add(createPerson("Bryn Terfel"));
        artists.add(createPerson("Sonya Yoncheva"));

        artists.add(createPerson("Billie Eilish"));
        artists.add(createPerson("The Weeknd"));
        artists.add(createPerson("Dua Lipa"));
        artists.add(createPerson("Harry Styles"));
        artists.add(createPerson("Olivia Rodrigo"));
        artists.add(createPerson("Sabrina Carpenter"));

        List<Artist> savedArtists = new ArrayList<>();
        for (Artist a : artists) {
            savedArtists.add(artistRepository.save(a));
        }

        savedArtists.get(21).setMembers(new ArrayList<>(List.of(savedArtists.get(19), savedArtists.get(20))));
        artistRepository.save(savedArtists.get(21));

        savedArtists.get(26).setMembers(new ArrayList<>(List.of(savedArtists.get(24), savedArtists.get(25))));
        artistRepository.save(savedArtists.get(26));

        savedArtists.get(30).setMembers(new ArrayList<>(List.of(savedArtists.get(28), savedArtists.get(29))));
        artistRepository.save(savedArtists.get(30));

        LOGGER.info("Generated {} artists: 19 actors, 14 musicians, 10 theater, 7 opera, 6 festival",
            savedArtists.size());
        return savedArtists;
    }

    private Artist createPerson(String name) {
        Artist artist = new Artist(name);
        artist.setIsBand(false);
        return artist;
    }

    private Artist createBand(String name, List<Artist> members) {
        Artist band = new Artist(name);
        band.setIsBand(true);
        band.setMembers(members);
        return band;
    }

    // ==================== EVENTS ====================

    private List<Event> generateEvents(List<Location> locations, List<Artist> artists) {
        LOGGER.info("Generating 38 events with matching images...");
        List<Event> events = new ArrayList<>();
        LocalDate today = LocalDate.now();

        events.add(createEvent("Oppenheimer", "Kino", 180,
            "Biopic über J. Robert Oppenheimer",
            today.plusDays(2), 20, 0, locations.get(0),
            getArtistSublist(artists, 0, 1)));

        events.add(createEvent("Dune: Part Two", "Kino", 166,
            "Episches Science-Fiction-Meisterwerk",
            today.plusDays(5), 19, 30, locations.get(1),
            getArtistSublist(artists, 2, 3)));

        events.add(createEvent("Poor Things", "Kino", 141,
            "Fantasy-Drama mit Emma Stone",
            today.plusDays(8), 20, 15, locations.get(2),
            getArtistSublist(artists, 4, 5)));

        events.add(createEvent("Cars 3", "Kino", 109,
            "Pixar-Animationsfilm",
            today.plusDays(11), 15, 0, locations.get(3),
            getArtistSublist(artists, 6)));

        events.add(createEvent("Barbie", "Kino", 114,
            "Fantasy-Komödie von Greta Gerwig",
            today.plusDays(14), 20, 0, locations.get(0),
            getArtistSublist(artists, 7, 8)));

        events.add(createEvent("The Batman", "Kino", 176,
            "Dunkler Superhelden-Thriller",
            today.plusDays(17), 21, 0, locations.get(1),
            getArtistSublist(artists, 9, 10)));

        events.add(createEvent("Odyssey", "Kino", 148,
            "Episches Abenteuer nach Homer",
            today.plusDays(20), 19, 30, locations.get(2),
            getArtistSublist(artists, 11)));

        events.add(createEvent("The Creator", "Kino", 133,
            "Science-Fiction über KI",
            today.plusDays(23), 20, 0, locations.get(3),
            getArtistSublist(artists, 12, 13)));

        events.add(createEvent("Avatar 3", "Kino", 195,
            "Fortsetzung der Avatar-Saga",
            today.plusDays(26), 19, 0, locations.get(0),
            getArtistSublist(artists, 14, 15)));

        events.add(createEvent("Greenland 2: Migration", "Kino", 142,
            "Actiongeladener Katastrophen-Thriller",
            today.plusDays(29), 20, 30, locations.get(1),
            getArtistSublist(artists, 16, 17)));

        events.add(createEvent("Mercy", "Kino", 128,
            "Psychologischer Thriller",
            today.plusDays(32), 21, 15, locations.get(2),
            getArtistSublist(artists, 18)));

        events.add(createEvent("Metallica Live in Vienna", "Konzert", 150,
            "Heavy-Metal-Legenden",
            today.plusDays(4), 20, 0, locations.get(4),
            getArtistSublist(artists, 21)));

        events.add(createEvent("Ed Sheeran - Mathematics Tour", "Konzert", 120,
            "Britischer Pop-Superstar",
            today.plusDays(9), 20, 30, locations.get(5),
            getArtistSublist(artists, 22)));

        events.add(createEvent("Adele - The Final Shows", "Konzert", 135,
            "Die Soulstimme unserer Zeit",
            today.plusDays(13), 21, 0, locations.get(6),
            getArtistSublist(artists, 23)));

        events.add(createEvent("Coldplay - Music of the Spheres", "Konzert", 150,
            "Spektakuläre Live-Show",
            today.plusDays(18), 19, 30, locations.get(7),
            getArtistSublist(artists, 26)));

        events.add(createEvent("Taylor Swift - The Eras Tour", "Konzert", 180,
            "Mega-Tour durch ihre Karriere",
            today.plusDays(22), 19, 0, locations.get(4),
            getArtistSublist(artists, 27)));

        events.add(createEvent("Rammstein Stadium Show", "Konzert", 165,
            "Industrial Metal mit Feuershow",
            today.plusDays(27), 20, 0, locations.get(5),
            getArtistSublist(artists, 30)));

        events.add(createEvent("Andreas Gabalier - Volks-Rock'n'Roll", "Konzert", 140,
            "Volksrock'n'Roller live",
            today.plusDays(31), 20, 30, locations.get(6),
            getArtistSublist(artists, 31)));

        events.add(createEvent("Apache 207 Live", "Konzert", 120,
            "Deutscher Rap-Superstar",
            today.plusDays(35), 21, 0, locations.get(7),
            getArtistSublist(artists, 32)));

        events.add(createEvent("Der König der Löwen", "Theater", 180,
            "Erfolgreichstes Musical aller Zeiten",
            today.plusDays(6), 19, 0, locations.get(8),
            getArtistSublist(artists, 33, 34)));

        events.add(createEvent("Hamilton", "Theater", 165,
            "Broadway-Hit über Gründungsväter",
            today.plusDays(10), 19, 30, locations.get(9),
            getArtistSublist(artists, 35, 36)));

        events.add(createEvent("Cats", "Theater", 150,
            "Klassiker von Andrew Lloyd Webber",
            today.plusDays(15), 20, 0, locations.get(10),
            getArtistSublist(artists, 37)));

        events.add(createEvent("Das Phantom der Oper", "Theater", 155,
            "Romantisches Musical-Meisterwerk",
            today.plusDays(19), 19, 0, locations.get(11),
            getArtistSublist(artists, 38)));

        events.add(createEvent("Wicked - Die Hexen von Oz", "Theater", 165,
            "Magische Vorgeschichte von Oz",
            today.plusDays(24), 19, 30, locations.get(8),
            getArtistSublist(artists, 39, 40)));

        events.add(createEvent("Les Misérables", "Theater", 180,
            "Episches Musical nach Victor Hugo",
            today.plusDays(28), 18, 30, locations.get(9),
            getArtistSublist(artists, 41, 42)));

        events.add(createEvent("Der Glöckner von Notre Dame", "Theater", 140,
            "Disney-Musical",
            today.plusDays(33), 20, 0, locations.get(10),
            getArtistSublist(artists, 33)));

        events.add(createEvent("Die Zauberflöte", "Oper", 180,
            "Mozarts berühmteste Oper",
            today.plusDays(7), 19, 0, locations.get(12),
            getArtistSublist(artists, 43, 48)));

        events.add(createEvent("La Traviata", "Oper", 150,
            "Verdis romantische Tragödie",
            today.plusDays(12), 19, 30, locations.get(13),
            getArtistSublist(artists, 43, 46)));

        events.add(createEvent("Carmen", "Oper", 165,
            "Bizets leidenschaftliche Oper",
            today.plusDays(16), 20, 0, locations.get(14),
            getArtistSublist(artists, 44)));

        events.add(createEvent("Tosca", "Oper", 145,
            "Puccinis dramatisches Meisterwerk",
            today.plusDays(21), 19, 0, locations.get(12),
            getArtistSublist(artists, 44, 49)));

        events.add(createEvent("Der Barbier von Sevilla", "Oper", 160,
            "Rossinis komische Oper",
            today.plusDays(25), 19, 30, locations.get(13),
            getArtistSublist(artists, 45)));

        events.add(createEvent("Rigoletto", "Oper", 135,
            "Verdis tragische Oper",
            today.plusDays(30), 20, 0, locations.get(14),
            getArtistSublist(artists, 45)));

        events.add(createEvent("Die Entführung aus dem Serail", "Oper", 150,
            "Mozarts Singspiel",
            today.plusDays(34), 19, 0, locations.get(12),
            getArtistSublist(artists, 47)));

        events.add(createEvent("Harmony Festival", "Festival", 480,
            "Mehrtägiges Festival mit Top-Acts",
            today.plusDays(40), 14, 0, locations.get(15),
            getArtistSublist(artists, 50, 51, 52, 27)));

        events.add(createEvent("Food & Music Festival Vienna", "Festival", 360,
            "Kulinarik trifft Live-Musik",
            today.plusDays(45), 12, 0, locations.get(16),
            getArtistSublist(artists, 53, 22, 23)));

        events.add(createEvent("Jazz Fest Wien", "Festival", 300,
            "Internationales Jazz-Festival",
            today.plusDays(50), 19, 0, locations.get(15),
            getArtistSublist(artists, 54, 55)));

        events.add(createEvent("Fridays Festival", "Festival", 420,
            "Größtes Wochenend-Festival Österreichs",
            today.plusDays(55), 13, 0, locations.get(16),
            getArtistSublist(artists, 21, 26, 30)));

        events.forEach(e -> eventRepository.save(e));

        LOGGER.info("Generated 38 events: 11 Kino, 8 Konzert, 7 Theater, 7 Oper, 4 Festival");
        return events;
    }

    // ==================== IMAGE UPLOAD ====================
    private void uploadEventImages(List<Event> events) {
        LOGGER.info("Uploading images for {} events...", events.size());

        Map<String, String> typePrefixes = Map.of(
            "Kino", "kino",
            "Konzert", "concert",
            "Theater", "theater",
            "Oper", "opera",
            "Festival", "festival"
        );

        Map<String, Integer> counters = new HashMap<>();
        typePrefixes.keySet().forEach(type -> counters.put(type, 1));

        for (Event event : events) {
            String type = event.getType();
            String prefix = typePrefixes.getOrDefault(type, "concert");
            int number = counters.getOrDefault(type, 1);

            String filename = prefix + number + ".jpg";
            String imagePath = "src/main/resources/eventImages/" + filename;

            try {
                java.io.File file = new java.io.File(imagePath);
                if (file.exists()) {
                    byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());

                    event.setImage(imageBytes);
                    event.setImageContentType("image/jpeg");
                    eventRepository.save(event);

                    LOGGER.info("✅ Uploaded {} for: {}", filename, event.getTitle());
                    counters.put(type, number + 1);
                } else {
                    LOGGER.warn("⚠️  Image not found: {}", imagePath);
                }
            } catch (Exception e) {
                LOGGER.error("❌ Failed to upload image for {}: {}", event.getTitle(), e.getMessage());
            }
        }

        LOGGER.info("Image upload completed!");
    }

    private void createSeatsForSectorPattern(

        Sector sector,
        PriceCategory priceCategory,
        int rowStart,
        int rowEnd,
        java.util.function.IntFunction<int[]> patternForRow
    ) {
        for (int row = rowStart; row <= rowEnd; row++) {

            int[] groups = patternForRow.apply(row);

            int seatNumber = 0;

            for (int g = 0; g < groups.length; g++) {
                int groupSize = groups[g];

                for (int i = 0; i < groupSize; i++) {
                    seatNumber++;

                    Seat seat = new Seat();
                    seat.setRowNumber(row);
                    seat.setSeatNumber(seatNumber);
                    seat.setSector(sector);
                    seat.setPriceCategory(priceCategory);
                    seatRepository.save(seat);
                }

                if (g < groups.length - 1) {
                    seatNumber++;
                }
            }
        }
    }

    private void createSeatsForSectorPatternWithCustomGaps(
        Sector sector,
        PriceCategory priceCategory,
        int rowStart,
        int rowEnd,
        IntFunction<int[]> groupsForRow,
        IntFunction<int[]> gapsForRow
    ) {

        for (int row = rowStart; row <= rowEnd; row++) {
            int[] groups = groupsForRow.apply(row);
            int[] gaps = gapsForRow.apply(row);

            int seatNumber = 0;

            for (int g = 0; g < groups.length; g++) {
                int groupSize = groups[g];

                for (int i = 0; i < groupSize; i++) {
                    seatNumber++;

                    Seat seat = new Seat();
                    seat.setRowNumber(row);
                    seat.setSeatNumber(seatNumber);
                    seat.setSector(sector);
                    seat.setPriceCategory(priceCategory);
                    seatRepository.save(seat);
                }

                if (g < groups.length - 1) {
                    int gap = gaps[g];
                    seatNumber += gap;
                }
            }
        }
    }


    // ==================== TICKETS ====================

    private void generateSeatsAndTickets(List<Location> locations, List<Event> events) {
        LOGGER.info("Generating PARTIAL tickets for events...");

        Random random = new Random(42);

        for (Event event : events) {
            Long locationId = event.getLocation().getId();
            List<Sector> sectors = sectorRepository.findByLocationId(locationId);
            List<Long> sectorIds = sectors.stream().map(Sector::getId).toList();
            List<Seat> seats = seatRepository.findBySectorIdIn(sectorIds);

            Collections.shuffle(seats, random);

            int ticketCount = 5 + random.nextInt(8);
            ticketCount = Math.min(ticketCount, seats.size());

            for (int i = 0; i < ticketCount; i++) {
                Seat seat = seats.get(i);

                int basePrice = seat.getPriceCategory() != null
                    ? seat.getPriceCategory().getBasePrice()
                    : 2000;

                double netPrice = basePrice / 100.0;
                double taxRate = 0.20;
                double grossPrice = round2(netPrice * (1.0 + taxRate));

                Ticket ticket = new Ticket(seat, event);
                ticket.setNetPrice(netPrice);
                ticket.setTaxRate(taxRate);
                ticket.setGrossPrice(grossPrice);

                ticketRepository.save(ticket);
            }
        }

        LOGGER.info("Limited tickets generated for {} events", events.size());
    }


    // ==================== MERCHANDISE ====================

    private List<Merchandise> generateMerchandise() {
        LOGGER.info("Generating merchandise...");
        List<Merchandise> items = new ArrayList<>();

        Object[][] merchData = {
            {"Konzert T-Shirt Classic", "Hochwertiges Baumwoll-Shirt mit Bandlogo", new BigDecimal("29.99"), 100, 3, false, null, null,
                null},
            {"Festival Hoodie", "Warmer Kapuzenpullover für Open-Air Events", new BigDecimal("54.99"), 50, 5, false, null, null, null},
            {"Limited Edition Poster", "Signiertes Konzertposter A2", new BigDecimal("19.99"), 200, 2, false, null, null, null},
            {"Fanschal Premium", "Gestickter Fanschal aus Acryl", new BigDecimal("14.99"), 150, 1, false, null, null, null},
            {"Vinyl Schallplatte", "Limited Edition Vinyl des neuesten Albums", new BigDecimal("34.99"), 75, 3, false, null, null, null},
            {"Backstage Pass Replica", "Nachbildung eines echten Backstage-Passes", new BigDecimal("9.99"), 500, 1, true, null, null, 150},
            {"VIP Goodie Bag", "Exklusive Merchandise-Tasche mit Überraschungen", new BigDecimal("79.99"), 30, 8, true, null, null,
                1000},
            {"Bandana mit Logo", "Stylisches Bandana mit Aufdruck", new BigDecimal("7.99"), 300, 1, false, null, null, null},
            {"Konzert-Cap", "Basecap mit gesticktem Logo", new BigDecimal("24.99"), 120, 2, false, null, null, null},
            {"USB-Stick mit Live-Album", "8GB USB-Stick mit exklusivem Live-Mitschnitt", new BigDecimal("16.99"), 80, 2, true, null, null,
                250},
            {"Tasse XXL", "Große Keramiktasse mit Bandmotiv", new BigDecimal("12.99"), 200, 1, false, null, null, null},
            {"Turnbeutel", "Praktischer Sportbeutel mit Kordelzug", new BigDecimal("9.99"), 150, 1, false, null, null, null},
            {"Schlüsselanhänger Metal", "Hochwertiger Metall-Schlüsselanhänger", new BigDecimal("5.99"), 500, 1, true, null, null, 75},
            {"Poster Set (3 Stück)", "3 verschiedene Poster im Set", new BigDecimal("34.99"), 100, 3, false, null, null, null},
            {"Festival Armband", "Gewebtes Festival-Armband", new BigDecimal("4.99"), 1000, 0, false, null, null, null},
            {"Button Set (18 Stück)", "18 verschiedene Buttons mit Bandmotiven", new BigDecimal("14.99"), 200, 1, false, null, null,
                null},
            {"Notizbuch A5", "Liniertes Notizbuch mit Hardcover", new BigDecimal("11.99"), 100, 1, false, null, null, null},
            {"Thermobecher", "Isolierbecher für heiße und kalte Getränke", new BigDecimal("19.99"), 80, 2, false, null, null, null},
            {"Regencape", "Praktisches Einweg-Regencape für Festivals", new BigDecimal("3.99"), 500, 0, false, null, null, null},
            {"Sitzkissen", "Aufblasbares Sitzkissen für Konzerte", new BigDecimal("8.99"), 200, 1, false, null, null, null}
        };

        for (Object[] data : merchData) {
            Merchandise merch = new Merchandise();
            merch.setName((String) data[0]);
            merch.setDescription((String) data[1]);
            merch.setUnitPrice((BigDecimal) data[2]);
            merch.setRemainingQuantity((Integer) data[3]);
            merch.setRewardPointsPerUnit((Integer) data[4]);
            merch.setRedeemableWithPoints((Boolean) data[5]);
            merch.setPointsPrice((Integer) data[8]);
            items.add(merchandiseRepository.save(merch));
        }

        LOGGER.info("Generated {} merchandise items", items.size());
        return items;
    }


    private void uploadMerchandiseImages(List<Merchandise> items) {
        LOGGER.info("Uploading images for {} merchandise items...", items.size());

        for (int i = 0; i < items.size(); i++) {
            Merchandise merch = items.get(i);
            int number = i + 1;

            String[] exts = {"webp", "jpg", "jpeg", "png"};

            boolean uploaded = false;

            for (String ext : exts) {
                String filename = "merch" + number + "." + ext;
                String imagePath = "src/main/resources/merchandiseImages/" + filename;

                try {
                    java.io.File file = new java.io.File(imagePath);
                    if (!file.exists()) {
                        continue;
                    }

                    byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());

                    merch.setImage(imageBytes);
                    merch.setImageContentType(contentTypeForExtension(ext));
                    merchandiseRepository.save(merch);

                    LOGGER.info("Uploaded {} for: {}", filename, merch.getName());
                    uploaded = true;
                    break;
                } catch (Exception e) {
                    LOGGER.error("Failed to upload image for {} ({}): {}", merch.getName(), imagePath, e.getMessage());
                    uploaded = true;
                    break;
                }
            }

            if (!uploaded) {
                LOGGER.warn("No image found for merchandise #{} (expected merch{}.webp/jpg/jpeg/png)", number, number);
            }
        }

        LOGGER.info("Merchandise image upload completed!");
    }

    private String contentTypeForExtension(String ext) {
        return switch (ext.toLowerCase(java.util.Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    // ==================== INVOICES & RESERVATIONS ====================

    private void generateInvoicesAndReservations(List<User> users, List<Event> events, List<Merchandise> merchandise) {
        LOGGER.info("Generating invoices and reservations...");

        Random random = new Random(42);
        int invoiceCounter = 1;
        int reservationCounter = 1;

        List<Ticket> allTickets = ticketRepository.findAll();
        Collections.shuffle(allTickets, random);

        int splitIndex = (int) (allTickets.size() * 0.6);

        List<Ticket> invoiceTickets = allTickets.subList(0, splitIndex);
        List<Ticket> reservationTickets = allTickets.subList(splitIndex, allTickets.size());

        Map<Event, List<Ticket>> ticketsByEventInvoice =
            invoiceTickets.stream().collect(Collectors.groupingBy(Ticket::getEvent));

        for (Event event : ticketsByEventInvoice.keySet()) {
            List<Ticket> tickets = ticketsByEventInvoice.get(event);
            int index = 0;

            while (index < tickets.size()) {
                User user = users.get(random.nextInt(users.size()));
                Invoice invoice = new Invoice();

                invoice.setUser(user);
                invoice.setInvoiceNumber(String.format("INV-2025-%03d", invoiceCounter++));
                invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(60)));
                invoice.setEventDate(event.getDateTime());
                invoice = invoiceRepository.save(invoice);

                int blockSize = 1 + random.nextInt(3);

                double netTotal = 0;
                double taxTotal = 0;
                double grossTotal = 0;

                for (int i = 0; i < blockSize && index < tickets.size(); i++) {
                    Ticket t = tickets.get(index++);
                    t.setInvoice(invoice);
                    ticketRepository.save(t);

                    netTotal += t.getNetPrice();
                    taxTotal += t.getNetPrice() * t.getTaxRate();
                    grossTotal += t.getGrossPrice();
                }
                invoice.setNetTotal(round2(netTotal));
                invoice.setTaxTotal(round2(taxTotal));
                invoice.setGrossTotal(round2(grossTotal));
                invoice = invoiceRepository.save(invoice);


                if (random.nextBoolean()) {
                    Merchandise merch = merchandise.get(random.nextInt(merchandise.size()));
                    int quantity = 1 + random.nextInt(3);

                    InvoiceMerchandiseItem item = new InvoiceMerchandiseItem(invoice, merch, quantity);
                    invoiceMerchandiseItemRepository.save(item);

                    double merchNet = merch.getUnitPrice().doubleValue() * quantity;
                    netTotal += merchNet;
                    taxTotal += merchNet * 0.20;
                    grossTotal += merchNet * 1.20;

                    invoice.setNetTotal(round2(netTotal));
                    invoice.setTaxTotal(round2(taxTotal));
                    invoice.setGrossTotal(round2(grossTotal));
                    invoiceRepository.save(invoice);
                }
            }
        }
        Map<Event, List<Ticket>> ticketsByEventReservation =
            reservationTickets.stream().collect(Collectors.groupingBy(Ticket::getEvent));

        for (Event event : ticketsByEventReservation.keySet()) {
            List<Ticket> tickets = ticketsByEventReservation.get(event);
            int index = 0;

            while (index < tickets.size()) {
                User user = users.get(1 + random.nextInt(users.size() - 1));

                Reservation reservation = new Reservation(user, event);
                reservation.setReservationNumber(String.format("RES-2025-%03d", reservationCounter++));
                reservation = reservationRepository.save(reservation);

                int blockSize = 1 + random.nextInt(3);

                for (int i = 0; i < blockSize && index < tickets.size(); i++) {
                    Ticket t = tickets.get(index++);
                    t.setReservation(reservation);
                    ticketRepository.save(t);
                }
            }
        }


        LOGGER.info("Generated 25 invoices and 20 reservations");
    }

    // ==================== CART ====================
    private void generateCarts(List<User> users) {
        LOGGER.info("Generating carts for users...");

        for (User user : users) {
            cartRepository.findByUserId(user.getId()).orElseGet(() -> {
                Cart cart = new Cart(user);
                cart = cartRepository.save(cart);

                List<Ticket> tickets = ticketRepository.findAll();
                if (!tickets.isEmpty()) {
                    Random random = new Random();
                    int ticketCount = 1 + random.nextInt(2);
                    Collections.shuffle(tickets, random);
                    for (int i = 0; i < ticketCount && i < tickets.size(); i++) {
                        Ticket ticket = tickets.get(i);
                        CartItem item = new CartItem(CartItemType.TICKET);
                        item.setCart(cart);
                        item.setTicket(ticket);
                        cartItemRepository.save(item);
                    }
                }

                return cart;
            });
        }

        LOGGER.info("Carts generated for {} users", users.size());
    }


    // ==================== NEWS ====================

    private List<NewsItem> generateNews() {
        LOGGER.info("Generating news items...");
        List<NewsItem> newsItems = new ArrayList<>();

        Object[][] newsData = {
            {"Willkommen bei Ticketline",
                "Die neue Ticketplattform für Österreich",
                "Ticketline ist ab sofort online und bietet euch eine moderne Plattform für den Ticketkauf. Entdeckt Konzerte, Festivals, Theater, "
                    + "Opern und viele weitere Events in ganz Österreich. Mit einer intuitiven Benutzeroberfläche, sicherem Zahlungsprozess und stets aktuellen Informationen wollen wir euer Event-Erlebnis von Anfang an verbessern.",
                LocalDate.now().minusDays(60)},

            {"Neue Konzerte verfügbar",
                "Große Auswahl an neuen Live-Events",
                "Unser Konzertangebot wurde erweitert. Zahlreiche nationale und internationale Künstler haben neue Tourdaten veröffentlicht, die ab sofort bei Ticketline verfügbar sind. "
                    + "Egal ob Rock, Pop, Jazz oder Klassik – hier findet jede*r das passende Event.",
                LocalDate.now().minusDays(58)},

            {"Sommer-Festivals 2026",
                "Der Festival-Sommer nimmt Form an",
                "Der kommende Sommer verspricht unvergessliche Festivalmomente. Von großen Open-Air-Events bis hin zu kleineren "
                    + "Indie-Festivals ist alles dabei. Sichert euch frühzeitig Tickets und plant euren Festivalsommer mit Ticketline.",
                LocalDate.now().minusDays(56)},

            {"Metallica Zusatzshow",
                "Zweite Show aufgrund hoher Nachfrage",
                "Die Nachfrage nach Metallica-Tickets war überwältigend. Deshalb wurde eine Zusatzshow angekündigt. Fans haben nun eine weitere Chance, "
                    + "die Band live in Österreich zu erleben. Tickets sind nur solange der Vorrat reicht verfügbar.",
                LocalDate.now().minusDays(54)},

            {"Opernsaison gestartet",
                "Klassische Highlights in Wien",
                "Die neue Opernsaison bringt zahlreiche hochkarätige Produktionen auf die Bühnen Wiens. Internationale Stars, beeindruckende Inszenierungen und zeitlose Werke sorgen für musikalische Abende auf höchstem Niveau.",
                LocalDate.now().minusDays(52)},

            {"Musical-Neuheiten 2026",
                "Neue Produktionen feiern Premiere",
                "Mehrere neue Musical-Produktionen feiern dieses Jahr ihre Premiere. Moderne Inszenierungen treffen auf bekannte Klassiker und sorgen für ein abwechslungsreiches Programm für Musical-Fans jeden Alters.",
                LocalDate.now().minusDays(50)},

            {"Jazz Fest Wien",
                "Internationale Stars zu Gast",
                "Das Jazz Fest Wien bringt auch dieses Jahr renommierte Künstler aus aller Welt in die Stadt. Freut euch auf abwechslungsreiche Konzerte, intime Club-Auftritte und große Bühnenmomente.",
                LocalDate.now().minusDays(48)},

            {"Rock Classics Live",
                "Legendäre Bands auf Tour",
                "Mehrere Rock-Legenden gehen erneut auf Tour und machen Halt in Österreich. Erlebt zeitlose Klassiker, mitreißende Gitarrenriffs und unvergessliche Live-Momente mit tausenden Fans.",
                LocalDate.now().minusDays(46)},

            {"Pop-Sommer angekündigt",
                "Internationale Popstars live",
                "Der Pop-Sommer 2026 steht vor der Tür. Zahlreiche internationale Superstars haben Konzerte in Österreich angekündigt. Erwartet große Shows, beeindruckende Bühnenbilder und unvergessliche Hits.",
                LocalDate.now().minusDays(44)},

            {"Indie Bands im Fokus",
                "Aufstrebende Acts entdecken",
                "Die Indie-Szene boomt. Immer mehr aufstrebende Bands füllen die Konzertkalender und begeistern mit frischen Sounds. Ticketline bietet euch eine Plattform, um neue Lieblingsbands live zu entdecken.",
                LocalDate.now().minusDays(42)},

            {"Comedy-Abende 2026",
                "Lachen garantiert",
                "Bekannte Comedians und neue Talente touren durch Österreich. Freut euch auf humorvolle Abende, pointierte Programme und beste Unterhaltung in Theatern und Hallen.",
                LocalDate.now().minusDays(40)},

            {"Klassik Open Air",
                "Musik unter freiem Himmel",
                "Open-Air-Konzerte mit klassischer Musik bieten ein besonderes Erlebnis. In einzigartiger Atmosphäre erklingen bekannte Werke, die Musikliebhaber*innen begeistern.",
                LocalDate.now().minusDays(38)},

            {"Familien-Events",
                "Unterhaltung für Groß und Klein",
                "Auch für Familien gibt es zahlreiche neue Events. Kindertheater, Mitmach-Shows und familienfreundliche Konzerte sorgen für gemeinsame Erlebnisse und unvergessliche Momente.",
                LocalDate.now().minusDays(36)},

            {"Theaterpremieren",
                "Neue Stücke auf den Bühnen",
                "Mehrere Theaterhäuser feiern Premieren neuer Stücke. Moderne Interpretationen, klassische Dramen und zeitgenössische Werke bieten ein vielfältiges Kulturprogramm.",
                LocalDate.now().minusDays(34)},

            {"Festival-Tickets im Vorverkauf",
                "Jetzt frühzeitig sichern",
                "Viele Festivals haben ihren Vorverkauf gestartet. Frühbucher profitieren von besserer Auswahl und Planungssicherheit. Ticketline informiert euch rechtzeitig über alle wichtigen Termine.",
                LocalDate.now().minusDays(32)},

            {"Elektronische Nächte",
                "DJ-Events und Clubshows",
                "Fans elektronischer Musik kommen voll auf ihre Kosten. Internationale DJs und bekannte Acts sorgen für lange Nächte und intensive Club-Erlebnisse in ganz Österreich.",
                LocalDate.now().minusDays(30)},

            {"Kulturherbst geplant",
                "Vielfältiges Programm im Herbst",
                "Der Kulturherbst bringt eine Mischung aus Konzerten, Theater, Lesungen und Sonderveranstaltungen. Ein abwechslungsreiches Programm für alle, die Kultur lieben.",
                LocalDate.now().minusDays(28)},

            {"Filmkonzerte",
                "Große Filme mit Live-Orchester",
                "Beliebte Filmklassiker werden mit Live-Orchester aufgeführt. Die Kombination aus Bild und Musik schafft ein intensives Erlebnis für Film- und Musikfans.",
                LocalDate.now().minusDays(26)},

            {"Studentenrabatte",
                "Ermäßigte Tickets verfügbar",
                "Für viele Veranstaltungen gibt es spezielle Studentenrabatte. So wird Kultur auch für junge Menschen leistbar und zugänglich.",
                LocalDate.now().minusDays(24)},

            {"Barrierefreie Events",
                "Kultur für alle",
                "Immer mehr Veranstalter setzen auf barrierefreie Angebote. Ticketline kennzeichnet diese Events klar, damit alle Besucher*innen ihren Eventbesuch planen können.",
                LocalDate.now().minusDays(22)},

            {"Neujahrskonzerte",
                "Musikalischer Start ins Jahr",
                "Traditionelle Neujahrskonzerte bieten einen festlichen Start ins neue Jahr. Klassische Werke und bekannte Melodien sorgen für einen stimmungsvollen Jahresbeginn.",
                LocalDate.now().minusDays(20)},

            {"Open-Air Kinos",
                "Filmgenuss unter Sternen",
                "In den Sommermonaten laden Open-Air-Kinos zu besonderen Filmabenden ein. Genießt aktuelle Filme und Klassiker in entspannter Atmosphäre.",
                LocalDate.now().minusDays(18)},

            {"Sportevents im Fokus",
                "Live dabei sein",
                "Auch Sportfans kommen auf ihre Kosten. Große Sportevents, Turniere und Showmatches sind über Ticketline buchbar.",
                LocalDate.now().minusDays(16)},

            {"Literaturveranstaltungen",
                "Lesungen und Talks",
                "Autor*innenlesungen, Buchpräsentationen und Literaturfestivals bieten spannende Einblicke in aktuelle Werke und kreative Prozesse.",
                LocalDate.now().minusDays(14)},

            {"Kinder-Musicals",
                "Magische Erlebnisse",
                "Speziell für Kinder konzipierte Musicals bringen bekannte Geschichten auf die Bühne und sorgen für leuchtende Augen im Publikum.",
                LocalDate.now().minusDays(12)},

            {"Silvester-Events",
                "Feiern zum Jahreswechsel",
                "Zahlreiche Events laden dazu ein, das Jahr gemeinsam zu verabschieden. Konzerte, Galas und Partys sorgen für einen gelungenen Jahreswechsel.",
                LocalDate.now().minusDays(10)},

            {"Neue Spielstätten",
                "Mehr Locations im Angebot",
                "Ticketline erweitert laufend das Angebot an Spielstätten. Neue Locations sorgen für noch mehr Vielfalt im Eventkalender.",
                LocalDate.now().minusDays(8)},

            {"Nachhaltige Events",
                "Umweltbewusst feiern",
                "Immer mehr Veranstaltungen setzen auf Nachhaltigkeit. Von umweltfreundlicher Organisation bis zu regionalen Partnern wird Verantwortung übernommen.",
                LocalDate.now().minusDays(6)},

            {"Winterkonzerte",
                "Musik in der kalten Jahreszeit",
                "Auch im Winter gibt es zahlreiche Konzerte und Veranstaltungen, die für warme Stimmung und besondere Abende sorgen.",
                LocalDate.now().minusDays(4)},

            {"Ticketline Updates",
                "Neue Funktionen verfügbar",
                "Unsere Plattform wurde weiter verbessert. Neue Funktionen erleichtern die Suche nach Events und machen den Ticketkauf noch angenehmer.",
                LocalDate.now().minusDays(2)},

            {"Ausblick 2026",
                "Ein spannendes Eventjahr",
                "Das kommende Jahr verspricht ein vielfältiges Eventangebot. Ticketline freut sich darauf, euch auch 2026 mit den besten Veranstaltungen zu begleiten.",
                LocalDate.now()}
        };


        for (Object[] data : newsData) {
            NewsItem news = new NewsItem((String) data[0], (String) data[1], (String) data[2], (LocalDate) data[3]);
            newsItems.add(news);
            newsItemRepository.save(news);
        }

        LOGGER.info("Generated {} news items", newsData.length);
        return newsItems;
    }

    private void uploadNewsImages(List<NewsItem> news) {
        LOGGER.info("Uploading news for {} events...", news.size());

        for (int i = 0; i < news.size(); i++) {
            String imagePath = "src/main/resources/newsImages/news" + i + ".jpg";
            NewsItem newsItem = news.get(i);

            try {
                java.io.File file = new java.io.File(imagePath);
                if (file.exists()) {
                    byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());

                    newsItem.setImageData(imageBytes);
                    newsItem.setImageContentType("image/jpeg");
                    newsItemRepository.save(newsItem);

                    LOGGER.info("Uploaded {} for: {}", imagePath, newsItem.getTitle());
                } else {
                    LOGGER.warn("Image not found: {}", imagePath);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to upload image for {}: {}", newsItem.getTitle(), e.getMessage());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Location createLocation(int zip, String city, String street, String houseNumber,
                                    String name, String stagePos, String stageLabel) {
        Location loc = new Location(zip, city, street, houseNumber);
        loc.setName(name);
        loc.setStagePosition(stagePos);
        loc.setStageLabel(stageLabel);
        return loc;
    }

    private int getVariantForIndex(int index) {
        if (index >= 0 && index <= 3) {
            return 0;
        }
        if (index >= 4 && index <= 7) {
            return 1;
        }
        if (index >= 8 && index <= 14) {
            return 2;
        }
        if (index >= 15 && index <= 16) {
            return 3;
        }
        return 0;
    }

    private String getVenueTypeForVariant(int variant) {
        return switch (variant) {
            case 0 -> "Kino";
            case 1 -> "Concert";
            case 2 -> "Musical";
            case 3 -> "Festival";
            default -> "Concert";
        };
    }

    private void configureStage(Location loc, int variant) {
        loc.setStageHeightPx(180);
        loc.setStageWidthPx(700);
        loc.setRunwayWidthPx(null);
        loc.setRunwayLengthPx(null);
        loc.setRunwayOffsetPx(null);
        loc.setStageRowStart(null);
        loc.setStageRowEnd(null);
        loc.setStageColStart(null);
        loc.setStageColEnd(null);

        switch (variant) {
            case 0 -> {
                loc.setStagePosition("BOTTOM");
                loc.setStageHeightPx(100);
                loc.setStageWidthPx(800);
            }
            case 1 -> {
                loc.setStagePosition("CENTER");
                loc.setStageRowStart(9);
                loc.setStageRowEnd(17);

                loc.setStageColStart(9);
                loc.setStageColEnd(11);

                loc.setStageHeightPx(160);
                loc.setStageWidthPx(220);
            }

            case 2 -> {
                loc.setStagePosition("BOTTOM");
                loc.setStageHeightPx(100);
                loc.setStageWidthPx(400);
            }
            case 3 -> {
                loc.setStagePosition("CENTER");
                loc.setStageRowStart(7);
                loc.setStageRowEnd(11);
                loc.setStageColStart(8);
                loc.setStageColEnd(12);
                loc.setStageHeightPx(220);
                loc.setStageWidthPx(220);
            }
            default -> {
                loc.setStagePosition("BOTTOM");
                loc.setStageHeightPx(100);
                loc.setStageWidthPx(800);
            }
        }
    }

    private void createSeatsFixedGridRadialPricing(
        int totalCols,
        int rowStart, int rowEnd,
        int colStart, int colEnd,
        IntFunction<boolean[]> blockedColsForRow,
        // Bühne
        int stageRowStart, int stageRowEnd,
        int stageColStart, int stageColEnd,
        // Sektoren + PCs
        Sector standing, PriceCategory pcFree,
        Sector cheap, PriceCategory pcCheap,
        Sector standard, PriceCategory pcStandard,
        Sector premium, PriceCategory pcPremium
    ) {
        for (int row = rowStart; row <= rowEnd; row++) {
            boolean[] blocked = blockedColsForRow.apply(row);

            for (int col = colStart; col <= colEnd; col++) {
                if (blocked != null && col < blocked.length && blocked[col]) {
                    continue; // Bühne / Gang
                }

                // Distanz zur Bühne (Rechteck)
                int dx = 0;
                if (col < stageColStart) {
                    dx = stageColStart - col;
                } else if (col > stageColEnd) {
                    dx = col - stageColEnd;
                }

                int dy = 0;
                if (row < stageRowStart) {
                    dy = stageRowStart - row;
                } else if (row > stageRowEnd) {
                    dy = row - stageRowEnd;
                }

                int ring = Math.max(dx, dy);

                // Ring -> Kategorie
                Sector s;
                PriceCategory pc;

                if (ring <= 1) {
                    s = premium;
                    pc = pcPremium;
                } else if (ring <= 3) {
                    s = standard;
                    pc = pcStandard;
                } else if (ring <= 5) {
                    s = cheap;
                    pc = pcCheap;
                } else {
                    s = standing;
                    pc = pcFree;
                }

                saveSeat(row, col, s, pc);
            }
        }
    }


    private Event createEvent(String title, String type, int duration, String description,
                              LocalDate date, int hour, int minute,
                              Location location, List<Artist> artists) {
        Event event = new Event();
        event.setTitle(title);
        event.setType(type);
        event.setDurationMinutes(duration);
        event.setDescription(description);
        event.setDateTime(LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)));
        event.setLocation(location);
        event.setArtists(artists);
        event.setTickets(new ArrayList<>());
        return event;
    }

    private List<Artist> getArtistSublist(List<Artist> artists, int... indices) {
        List<Artist> result = new ArrayList<>();
        for (int index : indices) {
            if (index >= 0 && index < artists.size()) {
                result.add(artists.get(index));
            }
        }
        return result;
    }
}
