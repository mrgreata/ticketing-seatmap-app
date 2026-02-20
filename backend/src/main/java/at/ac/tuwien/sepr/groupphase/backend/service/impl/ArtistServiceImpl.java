package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ArtistMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;

import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArtistServiceImpl implements ArtistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass());

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    @Override
    public List<Artist> findAll() {
        LOGGER.debug("Find all artists");
        return artistRepository.findAll();
    }

    @Override
    public Artist findById(Long id) {
        LOGGER.debug("Find artist by id {}", id);
        return artistRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Artist nicht gefunden: " + id));
    }

    @Override
    @Transactional
    public Artist create(Artist artist) {
        LOGGER.debug("Create artist {}", artist);

        if (artist.getName() == null || artist.getName().isBlank()) {
            throw new ValidationException("Artist muss einen Namen haben");
        }

        if (artist.getIsBand() == null) {
            artist.setIsBand(false);
        }

        Artist saved = artistRepository.save(artist);

        if (Boolean.TRUE.equals(artist.getIsBand()) && !artist.getMembers().isEmpty()) {
            List<Artist> resolvedMembers = new ArrayList<>();
            for (Artist member : artist.getMembers()) {
                if (member.getId() != null) {
                    Artist existingMember = findById(member.getId());
                    resolvedMembers.add(existingMember);
                }
            }
            saved.setMembers(resolvedMembers);
            saved = artistRepository.save(saved);
        }

        return saved;
    }

    @Override
    public List<Artist> searchByName(String name) {
        LOGGER.debug("Search artists by name: {}", name);
        return artistRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Artist> searchArtistsWithBands(String name) {
        LOGGER.debug("Search artists with bands by name: {}", name);

        List<Artist> directMatches = artistRepository.findByNameContainingIgnoreCase(name);
        Set<Artist> result = new HashSet<>(directMatches);

        for (Artist artist : directMatches) {
            if (!Boolean.TRUE.equals(artist.getIsBand())) {
                List<Artist> bands = artistRepository.findBandsByMemberId(artist.getId());
                result.addAll(bands);
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    @Transactional
    public SimpleArtistDto createFromDto(ArtistCreateDto dto) {
        LOGGER.debug("Create artist from DTO: {}", dto.name());

        Artist entity = artistMapper.fromCreateDto(dto);
        entity.setIsBand(dto.isBand() != null ? dto.isBand() : false);

        if (Boolean.TRUE.equals(entity.getIsBand()) && dto.memberIds() != null) {
            List<Artist> members = new ArrayList<>();
            for (Long memberId : dto.memberIds()) {
                Artist member = findById(memberId);
                members.add(member);
            }
            entity.setMembers(members);
        }

        Artist saved = create(entity);
        return artistMapper.toSimple(saved);
    }

    @Override
    @Transactional
    public List<SimpleArtistDto> searchArtistsAsDto(String name, boolean includeBands) {
        LOGGER.debug("Search artists as DTO: name={}, includeBands={}", name, includeBands);

        List<Artist> artists;
        if (includeBands) {
            artists = searchArtistsWithBands(name);
        } else {
            artists = searchByName(name);
        }

        return artists.stream()
            .map(artistMapper::toSimple)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleArtistDto> findAllAsDto() {
        LOGGER.debug("Find all artists as DTOs");
        return findAll().stream()
            .map(artistMapper::toSimple)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DetailedArtistDto findByIdAsDto(Long id) {
        LOGGER.debug("Find artist by id {} as detailed DTO", id);
        return artistMapper.toDetailed(findById(id));
    }
}