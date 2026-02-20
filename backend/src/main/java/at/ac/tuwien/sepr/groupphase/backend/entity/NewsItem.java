package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "news_items")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String title;

    @NotNull
    @Column(name = "published_at", nullable = false)
    private LocalDate publishedAt = LocalDate.now();

    @NotNull
    @Size(max = 250)
    @Column(nullable = false)
    private String summary;

    @NotNull
    @Column(nullable = false, length = 800)
    private String text;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;

    public NewsItem() {
    }

    public NewsItem(String title, String summary, String text, LocalDate publishedAt) {
        this.title = title;
        this.summary = summary;
        this.text = text;
        this.publishedAt = publishedAt;
    }

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

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NewsItem news)) {
            return false;
        }
        return id != null && id.equals(news.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}