package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.DetailedNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.NewsItemUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.newsItem.SimpleNewsItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.NewsItemMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsItem;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeenNews;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeenNewsItemRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.NewsItemService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.validators.NewsValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.List;

@Service
public class NewsItemServiceImpl implements NewsItemService {

    private final NewsItemRepository newsItemRepository;
    private final SeenNewsItemRepository seenNewsItemRepository;
    private final NewsValidator newsValidator;
    private final UserService userService;
    private final NewsItemMapper newsItemMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public NewsItemServiceImpl(NewsItemRepository newsItemRepository, SeenNewsItemRepository seenNewsItemRepository,
                               NewsValidator newsValidator, UserService userService, NewsItemMapper newsItemMapper) {
        this.newsItemRepository = newsItemRepository;
        this.seenNewsItemRepository = seenNewsItemRepository;
        this.newsValidator = newsValidator;
        this.userService = userService;
        this.newsItemMapper = newsItemMapper;
    }

    @Override
    public Page<SimpleNewsItemDto> getUnreadNews(String userEmail, Pageable pageable) {
        LOGGER.trace("Getting unread news for user: {} with pagination "
            + "(page={}, size{})", userEmail, pageable.getPageNumber(), pageable.getPageSize());

        User user = userService.findByEmail(userEmail);
        List<SeenNews> seenNewsList = seenNewsItemRepository.findByUser(user);
        List<Long> seenIds = seenNewsList.stream()
            .map(seenNewsId -> seenNewsId.getNewsItem().getId())
            .toList();

        List<SimpleNewsItemDto> result = newsItemRepository.findAll().stream()
            .filter(newsItem -> !seenIds.contains(newsItem.getId()))
            .filter(newsItem -> newsItem.getPublishedAt().isBefore(LocalDate.now()) || newsItem.getPublishedAt().isEqual(LocalDate.now()))
            .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
            .map(newsItemMapper::toSimple)
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());

        List<SimpleNewsItemDto> pageContent = result.subList(start, end);
        LOGGER.trace("Found {} unread news items for user: {}", result.size(), userEmail);

        return new PageImpl<>(pageContent, pageable, result.size());
    }

    @Override
    public Page<SimpleNewsItemDto> getReadNews(String userEmail, Pageable pageable) {
        LOGGER.trace("Getting read news for user: {} with pagination "
            + "(page={}, size{})", userEmail, pageable.getPageNumber(), pageable.getPageSize());

        User user = userService.findByEmail(userEmail);
        List<SeenNews> seenNewsList = seenNewsItemRepository.findByUser(user);
        List<Long> seenIds = seenNewsList.stream()
            .map(seenNews -> seenNews.getNewsItem().getId())
            .toList();

        List<SimpleNewsItemDto> result = newsItemRepository.findAll().stream()
            .filter(newsItem -> seenIds.contains(newsItem.getId()))
            .filter(newsItem -> newsItem.getPublishedAt().isBefore(LocalDate.now()) || newsItem.getPublishedAt().isEqual(LocalDate.now()))
            .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
            .map(newsItemMapper::toSimple)
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());

        List<SimpleNewsItemDto> pageContent = result.subList(start, end);
        LOGGER.trace("Found {} unread news items for user: {}", result.size(), userEmail);

        return new PageImpl<>(pageContent, pageable, result.size());
    }

    @Override
    public Page<SimpleNewsItemDto> findAll(Pageable pageable) {
        LOGGER.trace("Finding all published news items");

        List<SimpleNewsItemDto> result = newsItemRepository.findAll().stream()
            .filter(newsItem -> newsItem.getPublishedAt().isBefore(LocalDate.now()) || newsItem.getPublishedAt().isEqual(LocalDate.now()))
            .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
            .map(newsItemMapper::toSimple)
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());

        List<SimpleNewsItemDto> pageContent = result.subList(start, end);
        LOGGER.trace("Found {} published news items", result.size());

        return new PageImpl<>(pageContent, pageable, result.size());
    }

    @Override
    public Page<SimpleNewsItemDto> getUnpublished(Pageable pageable) {
        LOGGER.trace("Finsing all unpublished news items");

        List<SimpleNewsItemDto> result = newsItemRepository.findAll().stream()
            .filter(newsItem -> newsItem.getPublishedAt().isAfter(LocalDate.now()))
            .sorted((a, b) -> a.getPublishedAt().compareTo(b.getPublishedAt()))
            .map(newsItemMapper::toSimple)
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), result.size());

        List<SimpleNewsItemDto> pageContent = result.subList(start, end);
        LOGGER.trace("Found {} unpublished news items", result.size());

        return new PageImpl<>(pageContent, pageable, result.size());
    }

    @Override
    public DetailedNewsItemDto findById(Long id) {
        LOGGER.trace("Looking for news item with ID: {}", id);
        return newsItemMapper.toDetailed(newsItemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("News item not found: " + id)));
    }

    @Override
    @Transactional
    public void markAsRead(Long newsId, String userEmail) {
        LOGGER.trace("Marking news ID {} as read for user: {}", newsId, userEmail);

        NewsItem newsItem = newsItemMapper.fromDetailedDto(findById(newsId));
        User user = userService.findByEmail(userEmail);
        boolean alreadySeen = seenNewsItemRepository.existsByUserAndNewsItem(user, newsItem);
        if (!alreadySeen) {
            SeenNews seenNews = new SeenNews(user, newsItem);
            seenNewsItemRepository.save(seenNews);
        }
    }

    @Override
    public DetailedNewsItemDto createNews(@Valid NewsItemCreateDto newsItemCreateDto) {
        LOGGER.trace("Creating new news item with title: {}", newsItemCreateDto.title());
        LOGGER.trace("NewsItemCreateDto: {}", newsItemCreateDto);

        newsValidator.validateForCreate(newsItemCreateDto);

        NewsItem newsItem = newsItemMapper.fromCreateDto(newsItemCreateDto);

        if (newsItem.getPublishedAt() == null) {
            newsItem.setPublishedAt(LocalDate.now());
        }

        return newsItemMapper.toDetailed(newsItemRepository.save(newsItem));
    }

    @Override
    @Transactional
    public DetailedNewsItemDto update(@Valid NewsItemUpdateDto newsItemUpdateDto) {
        LOGGER.trace("Updating news item with ID: {}", newsItemUpdateDto.id());
        LOGGER.trace("Update data: {}", newsItemUpdateDto);

        newsValidator.validateForUpdate(newsItemUpdateDto);

        NewsItem existingNews = newsItemRepository.findById(newsItemUpdateDto.id())
            .orElseThrow(() -> new NotFoundException("News item not found: " + newsItemUpdateDto.id()));

        existingNews.setTitle(newsItemUpdateDto.title());
        existingNews.setSummary(newsItemUpdateDto.summary());
        existingNews.setText(newsItemUpdateDto.text());
        existingNews.setPublishedAt(newsItemUpdateDto.publishedAt());

        if (existingNews.getPublishedAt() == null) {
            existingNews.setPublishedAt(LocalDate.now());
        }

        NewsItem updatedNews = newsItemRepository.save(existingNews);

        return newsItemMapper.toDetailed(newsItemRepository.save(updatedNews));
    }

    @Override
    @Transactional
    public void uploadImage(Long newsId, MultipartFile image) {
        LOGGER.debug("Upload image for news item {}", newsId);
        LOGGER.trace("Image details - Name: {}, Size: {}, Content-Type: {}", image.getOriginalFilename(), image.getSize(), image.getContentType());

        newsValidator.validateImage(image);

        NewsItem newsItem = newsItemRepository.findById(newsId).orElseThrow(() -> new NotFoundException("Die News wurde nicht gefunden"));
        try {
            newsItem.setImageData(image.getBytes());
            newsItem.setImageContentType(image.getContentType());
        } catch (IOException e) {
            throw new ValidationException("Bilddaten konnten nicht gelesen werden");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImageData(Long newsId) {
        LOGGER.debug("Get image for news item {}", newsId);

        NewsItem newsItem = newsItemRepository.findById(newsId).orElseThrow(() -> new NotFoundException("Die News wurde nicht gefunden"));

        return newsItem.getImageData();
    }

    @Override
    public String getImageContentType(Long newsId) {
        LOGGER.trace("Getting image content type for news news item {}", newsId);

        NewsItem newsItem = newsItemRepository.findById(newsId).orElseThrow(() -> new NotFoundException("Die News wurde nicht gefunden"));

        if (newsItem.getImageData() == null) {
            throw new NotFoundException("Es wurde kein Bild für diese Veranstaltung gefunden");
        }

        return newsItem.getImageContentType();
    }

    @Override
    @Transactional
    public void deleteImage(Long newsId) {
        LOGGER.debug("Delete image for event {}", newsId);
        NewsItem newsItem = newsItemRepository.findById(newsId)
            .orElseThrow(() -> new NotFoundException("Event not found: " + newsId));

        if (newsItem.getImageData() == null) {
            throw new NotFoundException("Veranstaltung hat kein Bild");
        }

        newsItem.setImageData(null);
        newsItem.setImageContentType(null);
        newsItemRepository.save(newsItem);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LOGGER.trace("Deleting news item {}", id);

        NewsItem newsItem = newsItemMapper.fromDetailedDto(findById(id));
        if (newsItem == null) {
            throw new NotFoundException("News not found: " + id);
        }

        newsItemRepository.deleteById(id);
    }
}