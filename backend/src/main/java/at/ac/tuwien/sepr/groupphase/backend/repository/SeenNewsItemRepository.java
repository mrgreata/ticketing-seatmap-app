package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeenNews;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeenNewsItemRepository extends JpaRepository<SeenNews, Long> {
    List<SeenNews> findByUser(User user);

    boolean existsByUserAndNewsItem(User user, NewsItem newsItem);
}