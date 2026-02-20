package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "zip_code", nullable = false)
    private int zipCode;

    @NotNull
    @Size(max = 255)
    @Column(name = "city", nullable = false)
    private String city;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false)
    private String street;

    @NotNull
    @Size(max = 50)
    @Column(name = "street_number", nullable = false)
    private String streetNumber;

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Sector> sectors = new ArrayList<>();

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Event> events = new ArrayList<>();

    @NotNull
    @Column(name = "stage_position", nullable = false)
    private String stagePosition; // TOP, BOTTOM, LEFT, RIGHT

    @Column(name = "stage_label")
    private String stageLabel; // z.B. "BÜHNE"

    @Column(name = "stage_row_start")
    private Integer stageRowStart;

    @Column(name = "stage_row_end")
    private Integer stageRowEnd;

    @Column(name = "stage_col_start")
    private Integer stageColStart;

    @Column(name = "stage_col_end")
    private Integer stageColEnd;

    @Column(name = "stage_height_px")
    private Integer stageHeightPx;

    @Column(name = "stage_width_px")
    private Integer stageWidthPx;


    public Integer getStageRowStart() {
        return stageRowStart;
    }

    public void setStageRowStart(Integer v) {
        this.stageRowStart = v;
    }

    public Integer getStageHeightPx() {
        return stageHeightPx;
    }

    public void setStageHeightPx(Integer stageHeightPx) {
        this.stageHeightPx = stageHeightPx;
    }

    public Integer getStageWidthPx() {
        return stageWidthPx;
    }

    public void setStageWidthPx(Integer stageWidthPx) {
        this.stageWidthPx = stageWidthPx;
    }

    public Integer getStageRowEnd() {
        return stageRowEnd;
    }

    public void setStageRowEnd(Integer v) {
        this.stageRowEnd = v;
    }

    public Integer getStageColStart() {
        return stageColStart;
    }

    public void setStageColStart(Integer v) {
        this.stageColStart = v;
    }

    public Integer getStageColEnd() {
        return stageColEnd;
    }

    public void setStageColEnd(Integer v) {
        this.stageColEnd = v;
    }


    public Location() {
    }

    public Location(int zipCode, String city, String street, String streetNumber) {
        this.zipCode = zipCode;
        this.city = city;
        this.street = street;
        this.streetNumber = streetNumber;
    }

    public Location(String name, int zipCode, String city, String street, String streetNumber) {
        this.name = name;
        this.zipCode = zipCode;
        this.city = city;
        this.street = street;
        this.streetNumber = streetNumber;
    }

    public String getStagePosition() {
        return stagePosition;
    }

    public void setStagePosition(String stagePosition) {
        this.stagePosition = stagePosition;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public void setStageLabel(String stageLabel) {
        this.stageLabel = stageLabel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getZipCode() {
        return zipCode;
    }

    public void setZipCode(int zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public List<Sector> getSectors() {
        return sectors;
    }

    public void setSectors(List<Sector> sectors) {
        this.sectors = sectors;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location location)) {
            return false;
        }
        return id != null && id.equals(location.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // --- Runway (Catwalk) ---
    @Column(name = "runway_width_px")
    private Integer runwayWidthPx;

    @Column(name = "runway_length_px")
    private Integer runwayLengthPx;

    @Column(name = "runway_offset_px")
    private Integer runwayOffsetPx;

    public Integer getRunwayWidthPx() {
        return runwayWidthPx;
    }

    public void setRunwayWidthPx(Integer runwayWidthPx) {
        this.runwayWidthPx = runwayWidthPx;
    }

    public Integer getRunwayLengthPx() {
        return runwayLengthPx;
    }

    public void setRunwayLengthPx(Integer runwayLengthPx) {
        this.runwayLengthPx = runwayLengthPx;
    }

    public Integer getRunwayOffsetPx() {
        return runwayOffsetPx;
    }

    public void setRunwayOffsetPx(Integer runwayOffsetPx) {
        this.runwayOffsetPx = runwayOffsetPx;
    }


}