package at.ac.tuwien.sepr.groupphase.backend.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(name = "is_band")
    private Boolean isBand = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "band_members",
        joinColumns = @JoinColumn(name = "band_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private List<Artist> members = new ArrayList<>();

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private List<Artist> bandsWhereMember = new ArrayList<>();

    public Artist() {
    }

    public Artist(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsBand() {
        return isBand;
    }

    public void setIsBand(Boolean isBand) {
        this.isBand = isBand;
    }

    public List<Artist> getMembers() {
        return members;
    }

    public void setMembers(List<Artist> members) {
        this.members = members;
    }

    public List<Artist> getBandsWhereMember() {
        return bandsWhereMember;
    }

    public void setBandsWhereMember(List<Artist> bandsWhereMember) {
        this.bandsWhereMember = bandsWhereMember;
    }

    public void addMember(Artist artist) {
        if (!this.members.contains(artist)) {
            this.members.add(artist);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Artist artist)) {
            return false;
        }
        return id != null && id.equals(artist.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}