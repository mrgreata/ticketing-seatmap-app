package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Merchandise;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeenNews;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.MerchandiseRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PriceCategoryRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeenNewsItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.type.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Component
@Profile("e2e")
public class E2eTestDataGenerator implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;
    private final LocationRepository locationRepository;
    private final SectorRepository sectorRepository;
    private final PriceCategoryRepository priceCategoryRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationRepository reservationRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final NewsItemRepository newsItemRepository;
    private final SeenNewsItemRepository seenNewsItemRepository;

    public E2eTestDataGenerator(
        UserRepository userRepository,
        EventRepository eventRepository,
        ArtistRepository artistRepository,
        LocationRepository locationRepository,
        SectorRepository sectorRepository,
        PriceCategoryRepository priceCategoryRepository,
        SeatRepository seatRepository,
        TicketRepository ticketRepository,
        InvoiceRepository invoiceRepository,
        PasswordEncoder passwordEncoder,
        ReservationRepository reservationRepository,
        MerchandiseRepository merchandiseRepository,
        NewsItemRepository newsItemRepository,
        SeenNewsItemRepository seenNewsItemRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
        this.locationRepository = locationRepository;
        this.sectorRepository = sectorRepository;
        this.priceCategoryRepository = priceCategoryRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.invoiceRepository = invoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.reservationRepository = reservationRepository;
        this.merchandiseRepository = merchandiseRepository;
        this.newsItemRepository = newsItemRepository;
        this.seenNewsItemRepository = seenNewsItemRepository;
    }


    @Transactional
    @Override
    public void run(ApplicationArguments args) {

        // ===== ADMIN =====
        User admin = new User();
        admin.setEmail("admin@email.com");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setUserRole(UserRole.ROLE_ADMIN);
        admin = userRepository.save(admin);

        // ===== USER =====
        User user5 = new User();
        user5.setEmail("user5@email.com");
        user5.setPasswordHash(passwordEncoder.encode("password"));
        user5.setFirstName("Max");
        user5.setLastName("Mustermann");
        user5.setUserRole(UserRole.ROLE_USER);
        user5.setTotalCentsSpent(6000);
        user5.setRewardPoints(5000);
        user5 = userRepository.save(user5);


        User user4 = new User();
        user4.setEmail("user4@email.com");
        user4.setPasswordHash(passwordEncoder.encode("password"));
        user4.setFirstName("Alice");
        user4.setLastName("Tester");
        user4.setUserRole(UserRole.ROLE_USER);
        user4 = userRepository.save(user4);

        User userForPurchaseTest = new User();
        userForPurchaseTest.setEmail("userForPurchaseTest@email.com");
        userForPurchaseTest.setPasswordHash(passwordEncoder.encode("password"));
        userForPurchaseTest.setFirstName("Purchase");
        userForPurchaseTest.setLastName("Test");
        userForPurchaseTest.setUserRole(UserRole.ROLE_USER);
        userForPurchaseTest = userRepository.save(userForPurchaseTest);

        User user6 = new User();
        user6.setEmail("user6@email.com");
        user6.setPasswordHash(passwordEncoder.encode("password"));
        user6.setFirstName("No");
        user6.setLastName("Tickets");
        user6.setUserRole(UserRole.ROLE_USER);
        userRepository.save(user6);

        User userSinglePurchased = new User();
        userSinglePurchased.setEmail("userSinglePurchased@email.com");
        userSinglePurchased.setPasswordHash(passwordEncoder.encode("password"));
        userSinglePurchased.setFirstName("Single");
        userSinglePurchased.setLastName("Ticket");
        userSinglePurchased.setUserRole(UserRole.ROLE_USER);
        userSinglePurchased = userRepository.save(userSinglePurchased);

        User userWithReservation = new User();
        userWithReservation.setEmail("userWithReservation@email.com");
        userWithReservation.setPasswordHash(passwordEncoder.encode("password"));
        userWithReservation.setFirstName("Reserved");
        userWithReservation.setLastName("Only");
        userWithReservation.setUserRole(UserRole.ROLE_USER);
        userWithReservation = userRepository.save(userWithReservation);

        User seatmapTestUser = new User();
        seatmapTestUser.setEmail("seatmapTestUser@email.com");
        seatmapTestUser.setPasswordHash(passwordEncoder.encode("password"));
        seatmapTestUser.setFirstName("Seatmap");
        seatmapTestUser.setLastName("Tester");
        seatmapTestUser.setUserRole(UserRole.ROLE_USER);
        seatmapTestUser = userRepository.save(seatmapTestUser);

        // ===== LOCATION =====
        Location loc = new Location();
        loc.setName("Test Halle");
        loc.setCity("Wien");
        loc.setZipCode(1010);
        loc.setStreet("Teststraße");
        loc.setStreetNumber("1");
        loc.setStagePosition("TOP");
        loc = locationRepository.save(loc);

        Location seatmapLocation = new Location();
        seatmapLocation.setName("Seatmap Test Arena");
        seatmapLocation.setCity("Wien");
        seatmapLocation.setZipCode(1020);
        seatmapLocation.setStreet("Seatmapstraße");
        seatmapLocation.setStreetNumber("42");
        seatmapLocation.setStagePosition("CENTER");
        seatmapLocation = locationRepository.save(seatmapLocation);

        Event pastEvent = new Event();
        pastEvent.setTitle("Vergangenes Konzert");
        pastEvent.setDateTime(LocalDateTime.now().minusDays(10)); // Vor 10 Tagen
        pastEvent.setLocation(loc);
        pastEvent = eventRepository.save(pastEvent);

        Event seatmapEvent = new Event();
        seatmapEvent.setTitle("Seatmap Test Concert");
        seatmapEvent.setDateTime(LocalDateTime.now().plusDays(4)); // In 4 Tagen
        seatmapEvent.setLocation(seatmapLocation);
        seatmapEvent.setDescription("Testveranstaltung für Seatmap-Reservierungen");
        seatmapEvent = eventRepository.save(seatmapEvent);

        Event loneEvent = new Event();
        loneEvent.setTitle("Lone Concert");
        loneEvent.setDateTime(LocalDateTime.now().plusDays(7)); // Zukünftiges Event
        loneEvent.setLocation(loc); // bestehende Location
        loneEvent = eventRepository.save(loneEvent);


        Event reservedOnlyEvent = new Event();
        reservedOnlyEvent.setTitle("Reserved Only Concert");
        reservedOnlyEvent.setDateTime(LocalDateTime.now().plusDays(2)); // In 2 Tagen
        reservedOnlyEvent.setLocation(loc);
        reservedOnlyEvent = eventRepository.save(reservedOnlyEvent);

        // ===== SECTOR =====
        Sector sector = new Sector();
        sector.setName("A");
        sector.setLocation(loc);
        sector = sectorRepository.save(sector);

        Sector seatmapSector = new Sector();
        seatmapSector.setName("Haupttribüne");
        seatmapSector.setLocation(seatmapLocation);
        seatmapSector = sectorRepository.save(seatmapSector);

        // ===== PRICE CATEGORY =====
        PriceCategory pc = new PriceCategory();
        pc.setDescription("Standard");
        pc.setBasePrice(6000);
        pc.setSector(sector);
        pc = priceCategoryRepository.save(pc);

        // ===== EVENTS =====
        Seat pastSeat = new Seat();
        pastSeat.setRowNumber(3);
        pastSeat.setSeatNumber(1);
        pastSeat.setSector(sector);
        pastSeat.setPriceCategory(pc);
        pastSeat = seatRepository.save(pastSeat);

        Seat seatForPurchaseTest = new Seat();
        seatForPurchaseTest.setRowNumber(3);
        seatForPurchaseTest.setSeatNumber(1);
        seatForPurchaseTest.setSector(sector);
        seatForPurchaseTest.setPriceCategory(pc);
        seatForPurchaseTest = seatRepository.save(seatForPurchaseTest);

        Seat loneSeat = new Seat();
        loneSeat.setRowNumber(1);
        loneSeat.setSeatNumber(1);
        loneSeat.setSector(sector);
        loneSeat.setPriceCategory(pc);
        loneSeat = seatRepository.save(loneSeat);

        Seat reservedOnlySeat = new Seat();
        reservedOnlySeat.setRowNumber(5);
        reservedOnlySeat.setSeatNumber(5);
        reservedOnlySeat.setSector(sector);
        reservedOnlySeat.setPriceCategory(pc);
        reservedOnlySeat = seatRepository.save(reservedOnlySeat);

        Reservation singleReservation = new Reservation();
        singleReservation.setUser(userWithReservation);
        singleReservation.setEvent(reservedOnlyEvent);
        singleReservation.setReservationNumber("RES-SINGLE-" + System.currentTimeMillis());
        singleReservation = reservationRepository.save(singleReservation);

        // NEU: Ticket mit Reservation (nicht gekauft!)
        Ticket reservedOnlyTicket = new Ticket();
        reservedOnlyTicket.setEvent(reservedOnlyEvent);
        reservedOnlyTicket.setSeat(reservedOnlySeat);
        reservedOnlyTicket.setNetPrice(4500.0); // Anderer Preis zur Unterscheidung
        reservedOnlyTicket.setTaxRate(0.10);
        reservedOnlyTicket.setGrossPrice(reservedOnlyTicket.getNetPrice() * (1 + reservedOnlyTicket.getTaxRate()));
        reservedOnlyTicket.setLocation(loc);
        reservedOnlyTicket.setSector(sector);
        reservedOnlyTicket.setReservation(singleReservation); // WICHTIG: Nur Reservation, keine Invoice!
        reservedOnlyTicket = ticketRepository.save(reservedOnlyTicket);

        singleReservation.getTickets().add(reservedOnlyTicket);
        reservationRepository.save(singleReservation);

        Invoice pastInvoice = new Invoice();
        pastInvoice.setUser(user5);
        pastInvoice.setInvoiceDate(LocalDate.now().minusDays(9));
        pastInvoice.setInvoiceNumber("INV-PAST-" + System.currentTimeMillis());
        pastInvoice.setEventDate(LocalDateTime.now().minusDays(10));
        pastInvoice.setNetTotal(5000.0);
        pastInvoice.setTaxTotal(500.0);
        pastInvoice.setGrossTotal(5500.0);
        pastInvoice = invoiceRepository.save(pastInvoice);

        Ticket pastTicket = new Ticket();
        pastTicket.setEvent(pastEvent);
        pastTicket.setSeat(pastSeat);
        pastTicket.setNetPrice(5000.00);
        pastTicket.setTaxRate(0.10);
        pastTicket.setGrossPrice(pastTicket.getNetPrice() * (1 + pastTicket.getTaxRate()));
        pastTicket.setLocation(loc);
        pastTicket.setSector(sector);
        pastTicket.setInvoice(pastInvoice);
        pastInvoice.getTickets().add(pastTicket);
        ticketRepository.save(pastTicket);
        invoiceRepository.save(pastInvoice);
        // ===== EVENT =====
        Event cats = new Event();
        cats.setTitle("Cats");
        cats.setDateTime(LocalDateTime.now().plusDays(3));
        cats.setLocation(loc);
        cats = eventRepository.save(cats);

        Event coldplay = new Event();
        coldplay.setTitle("Coldplay Tour");
        coldplay.setDateTime(LocalDateTime.now().plusDays(5));
        coldplay.setLocation(loc);
        coldplay = eventRepository.save(coldplay);

        Event taylorSwift = new Event();
        taylorSwift.setTitle("Taylor Swift");
        taylorSwift.setDateTime(LocalDateTime.now().plusDays(5));
        taylorSwift.setLocation(loc);
        taylorSwift = eventRepository.save(taylorSwift);

        // ===== ARTISTS =====
        Artist taylorSwiftArtist = new Artist();
        taylorSwiftArtist.setName("Taylor Swift");
        taylorSwiftArtist.setIsBand(false);
        taylorSwiftArtist = artistRepository.save(taylorSwiftArtist);

        List<Artist> taylorArtists = new ArrayList<>();
        taylorArtists.add(taylorSwiftArtist);
        taylorSwift.setArtists(taylorArtists);
        taylorSwift = eventRepository.save(taylorSwift);

        Artist coldplayBand = new Artist();
        coldplayBand.setName("Coldplay");
        coldplayBand.setIsBand(true);
        coldplayBand = artistRepository.save(coldplayBand);

        List<Artist> coldplayArtists = new ArrayList<>();
        coldplayArtists.add(coldplayBand);
        coldplay.setArtists(coldplayArtists);
        coldplay = eventRepository.save(coldplay);

        // ===== INVOICE =====
        Invoice invoice = new Invoice();
        invoice.setUser(user5);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setEventDate(LocalDateTime.now());
        invoice.setNetTotal(6000.0);
        invoice.setTaxTotal(600.0);
        invoice.setGrossTotal(6600.0);

        // ===== SEATS =====
        // cats Seats
        Seat catsSeat1 = new Seat();
        catsSeat1.setRowNumber(1);
        catsSeat1.setSeatNumber(1);
        catsSeat1.setSector(sector);
        catsSeat1.setPriceCategory(pc);
        catsSeat1 = seatRepository.save(catsSeat1);

        // Cats: zusätzlicher FREIER Seat
        Seat catsSeat2 = new Seat();
        catsSeat2.setRowNumber(1);
        catsSeat2.setSeatNumber(2);
        catsSeat2.setSector(sector);
        catsSeat2.setPriceCategory(pc);
        seatRepository.save(catsSeat2);

        // Coldplay: zusätzlicher FREIER Seat
        Seat coldplaySeat2 = new Seat();
        coldplaySeat2.setRowNumber(2);
        coldplaySeat2.setSeatNumber(2);
        coldplaySeat2.setSector(sector);
        coldplaySeat2.setPriceCategory(pc);
        seatRepository.save(coldplaySeat2);

        // Taylor Swift: zusätzlicher FREIER Seat
        Seat taylorSeat2 = new Seat();
        taylorSeat2.setRowNumber(2);
        taylorSeat2.setSeatNumber(3);
        taylorSeat2.setSector(sector);
        taylorSeat2.setPriceCategory(pc);
        seatRepository.save(taylorSeat2);




        // Coldplay Seats
        Seat coldplaySeat1 = new Seat();
        coldplaySeat1.setRowNumber(2);
        coldplaySeat1.setSeatNumber(1);
        coldplaySeat1.setSector(sector);
        coldplaySeat1.setPriceCategory(pc);
        coldplaySeat1 = seatRepository.save(coldplaySeat1);

        Seat taylorSwiftSeat1 = new Seat();
        taylorSwiftSeat1.setRowNumber(2);
        taylorSwiftSeat1.setSeatNumber(2);
        taylorSwiftSeat1.setSector(sector);
        taylorSwiftSeat1.setPriceCategory(pc);
        taylorSwiftSeat1 = seatRepository.save(taylorSwiftSeat1);

        Reservation reservationForPurchaseTest = new Reservation();
        reservationForPurchaseTest.setUser(userForPurchaseTest);
        reservationForPurchaseTest.setEvent(taylorSwift); // oder ein anderes Event
        reservationForPurchaseTest.setReservationNumber("RES-PURCHASE-" + System.currentTimeMillis());
        reservationForPurchaseTest = reservationRepository.save(reservationForPurchaseTest);


        Ticket ticketForPurchaseTest = new Ticket();
        ticketForPurchaseTest.setEvent(taylorSwift);
        ticketForPurchaseTest.setSeat(seatForPurchaseTest);
        ticketForPurchaseTest.setNetPrice(5000.0);
        ticketForPurchaseTest.setTaxRate(0.10);
        ticketForPurchaseTest.setGrossPrice(ticketForPurchaseTest.getNetPrice() * (1 + ticketForPurchaseTest.getTaxRate()));
        ticketForPurchaseTest.setLocation(loc);
        ticketForPurchaseTest.setSector(sector);
        ticketForPurchaseTest.setReservation(reservationForPurchaseTest);
        ticketForPurchaseTest = ticketRepository.save(ticketForPurchaseTest);


        reservationForPurchaseTest.getTickets().add(ticketForPurchaseTest);
        reservationRepository.save(reservationForPurchaseTest);
        // ===== TICKETS =====
        // cats - bereits gekauft
        Ticket catsPurchasedTicket = new Ticket();
        catsPurchasedTicket.setEvent(cats);
        catsPurchasedTicket.setSeat(catsSeat1);
        catsPurchasedTicket.setNetPrice(5000.00);
        catsPurchasedTicket.setTaxRate(0.10);
        catsPurchasedTicket.setGrossPrice(catsPurchasedTicket.getNetPrice() * (1 + catsPurchasedTicket.getTaxRate()));
        catsPurchasedTicket.setLocation(loc);
        catsPurchasedTicket.setSector(sector);
        catsPurchasedTicket.setInvoice(invoice);
        invoice.getTickets().add(catsPurchasedTicket);

        Ticket coldplayPurchased = new Ticket();
        coldplayPurchased.setEvent(coldplay);
        coldplayPurchased.setSeat(coldplaySeat1);
        coldplayPurchased.setNetPrice(5000.00);
        coldplayPurchased.setTaxRate(0.10);
        coldplayPurchased.setGrossPrice(coldplayPurchased.getNetPrice() * (1 + coldplayPurchased.getTaxRate()));
        coldplayPurchased.setLocation(loc);
        coldplayPurchased.setSector(sector);
        invoice.getTickets().add(coldplayPurchased);
        coldplayPurchased.setInvoice(invoice);

        invoice = invoiceRepository.save(invoice);


        Reservation reservation = new Reservation();
        reservation.setUser(user4);
        reservation.setEvent(taylorSwift);
        reservation.setReservationNumber("RES-" + System.currentTimeMillis());

        // Reservation speichern
        reservation = reservationRepository.save(reservation);

        // Dann Ticket erstellen (MIT Reservation)
        Ticket taylorSwiftReserved = new Ticket();
        taylorSwiftReserved.setEvent(taylorSwift);
        taylorSwiftReserved.setSeat(taylorSwiftSeat1);
        taylorSwiftReserved.setNetPrice(5000.0);
        taylorSwiftReserved.setTaxRate(0.10);
        taylorSwiftReserved.setGrossPrice(taylorSwiftReserved.getNetPrice() * (1 + taylorSwiftReserved.getTaxRate()));
        taylorSwiftReserved.setLocation(loc);
        taylorSwiftReserved.setSector(sector);
        taylorSwiftReserved.setReservation(reservation); // WICHTIG: Reservation setzen

        taylorSwiftReserved = ticketRepository.save(taylorSwiftReserved);

        reservation.getTickets().add(taylorSwiftReserved);

        reservationRepository.save(reservation);

        Invoice loneInvoice = new Invoice();
        loneInvoice.setUser(userSinglePurchased);
        loneInvoice.setInvoiceDate(LocalDate.now());
        loneInvoice.setInvoiceNumber("INV-LONE-" + System.currentTimeMillis());
        loneInvoice.setEventDate(loneEvent.getDateTime());
        loneInvoice.setNetTotal(5000.0);
        loneInvoice.setTaxTotal(500.0);
        loneInvoice.setGrossTotal(5500.0);
        loneInvoice = invoiceRepository.save(loneInvoice);

        Ticket loneTicket = new Ticket();
        loneTicket.setEvent(loneEvent);
        loneTicket.setSeat(loneSeat);
        loneTicket.setNetPrice(5000.0);
        loneTicket.setTaxRate(0.10);
        loneTicket.setGrossPrice(loneTicket.getNetPrice() * 1.10);
        loneTicket.setLocation(loc);
        loneTicket.setSector(sector);
        loneTicket.setInvoice(loneInvoice);
        loneTicket = ticketRepository.save(loneTicket);

        loneInvoice.getTickets().add(loneTicket);
        invoiceRepository.save(loneInvoice);

        // ===== MERCHANDISE =====
        Merchandise catsShirt = new Merchandise();
        catsShirt.setName("Cats Shirt");
        catsShirt.setDescription("E2E test item - Cats Shirt");
        catsShirt.setUnitPrice(new BigDecimal("19.90"));
        catsShirt.setRewardPointsPerUnit(10);
        catsShirt.setRemainingQuantity(100);
        catsShirt.setRedeemableWithPoints(false);
        catsShirt.setPointsPrice(null);
        catsShirt.setDeleted(false);
        catsShirt = merchandiseRepository.save(catsShirt);

        Merchandise coldplayPoster = new Merchandise();
        coldplayPoster.setName("Coldplay Poster");
        coldplayPoster.setDescription("E2E test item - Coldplay Poster");
        coldplayPoster.setUnitPrice(new BigDecimal("9.90"));
        coldplayPoster.setRewardPointsPerUnit(5);
        coldplayPoster.setRemainingQuantity(100);
        coldplayPoster.setRedeemableWithPoints(false);
        coldplayPoster.setPointsPrice(null);
        coldplayPoster.setDeleted(false);
        merchandiseRepository.save(coldplayPoster);

        Merchandise vipVoucher = new Merchandise();
        vipVoucher.setName("VIP Voucher");
        vipVoucher.setDescription("E2E reward item - VIP Voucher");
        vipVoucher.setUnitPrice(new BigDecimal("0.00"));
        vipVoucher.setRewardPointsPerUnit(0);
        vipVoucher.setRemainingQuantity(50);
        vipVoucher.setRedeemableWithPoints(true);
        vipVoucher.setPointsPrice(100);
        vipVoucher.setDeleted(false);
        merchandiseRepository.save(vipVoucher);

        // ===== NEWS =====
        NewsItem news1 = new NewsItem();
        news1.setTitle("E2E Test News 1");
        news1.setSummary("Kurze Zusammenfassung für News 1");
        news1.setText("Dies ist ein ausführlicher Text für News 1, der für E2E Tests genutzt wird.");
        news1.setPublishedAt(LocalDate.now().minusDays(1)); // bereits veröffentlicht
        news1 = newsItemRepository.save(news1);

        NewsItem news2 = new NewsItem();
        news2.setTitle("E2E Test News 2");
        news2.setSummary("Kurze Zusammenfassung für News 2");
        news2.setText("Dies ist ein ausführlicher Text für News 2, der für E2E Tests genutzt wird.");
        news2.setPublishedAt(LocalDate.now()); // heute veröffentlicht
        news2 = newsItemRepository.save(news2);

        NewsItem news3 = new NewsItem();
        news3.setTitle("E2E Test News 3 - Future");
        news3.setSummary("Kurze Zusammenfassung für News 3");
        news3.setText("Dies ist ein ausführlicher Text für News 3, der für E2E Tests genutzt wird.");
        news3.setPublishedAt(LocalDate.now().plusDays(2)); // zukünftige Veröffentlichung
        news3 = newsItemRepository.save(news3);

        SeenNews seenNews = new SeenNews(user5, news1);
        seenNewsItemRepository.save(seenNews);


        System.out.println("E2E Testdaten geladen");
    }
}