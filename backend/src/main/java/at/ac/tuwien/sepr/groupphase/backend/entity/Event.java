package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "title", nullable = false)
    private String title;

    @Size(max = 100)
    @Column(length = 100)
    private String type;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_artists",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists = new ArrayList<>();

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Ticket> tickets = new ArrayList<>();

    @Lob
    @Column(name = "image", columnDefinition = "BLOB")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    // --------------------------------------------------
    // Constructors
    // --------------------------------------------------

    public Event() {
    }

    /** Vollständiger Konstruktor (dev). */
    public Event(String title, String type, Integer durationMinutes, String description,
                 LocalDateTime dateTime, Location location) {
        this.title = title;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
    }

    /** Minimaler Konstruktor (feature/seatmap-kompatibel). */
    public Event(String title, Location location) {
        this.title = title;
        this.location = location;
    }

    // --------------------------------------------------
    // Getter & Setter
    // --------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }



    // --------------------------------------------------
    // Convenience Methods
    // --------------------------------------------------

    public void addArtist(Artist artist) {
        this.artists.add(artist);
    }

    public void addTicket(Ticket ticket) {
        this.tickets.add(ticket);
        ticket.setEvent(this);
    }

    // --------------------------------------------------
    // equals / hashCode
    // --------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event event)) {
            return false;
        }
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
