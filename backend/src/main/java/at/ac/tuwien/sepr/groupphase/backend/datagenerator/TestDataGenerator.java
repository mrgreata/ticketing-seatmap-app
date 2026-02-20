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
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
@Profile("test")
public class TestDataGenerator implements ApplicationRunner {

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

    @Value("${app.testdatagen.enabled:false}")
    private boolean enabled;

    // Constants for data generation
    private static final int TARGET_USERS = 1000;
    private static final int TARGET_EVENTS = 200;
    private static final int TARGET_LOCATIONS = 25;
    private static final int TARGET_INVOICES = 500;
    private static final int TARGET_RESERVATIONS = 500;

    private Random random = new Random(42);

    public TestDataGenerator(
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
            LOGGER.info("Test data generation is disabled (app.testdatagen.enabled=false)");
            return;
        }

        LOGGER.info("Starting test data generation...");

        // Cleanup existing data
        cleanupData();

        // Generate massive data sets
        List<User> users = generateLargeUserSet();
        List<Location> locations = generateLargeLocationSet();
        List<Artist> artists = generateLargeArtistSet();
        List<Event> events = generateLargeEventSet(locations, artists);
        List<Sector> allSectors = generateSeatsForAllLocations(locations);
        List<Merchandise> merchandise = generateMerchandiseItems();

        // GENERATE RESERVATIONS (500) with tickets
        List<Reservation> reservations = generateReservations(users, events, allSectors);

        // GENERATE INVOICES (500) with tickets
        generateInvoices(users, events, allSectors, merchandise);

        // Generate  news
        generateNewsItems();

        LOGGER.info("Large-scale data generation completed!");
        LOGGER.info("Generated: {} users, {} locations, {} events, {} invoices, {} reservations",
            users.size(), locations.size(), events.size(), TARGET_INVOICES, TARGET_RESERVATIONS);
    }

    private void cleanupData() {
        LOGGER.info("Cleaning up existing data...");

        invoiceMerchandiseItemRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
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

        LOGGER.info("Cleanup completed");
    }

    // ==================== MASSIVE USER GENERATION ====================

    private List<User> generateLargeUserSet() {
        LOGGER.info("Generating {} users...", TARGET_USERS);
        List<User> users = new ArrayList<>();

        // Generate admin users (1%)
        int adminCount = Math.max(1, TARGET_USERS / 100);
        for (int i = 0; i < adminCount; i++) {
            User admin = new User(
                "admin" + (i + 1) + "@email.com",
                passwordEncoder.encode("password"),
                UserRole.ROLE_ADMIN,
                "Admin" + (i + 1),
                "Super",
                generateRandomAddress()
            );
            admin.setRewardPoints(random.nextInt(10000));
            admin.setTotalCentsSpent(random.nextInt(1000000));
            admin.setLocked(false);
            admin.setLoginFailCount(0);
            users.add(userRepository.save(admin));
        }
        User premiumUser = new User(
            "premium@email.com",
            passwordEncoder.encode("password"),
            UserRole.ROLE_USER,
            "Premium",
            "User",
            "Bahnhofstraße 15, 1020 Wien"
        );
        premiumUser.setRewardPoints(25000);    // Viele Punkte
        premiumUser.setTotalCentsSpent(10000000); // Viel ausgegeben
        premiumUser.setLocked(false);
        users.add(userRepository.save(premiumUser));

        // 3. Normaler User ohne viel Aktivität
        User normalUser = new User(
            "normal@email.com",
            passwordEncoder.encode("password"),
            UserRole.ROLE_USER,
            "Normal",
            "User",
            "Musterweg 3, 1030 Wien"
        );
        normalUser.setRewardPoints(500);
        normalUser.setTotalCentsSpent(50000);
        normalUser.setLocked(false);
        users.add(userRepository.save(normalUser));

        // 4. Gesperrter User
        User lockedUser = new User(
            "locked@email.com",
            passwordEncoder.encode("password"),
            UserRole.ROLE_USER,
            "Gesperrt",
            "User",
            "Parkallee 7, 1040 Wien"
        );
        lockedUser.setRewardPoints(100);
        lockedUser.setTotalCentsSpent(10000);
        lockedUser.setLocked(true);
        lockedUser.setLoginFailCount(5); // Zu viele fehlgeschlagene Versuche
        users.add(userRepository.save(lockedUser));

        // 5. User für Reservierungen/Invoices Tests
        User reservationUser = new User(
            "reservation@email.com",
            passwordEncoder.encode("password"),
            UserRole.ROLE_USER,
            "Reservation",
            "Tester",
            "Dorfplatz 12, 1050 Wien"
        );
        reservationUser.setRewardPoints(1500);
        reservationUser.setTotalCentsSpent(150000);
        users.add(userRepository.save(reservationUser));

        // Generate regular users (99%)
        String[] firstNames = {
            "Max", "Anna", "Peter", "Lisa", "Michael", "Sarah", "Thomas", "Julia",
            "David", "Emma", "Lukas", "Sophie", "Felix", "Marie", "Paul", "Laura",
            "Simon", "Hannah", "Daniel", "Mia", "Leon", "Emily", "Jonas", "Lena",
            "Tim", "Katharina", "Sebastian", "Clara", "Moritz", "Johanna"
        };

        String[] lastNames = {
            "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
            "Becker", "Schulz", "Hoffmann", "Koch", "Bauer", "Richter", "Klein",
            "Wolf", "Schröder", "Neumann", "Schwarz", "Braun", "Hofmann", "Lorenz",
            "Winkler", "Lang", "Baumann", "Franke", "Albrecht", "Schuster", "Simon"
        };

        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "web.de", "gmx.at"};

        for (int i = 0; i < TARGET_USERS - adminCount; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String domain = domains[random.nextInt(domains.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + (i + 1) + "@" + domain;

            User user = new User(
                email,
                passwordEncoder.encode("password"),
                UserRole.ROLE_USER,
                firstName,
                lastName,
                generateRandomAddress()
            );

            // Randomize user properties
            user.setRewardPoints(random.nextInt(5000));
            user.setTotalCentsSpent(random.nextInt(500000));
            user.setLocked(random.nextDouble() < 0.02); // 2% locked
            user.setLoginFailCount(user.isLocked() ? random.nextInt(6) : 0);

            users.add(userRepository.save(user));

            if ((i + 1) % 100 == 0) {
                LOGGER.info("Generated {} users...", i + 1);
            }
        }

        LOGGER.info("Generated {} total users ({} locked)", users.size(),
            users.stream().filter(User::isLocked).count());
        return users;
    }

    private String generateRandomAddress() {
        String[] streets = {"Hauptstraße", "Bahnhofstraße", "Musterweg", "Parkallee",
            "Berggasse", "Dorfplatz", "Industriestraße", "Waldweg"};
        String[] cities = {"Wien", "Salzburg", "Graz", "Linz", "Innsbruck", "Klagenfurt",
            "Villach", "Wels", "St. Pölten", "Dornbirn"};

        return streets[random.nextInt(streets.length)]
            + " "
            + (random.nextInt(200) + 1)
            + ", "
            + (1000 + random.nextInt(9000))
            + " "
            + cities[random.nextInt(cities.length)];
    }

    // ==================== LOCATION GENERATION ====================

    private List<Location> generateLargeLocationSet() {
        LOGGER.info("Generating {} locations...", TARGET_LOCATIONS);
        List<Location> locations = new ArrayList<>();

        String[][] locationData = {
            {"Wiener Stadthalle", "Vienna", "1140", "Roland Rainer Platz", "1", "CENTER", "CONCERT"},
            {"Ernst Happel Stadion", "Vienna", "1020", "Meiereistraße", "7", "CENTER", "CONCERT"},
            {"Wiener Staatsoper", "Vienna", "1010", "Opernring", "2", "BOTTOM", "OPERA"},
            {"Volksoper Wien", "Vienna", "1090", "Währinger Straße", "78", "BOTTOM", "THEATER"},
            {"Burgtheater", "Vienna", "1010", "Universitätsring", "2", "BOTTOM", "THEATER"},
            {"Gasometer", "Vienna", "1110", "Guglgasse", "6", "CENTER", "CONCERT"},
            {"Planet.tt Bank Austria Halle", "Vienna", "1150", "Engelhartszellergasse", "6-8", "CENTER", "CONCERT"},
            {"Wiener Konzerthaus", "Vienna", "1030", "Lothringerstraße", "20", "BOTTOM", "CONCERT"},
            {"Stadthalle Graz", "Graz", "8010", "Messeplatz", "1", "CENTER", "CONCERT"},
            {"Opernhaus Graz", "Graz", "8010", "Kaiser-Josef-Platz", "10", "BOTTOM", "OPERA"},
            {"Messehalle Salzburg", "Salzburg", "5020", "Am Messezentrum", "1", "CENTER", "CONCERT"},
            {"Landestheater Salzburg", "Salzburg", "5020", "Schwarzstraße", "22", "BOTTOM", "THEATER"},
            {"Olympiahalle Innsbruck", "Innsbruck", "6020", "Olympiastraße", "10", "CENTER", "CONCERT"},
            {"Tiroler Landestheater", "Innsbruck", "6020", "Rennweg", "2", "BOTTOM", "THEATER"},
            {"Design Center Linz", "Linz", "4020", "Europaplatz", "1", "CENTER", "CONCERT"},
            {"Landestheater Linz", "Linz", "4020", "Promenade", "39", "BOTTOM", "THEATER"},
            {"Wörthersee Stadion", "Klagenfurt", "9020", "Wörthersee", "1", "CENTER", "CONCERT"},
            {"Stadthalle Villach", "Villach", "9500", "Ossiacher Zeile", "1", "CENTER", "CONCERT"},
            {"Stadttheater Klagenfurt", "Klagenfurt", "9020", "Theaterplatz", "4", "BOTTOM", "THEATER"},
            {"Messezentrum Wels", "Wels", "4600", "Messeplatz", "1", "CENTER", "CONCERT"},
            {"Stadttheater St. Pölten", "St. Pölten", "3100", "Rathausplatz", "11", "BOTTOM", "THEATER"},
            {"Festspielhaus Bregenz", "Bregenz", "6900", "Platz der Wiener Symphoniker", "1", "BOTTOM", "THEATER"},
            {"Stadthalle Dornbirn", "Dornbirn", "6850", "Messestraße", "4", "CENTER", "CONCERT"},
            {"Eishalle Feldkirch", "Feldkirch", "6800", "Montfortstraße", "2", "CENTER", "CONCERT"},
            {"Kulturzentrum Eisenstadt", "Eisenstadt", "7000", "Schloss Esterhazy", "1", "BOTTOM", "CONCERT"}
        };

        for (int i = 0; i < Math.min(TARGET_LOCATIONS, locationData.length); i++) {
            String[] data = locationData[i];
            Location location = new Location(
                Integer.parseInt(data[2]),
                data[1],
                data[3],
                data[4]
            );
            location.setName(data[0]);
            location.setStagePosition(data[5]);
            location.setStageLabel(data[6]);

            configureLargeStage(location, i % 4);

            Location saved = locationRepository.save(location);
            locations.add(saved);
        }

        // Generate additional random locations if needed
        for (int i = locationData.length; i < TARGET_LOCATIONS; i++) {
            Location location = generateRandomLocation();
            Location saved = locationRepository.save(location);
            locations.add(saved);
        }

        LOGGER.info("Generated {} locations", locations.size());
        return locations;
    }

    private void configureLargeStage(Location loc, int variant) {
        loc.setStageHeightPx(150 + random.nextInt(100));
        loc.setStageWidthPx(400 + random.nextInt(400));

        switch (variant) {
            case 0 -> loc.setStagePosition("BOTTOM");
            case 1 -> loc.setStagePosition("CENTER");
            case 2 -> {
                loc.setStagePosition("CENTER");
                loc.setRunwayWidthPx(50 + random.nextInt(50));
                loc.setRunwayLengthPx(200 + random.nextInt(100));
            }
            case 3 -> loc.setStagePosition("TOP");
            default -> loc.setStagePosition("BOTTOM");
        }
    }

    private Location generateRandomLocation() {
        String[] cities = {"Wien", "Graz", "Linz", "Salzburg", "Innsbruck", "Klagenfurt",
            "Villach", "Wels", "St. Pölten", "Dornbirn", "Eisenstadt", "Bregenz"};
        String[] venueTypes = {"Halle", "Theater", "Oper", "Konzerthaus", "Stadion", "Arena",
            "Park", "Open Air", "Club", "Bar"};
        String[] names = {"City", "Royal", "Grand", "Imperial", "National", "Municipal",
            "Community", "Cultural", "Event", "Performance"};

        String city = cities[random.nextInt(cities.length)];
        String venueType = venueTypes[random.nextInt(venueTypes.length)];
        String name = names[random.nextInt(names.length)] + " " + venueType + " " + city;

        Location location = new Location(
            1000 + random.nextInt(9000),
            city,
            "Musterstraße",
            String.valueOf(random.nextInt(200) + 1)
        );
        location.setName(name);
        location.setStagePosition(random.nextBoolean() ? "BOTTOM" : "CENTER");
        location.setStageLabel(venueType.toUpperCase());

        return location;
    }

    // ==================== ARTIST GENERATION ====================

    private List<Artist> generateLargeArtistSet() {
        LOGGER.info("Generating artists...");
        List<Artist> artists = new ArrayList<>();

        String[] internationalArtists = {
            "The Rolling Stones", "U2", "Coldplay", "Ed Sheeran", "Taylor Swift",
            "Beyoncé", "Lady Gaga", "Bruno Mars", "Adele", "Rihanna",
            "Drake", "Post Malone", "Billie Eilish", "The Weeknd", "Dua Lipa",
            "Harry Styles", "Olivia Rodrigo", "Bad Bunny", "Kendrick Lamar",
            "Metallica", "Foo Fighters", "Red Hot Chili Peppers", "Green Day",
            "Arctic Monkeys", "Imagine Dragons", "Maroon 5", "One Direction",
            "Justin Bieber", "Shawn Mendes", "Ariana Grande"
        };

        // Generate all artists
        for (String name : internationalArtists) {
            Artist artist = new Artist(name);
            artist.setIsBand(name.startsWith("The ")
                || name.contains(" and ")
                || name.contains(" & ")
                || random.nextDouble() < 0.3);
            artists.add(artistRepository.save(artist));
        }

        String[] germanArtists = {
            "Rammstein", "Die Toten Hosen", "Die Ärzte", "Helene Fischer",
            "Andreas Gabalier", "Apache 207", "Bushido", "Marteria",
            "Kraftklub", "Wir sind Helden", "Silbermond", "Fettes Brot",
            "Die Fantastischen Vier", "Peter Fox", "Udo Jürgens",
            "Rainhard Fendrich", "Wolfgang Ambros", "STS", "Hubert von Goisern",
            "Christina Stürmer", "Seiler und Speer", "Wanda", "Bilderbuch",
            "Kreisky", "Pizzera & Jaus", "Voodoo Jürgens"
        };

        for (String name : germanArtists) {
            Artist artist = new Artist(name);
            artist.setIsBand(name.startsWith("Die ")
                || name.contains(" und ")
                || random.nextDouble() < 0.4);
            artists.add(artistRepository.save(artist));
        }

        String[] classicalArtists = {
            "Wiener Philharmoniker", "Berliner Philharmoniker", "London Symphony Orchestra",
            "New York Philharmonic", "Chicago Symphony Orchestra", "Boston Symphony Orchestra",
            "Cleveland Orchestra", "Philadelphia Orchestra", "Los Angeles Philharmonic",
            "San Francisco Symphony", "Royal Concertgebouw Orchestra", "Staatskapelle Dresden",
            "Gewandhausorchester Leipzig", "Symphonieorchester des Bayerischen Rundfunks",
            "NDR Elbphilharmonie Orchester", "SWR Symphonieorchester", "WDR Sinfonieorchester",
            "Orchestre de Paris", "Orchestre National de France", "Accademia Nazionale di Santa Cecilia",
            "Orchestra del Teatro alla Scala", "Wiener Symphoniker", "Bamberger Symphoniker",
            "Münchner Philharmoniker", "Stuttgarter Philharmoniker"
        };

        for (String name : classicalArtists) {
            Artist artist = new Artist(name);
            artist.setIsBand(true); // Orchestras are treated as bands
            artists.add(artistRepository.save(artist));
        }

        String[] theaterArtists = {
            "Wiener Burgtheater Ensemble", "Salzburger Festspiele Ensemble",
            "Bregenzer Festspiele Ensemble", "Münchner Kammerspiele",
            "Deutsches Theater Berlin", "Schauspielhaus Zürich",
            "Thalia Theater Hamburg", "Schauspiel Frankfurt",
            "Düsseldorfer Schauspielhaus", "Staatsschauspiel Dresden",
            "Schauspiel Stuttgart", "Theater Basel", "Volkstheater Wien",
            "Theater in der Josefstadt", "Schauspielhaus Graz",
            "Landestheater Linz", "Tiroler Landestheater",
            "Vorarlberger Landestheater", "Stadttheater Klagenfurt",
            "Landestheater Salzburg"
        };

        for (String name : theaterArtists) {
            Artist artist = new Artist(name);
            artist.setIsBand(true); // Ensembles are treated as bands
            artists.add(artistRepository.save(artist));
        }

        LOGGER.info("Generated {} artists", artists.size());
        return artists;
    }

    // ==================== EVENT GENERATION ====================

    private List<Event> generateLargeEventSet(List<Location> locations, List<Artist> artists) {
        LOGGER.info("Generating {} events...", TARGET_EVENTS);
        List<Event> events = new ArrayList<>();

        LocalDate startDate = LocalDate.now().plusDays(180);
        LocalDate endDate = LocalDate.now().plusDays(365);

        String[][] eventTemplates = {
            {"{artist} Live in {city}", "CONCERT", "Experience {artist} live in concert!"},
            {"{artist} - {tour} Tour", "CONCERT", "The spectacular {tour} tour by {artist}!"},
            {"{artist} Unplugged", "CONCERT", "Intimate acoustic performance by {artist}"},
            {"{artist} - Greatest Hits", "CONCERT", "All the hits from {artist}'s career!"},
            {"{city} Music Festival", "FESTIVAL", "Multi-day festival featuring top artists"},
            {"{season} Festival {city}", "FESTIVAL", "Celebrate {season} with amazing music"},
            {"{genre} Festival", "FESTIVAL", "Dedicated to the best in {genre} music"},
            {"{play} - The Play", "THEATER", "Award-winning production of {play}"},
            {"{play} - Limited Run", "THEATER", "Special limited engagement of {play}"},
            {"{play} - Premiere", "THEATER", "World premiere of the new play {play}"},
            {"{opera} - The Opera", "OPERA", "Classic opera {opera} in a new production"},
            {"{opera} - Gala Performance", "OPERA", "Special gala performance of {opera}"},
            {"{composer}'s {opera}", "OPERA", "{composer}'s masterpiece {opera}"},
            {"{musical} - The Musical", "MUSICAL", "Spectacular production of {musical}"},
            {"{musical} - New Production", "MUSICAL", "Fresh new take on {musical}"},
            {"{comedian} Live", "COMEDY", "Hilarious stand-up comedy by {comedian}"},
            {"{comedian} - New Material", "COMEDY", "Brand new material from {comedian}"},
            {"{dance} - Dance Performance", "DANCE", "Breathtaking {dance} performance"},
            {"{company} Dance Company", "DANCE", "World-renowned {company} in performance"}
        };

        String[] tours = {"World", "European", "Summer", "Winter", "Anniversary",
            "Reunion", "Farewell", "Comeback", "Victory"};
        String[] plays = {"Hamlet", "Macbeth", "Romeo and Juliet", "A Midsummer Night's Dream",
            "The Tempest", "Othello", "King Lear", "The Cherry Orchard",
            "Death of a Salesman", "A Streetcar Named Desire", "The Glass Menagerie",
            "Long Day's Journey Into Night", "Waiting for Godot", "Endgame"};
        String[] operas = {"La Traviata", "Carmen", "The Magic Flute", "The Barber of Seville",
            "Tosca", "Madama Butterfly", "Aida", "Rigoletto", "Don Giovanni",
            "The Marriage of Figaro", "Der Rosenkavalier", "Salome", "Elektra"};
        String[] musicals = {"The Phantom of the Opera", "Les Misérables", "Cats", "Miss Saigon",
            "Chicago", "Wicked", "The Lion King", "Mamma Mia!", "Hamilton",
            "Dear Evan Hansen", "The Book of Mormon", "Come From Away"};
        String[] comedians = {"Trevor Noah", "Dave Chappelle", "Jerry Seinfeld", "Kevin Hart",
            "Ricky Gervais", "John Mulaney", "Ali Wong", "Hannah Gadsby",
            "Bill Burr", "Louis C.K.", "Sarah Silverman", "Chris Rock"};
        String[] danceStyles = {"Ballet", "Contemporary", "Modern", "Jazz", "Tap",
            "Hip Hop", "Flamenco", "Swing", "Salsa", "Tango"};
        String[] seasons = {"Spring", "Summer", "Autumn", "Winter"};
        String[] genres = {"Rock", "Pop", "Jazz", "Classical", "Electronic", "Hip Hop",
            "Country", "Reggae", "Metal", "Indie", "Alternative", "Folk"};
        String[] composers = {"Mozart", "Beethoven", "Verdi", "Puccini", "Wagner",
            "Bach", "Handel", "Tchaikovsky", "Brahms", "Mahler"};

        for (int i = 0; i < TARGET_EVENTS; i++) {
            Location location = locations.get(random.nextInt(locations.size()));
            String city = location.getCity();

            String[] template = eventTemplates[random.nextInt(eventTemplates.length)];
            String title = template[0]
                .replace("{city}", city)
                .replace("{artist}", "Artist" + (i % 50 + 1))
                .replace("{tour}", tours[random.nextInt(tours.length)])
                .replace("{play}", plays[random.nextInt(plays.length)])
                .replace("{opera}", operas[random.nextInt(operas.length)])
                .replace("{musical}", musicals[random.nextInt(musicals.length)])
                .replace("{comedian}", comedians[random.nextInt(comedians.length)])
                .replace("{dance}", danceStyles[random.nextInt(danceStyles.length)])
                .replace("{season}", seasons[random.nextInt(seasons.length)])
                .replace("{genre}", genres[random.nextInt(genres.length)])
                .replace("{composer}", composers[random.nextInt(composers.length)])
                .replace("{company}", city + " Dance");

            String description = template[2]
                .replace("{artist}", "Artist" + (i % 50 + 1))
                .replace("{tour}", tours[random.nextInt(tours.length)])
                .replace("{play}", plays[random.nextInt(plays.length)])
                .replace("{opera}", operas[random.nextInt(operas.length)])
                .replace("{musical}", musicals[random.nextInt(musicals.length)])
                .replace("{season}", seasons[random.nextInt(seasons.length)])
                .replace("{genre}", genres[random.nextInt(genres.length)])
                .replace("{composer}", composers[random.nextInt(composers.length)]);

            Event event = new Event();
            event.setTitle(title + " #" + (i + 1));
            event.setType(template[1]);
            event.setDurationMinutes(90 + random.nextInt(150)); // 90-240 minutes
            event.setDescription(description);

            // Random date within range
            long daysBetween = startDate.until(endDate).getDays();
            LocalDate eventDate = startDate.plusDays(random.nextInt((int) daysBetween));
            int hour = 18 + random.nextInt(5); // 18:00 - 22:59
            int minute = random.nextInt(60);
            event.setDateTime(LocalDateTime.of(eventDate, java.time.LocalTime.of(hour, minute)));

            event.setLocation(location);

            // Assign 1-3 random artists
            List<Artist> eventArtists = new ArrayList<>();
            int artistCount = 1 + random.nextInt(3);
            for (int j = 0; j < artistCount; j++) {
                Artist artist = artists.get(random.nextInt(artists.size()));
                if (!eventArtists.contains(artist)) {
                    eventArtists.add(artist);
                }
            }
            event.setArtists(eventArtists);

            events.add(eventRepository.save(event));

            if ((i + 1) % 50 == 0) {
                LOGGER.info("Generated {} events...", i + 1);
            }
        }

        LOGGER.info("Generated {} events", events.size());
        return events;
    }

    // ==================== SEAT GENERATION ====================

    private List<Sector> generateSeatsForAllLocations(List<Location> locations) {
        LOGGER.info("Generating seats for all locations...");
        List<Sector> allSectors = new ArrayList<>();

        for (Location location : locations) {
            // Generate sectors for each location (5 per location)
            for (int s = 0; s < 5; s++) {
                Sector sector = new Sector();
                sector.setName("Sector " + (char) ('A' + s));
                sector.setLocation(location);
                sector = sectorRepository.save(sector);
                allSectors.add(sector);

                // Create price category for this sector
                PriceCategory priceCategory = new PriceCategory();
                priceCategory.setDescription("Category " + (s + 1));
                priceCategory.setBasePrice(2000 + (s * 1000)); // €20, €30, €40, €50, €60
                priceCategory.setSector(sector);
                priceCategoryRepository.save(priceCategory);

                // Generate seats for this sector (50-200 seats per sector)
                int seatCount = 50 + random.nextInt(151); // 50-200 seats
                generateSeatsForSector(sector, priceCategory, seatCount);
            }
        }

        LOGGER.info("Seat generation completed - created {} sectors", allSectors.size());
        return allSectors;
    }

    private void generateSeatsForSector(Sector sector, PriceCategory priceCategory, int seatCount) {
        int rows = Math.min(20, (int) Math.sqrt(seatCount)); // Max 20 rows
        int seatsPerRow = seatCount / rows;

        for (int row = 1; row <= rows; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setSeatNumber(seatNum);
                seat.setSector(sector);
                seat.setPriceCategory(priceCategory);
                seatRepository.save(seat);
            }
        }
    }

    // ==================== RESERVATION GENERATION ====================

    // ==================== RESERVATION GENERATION ====================

    private List<Reservation> generateReservations(List<User> users, List<Event> events, List<Sector> sectors) {
        LOGGER.info("Generating {} reservations...", TARGET_RESERVATIONS);
        List<Reservation> reservations = new ArrayList<>();

        // Map um zu tracken, welche Sitzplätze bereits für welche Events verwendet wurden
        Map<Long, Set<Long>> usedSeatsByEvent = new HashMap<>();

        for (int i = 0; i < TARGET_RESERVATIONS; i++) {
            User user = users.get(random.nextInt(users.size()));
            Event event = events.get(random.nextInt(events.size()));
            Long eventId = event.getId();

            // Find available seats for this event's location
            Location location = event.getLocation();
            List<Sector> locationSectors = sectors.stream()
                .filter(s -> s.getLocation().getId().equals(location.getId()))
                .toList();

            if (!locationSectors.isEmpty()) {
                Sector sector = locationSectors.get(random.nextInt(locationSectors.size()));
                List<Seat> seats = seatRepository.findBySectorId(sector.getId());

                // Filter out seats already used for this event
                Set<Long> usedSeats = usedSeatsByEvent.getOrDefault(eventId, new HashSet<>());
                List<Seat> availableSeats = seats.stream()
                    .filter(seat -> !usedSeats.contains(seat.getId()))
                    .toList();

                if (!availableSeats.isEmpty()) {
                    // Create reservation
                    Reservation reservation = new Reservation();
                    reservation.setUser(user);
                    reservation.setEvent(event);
                    reservation.setReservationNumber("RES-" + System.currentTimeMillis() + "-" + i);
                    reservation = reservationRepository.save(reservation);

                    // Create 1-4 tickets for this reservation
                    int ticketCount = Math.min(1 + random.nextInt(4), availableSeats.size());

                    // Shuffle to get random seats
                    List<Seat> shuffledSeats = new ArrayList<>(availableSeats);
                    Collections.shuffle(shuffledSeats, random);

                    for (int t = 0; t < ticketCount; t++) {
                        Seat seat = shuffledSeats.get(t);

                        // Mark seat as used for this event
                        usedSeats.add(seat.getId());
                        usedSeatsByEvent.put(eventId, usedSeats);

                        // Calculate price
                        PriceCategory pc = seat.getPriceCategory();
                        int basePrice = pc != null ? pc.getBasePrice() : 2000;
                        double priceMultiplier = 0.8 + random.nextDouble() * 0.4;
                        double netPrice = (basePrice / 100.0) * priceMultiplier;
                        double taxRate = 0.20;
                        double grossPrice = netPrice * (1.0 + taxRate);

                        // Create ticket with reservation (NO invoice!)
                        Ticket ticket = new Ticket(seat, event);
                        ticket.setNetPrice(round2(netPrice));
                        ticket.setTaxRate(taxRate);
                        ticket.setGrossPrice(round2(grossPrice));
                        ticket.setReservation(reservation);
                        ticketRepository.save(ticket);
                    }

                    reservations.add(reservation);

                    if ((i + 1) % 50 == 0) {
                        LOGGER.info("Generated {} reservations...", i + 1);
                    }
                } else {
                    // No available seats for this event, skip and try next reservation
                    i--;
                }
            }
        }

        LOGGER.info("Generated {} reservations", reservations.size());
        return reservations;
    }


    // ==================== INVOICE GENERATION ====================

    private void generateInvoices(List<User> users, List<Event> events, List<Sector> sectors, List<Merchandise> merchandise) {
        LOGGER.info("Generating {} invoices...", TARGET_INVOICES);

        // Map um zu tracken, welche Sitzplätze bereits für welche Events verwendet wurden
        Map<Long, Set<Long>> usedSeatsByEvent = new HashMap<>();

        // First, collect already used seats from existing tickets (from reservations)
        List<Ticket> existingTickets = ticketRepository.findAll();
        for (Ticket ticket : existingTickets) {
            Long eventId = ticket.getEvent().getId();
            Long seatId = ticket.getSeat().getId();
            usedSeatsByEvent.computeIfAbsent(eventId, k -> new HashSet<>()).add(seatId);
        }

        for (int i = 0; i < TARGET_INVOICES; i++) {
            User user = users.get(random.nextInt(users.size()));

            // Wähle ein Event mit verfügbaren Plätzen
            Event event = null;
            int attempts = 0;
            while (event == null && attempts < 10) {
                Event candidate = events.get(random.nextInt(events.size()));
                Long eventId = candidate.getId();

                // Prüfe ob es noch verfügbare Plätze für dieses Event gibt
                Location location = candidate.getLocation();
                List<Sector> locationSectors = sectors.stream()
                    .filter(s -> s.getLocation().getId().equals(location.getId()))
                    .toList();

                boolean hasAvailableSeats = false;
                for (Sector sector : locationSectors) {
                    List<Seat> seats = seatRepository.findBySectorId(sector.getId());
                    Set<Long> usedSeats = usedSeatsByEvent.getOrDefault(eventId, new HashSet<>());
                    long availableCount = seats.stream()
                        .filter(seat -> !usedSeats.contains(seat.getId()))
                        .count();

                    if (availableCount > 0) {
                        hasAvailableSeats = true;
                        break;
                    }
                }

                if (hasAvailableSeats) {
                    event = candidate;
                } else {
                    attempts++;
                }
            }

            if (event == null) {
                // Kein Event mit verfügbaren Plätzen gefunden, überspringen
                LOGGER.warn("Could not find event with available seats, skipping invoice");
                continue;
            }

            // Create invoice
            Invoice invoice = new Invoice();
            invoice.setUser(user);
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis() + "-" + i);
            invoice.setInvoiceDate(LocalDate.now().minusDays(random.nextInt(180)));
            invoice.setEventDate(event.getDateTime()); // SETZE eventDate VOR dem Speichern!
            invoice = invoiceRepository.save(invoice);

            double netTotal = 0;
            double taxTotal = 0;
            double grossTotal = 0;

            // Add 1-4 purchased tickets to invoice
            int ticketCount = 1 + random.nextInt(4);
            int ticketsCreated = 0;

            // Find available seats for this event's location
            Location location = event.getLocation();
            List<Sector> locationSectors = sectors.stream()
                .filter(s -> s.getLocation().getId().equals(location.getId()))
                .toList();

            if (!locationSectors.isEmpty()) {
                Long eventId = event.getId();

                // Try different sectors until we find enough seats
                for (int t = 0; t < ticketCount && ticketsCreated < ticketCount; t++) {
                    Sector sector = locationSectors.get(random.nextInt(locationSectors.size()));
                    List<Seat> seats = seatRepository.findBySectorId(sector.getId());

                    // Filter out seats already used for this event
                    Set<Long> usedSeats = usedSeatsByEvent.getOrDefault(eventId, new HashSet<>());
                    List<Seat> availableSeats = seats.stream()
                        .filter(seat -> !usedSeats.contains(seat.getId()))
                        .toList();

                    if (!availableSeats.isEmpty()) {
                        // Take a random available seat
                        Seat seat = availableSeats.get(random.nextInt(availableSeats.size()));

                        // Mark seat as used for this event
                        usedSeats.add(seat.getId());
                        usedSeatsByEvent.put(eventId, usedSeats);

                        // Calculate price
                        PriceCategory pc = seat.getPriceCategory();
                        int basePrice = pc != null ? pc.getBasePrice() : 2000;
                        double priceMultiplier = 0.8 + random.nextDouble() * 0.4;
                        double netPrice = (basePrice / 100.0) * priceMultiplier;
                        double taxRate = 0.20;
                        double grossPrice = netPrice * (1.0 + taxRate);

                        // Create ticket WITH invoice (purchased ticket)
                        Ticket ticket = new Ticket(seat, event);
                        ticket.setNetPrice(round2(netPrice));
                        ticket.setTaxRate(taxRate);
                        ticket.setGrossPrice(round2(grossPrice));
                        ticket.setInvoice(invoice);
                        ticketRepository.save(ticket);

                        netTotal += netPrice;
                        taxTotal += netPrice * taxRate;
                        grossTotal += grossPrice;
                        ticketsCreated++;
                    }
                }
            }

            // If we couldn't create any tickets, skip this invoice
            if (ticketsCreated == 0) {
                invoiceRepository.delete(invoice);
                i--; // Retry this invoice
                continue;
            }

            // 30% chance to add merchandise to invoice
            if (random.nextDouble() < 0.3 && !merchandise.isEmpty()) {
                int merchItems = 1 + random.nextInt(2);
                for (int m = 0; m < merchItems; m++) {
                    Merchandise merch = merchandise.get(random.nextInt(merchandise.size()));
                    int quantity = 1 + random.nextInt(2);

                    InvoiceMerchandiseItem item = new InvoiceMerchandiseItem(invoice, merch, quantity);
                    invoiceMerchandiseItemRepository.save(item);

                    double merchNet = merch.getUnitPrice().doubleValue() * quantity;
                    netTotal += merchNet;
                    taxTotal += merchNet * 0.20;
                    grossTotal += merchNet * 1.20;
                }
            }

            // Set invoice totals
            invoice.setNetTotal(round2(netTotal));
            invoice.setTaxTotal(round2(taxTotal)); // Korrekte Methode: setTaxTotal, nicht setTaxRate
            invoice.setGrossTotal(round2(grossTotal));

            // Invoice bereits gespeichert, nur Totals aktualisieren
            invoiceRepository.save(invoice);

            // Update user stats
            user.setTotalCentsSpent(user.getTotalCentsSpent() + (int) (grossTotal * 100));
            user.setRewardPoints(user.getRewardPoints() + (int) (grossTotal * 0.1));
            userRepository.save(user);

            if ((i + 1) % 50 == 0) {
                LOGGER.info("Generated {} invoices...", i + 1);
            }
        }

        LOGGER.info("Generated {} invoices", TARGET_INVOICES);
    }
    // ==================== MERCHANDISE GENERATION ====================

    private List<Merchandise> generateMerchandiseItems() {
        LOGGER.info("Generating merchandise items...");
        List<Merchandise> items = new ArrayList<>();

        Object[][] merchData = {
            {"Basic T-Shirt", "Comfortable cotton t-shirt", new BigDecimal("24.99"), 1000, 10, false},
            {"Premium Hoodie", "Soft fleece hoodie with embroidered logo", new BigDecimal("59.99"), 500, 25, false},
            {"Event Poster", "Limited edition event poster", new BigDecimal("14.99"), 2000, 5, false},
            {"Baseball Cap", "Adjustable cap with team logo", new BigDecimal("29.99"), 800, 15, false},
            {"Water Bottle", "Insulated stainless steel bottle", new BigDecimal("34.99"), 400, 20, false},
            {"Tote Bag", "Reusable canvas tote bag", new BigDecimal("19.99"), 600, 8, false},
            {"Phone Case", "Protective phone case with design", new BigDecimal("24.99"), 300, 12, false},
            {"Keychain", "Metal keychain with logo", new BigDecimal("9.99"), 1500, 3, false},
            {"Sticker Pack", "Set of 10 vinyl stickers", new BigDecimal("7.99"), 2500, 2, false},
            {"Backpack", "Durable backpack for everyday use", new BigDecimal("49.99"), 200, 30, false}
        };

        for (Object[] data : merchData) {
            Merchandise merch = new Merchandise();
            merch.setName((String) data[0]);
            merch.setDescription((String) data[1]);
            merch.setUnitPrice((BigDecimal) data[2]);
            merch.setRemainingQuantity((Integer) data[3]);
            merch.setRewardPointsPerUnit((Integer) data[4]);
            merch.setRedeemableWithPoints((Boolean) data[5]);

            // Make some items redeemable with points
            if (random.nextDouble() < 0.3) {
                merch.setRedeemableWithPoints(true);
                merch.setPointsPrice((Integer) data[4] * 10);
            }

            items.add(merchandiseRepository.save(merch));
        }

        LOGGER.info("Generated {} merchandise items", items.size());
        return items;
    }


    // ==================== NEWS GENERATION ====================

    private void generateNewsItems() {
        LOGGER.info("Generating news items...");

        String[][] newsTemplates = {
            {"New Event Announcement", "Exciting new events have been added to our lineup!"},
            {"Special Promotion", "Limited time offer on selected events!"},
            {"Venue Updates", "Important updates about our venues"},
            {"Artist Spotlight", "Featured artist of the month"},
            {"Seasonal Events", "Get ready for seasonal celebrations"},
            {"VIP Packages", "Exclusive VIP experiences now available"},
            {"Mobile App Update", "Enhanced features in our mobile app"},
            {"Sustainability Initiative", "Our commitment to the environment"},
            {"Community Events", "Supporting local communities"},
            {"Customer Appreciation", "Thank you to our loyal customers"}
        };

        LocalDate startDate = LocalDate.now().minusDays(60);

        for (int i = 0; i < 10; i++) {
            String[] template = newsTemplates[random.nextInt(newsTemplates.length)];

            NewsItem news = new NewsItem();
            news.setTitle(template[0] + " #" + (i + 1));
            news.setSummary(template[1]);
            news.setText("This is news item number " + (i + 1) + " with detailed information.");

            // Random publish date within last 60 days
            long daysAgo = random.nextInt(61);
            news.setPublishedAt(startDate.plusDays(daysAgo));

            newsItemRepository.save(news);
        }

        LOGGER.info("Generated 10 news items");
    }

    // ==================== HELPER METHODS ====================

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}