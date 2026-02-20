package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "seen_news")
public class SeenNews {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private NewsItem newsItem;

    private LocalDateTime seenAt;

    public SeenNews() {
    }

    public SeenNews(User user, NewsItem newsItem) {
        this.user = user;
        this.newsItem = newsItem;
        this.seenAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }
}