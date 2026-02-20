package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ArtistMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl artistService;

    private Artist soloArtist;
    private Artist bandArtist;
    private Artist member1;
    private Artist member2;

    @BeforeEach
    void setup() {
        soloArtist = new Artist();
        soloArtist.setId(1L);
        soloArtist.setName("Ed Sheeran");
        soloArtist.setIsBand(false);
        soloArtist.setMembers(new ArrayList<>());

        member1 = new Artist();
        member1.setId(2L);
        member1.setName("John Lennon");
        member1.setIsBand(false);

        member2 = new Artist();
        member2.setId(3L);
        member2.setName("Paul McCartney");
        member2.setIsBand(false);

        bandArtist = new Artist();
        bandArtist.setId(4L);
        bandArtist.setName("The Beatles");
        bandArtist.setIsBand(true);
        bandArtist.setMembers(List.of(member1, member2));
    }

    @Test
    void findAll_returnsAllArtists() {
        when(artistRepository.findAll()).thenReturn(List.of(soloArtist, bandArtist));

        List<Artist> result = artistService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(soloArtist, bandArtist);
        verify(artistRepository).findAll();
    }

    @Test
    void findById_existingId_returnsArtist() {
        when(artistRepository.findById(1L)).thenReturn(Optional.of(soloArtist));

        Artist result = artistService.findById(1L);

        assertThat(result).isEqualTo(soloArtist);
        verify(artistRepository).findById(1L);
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Artist nicht gefunden");
    }

    @Test
    void create_validSoloArtist_savesSuccessfully() {
        Artist newArtist = new Artist();
        newArtist.setName("Adele");
        newArtist.setIsBand(false);
        newArtist.setMembers(new ArrayList<>());

        when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);

        Artist result = artistService.create(newArtist);

        assertThat(result).isEqualTo(newArtist);
        verify(artistRepository).save(newArtist);
    }

    @Test
    void create_validBandWithMembers_resolvesMembers() {
        Artist band = new Artist();
        band.setName("New Band");
        band.setIsBand(true);

        Artist memberWithIdOnly = new Artist();
        memberWithIdOnly.setId(2L);
        band.setMembers(List.of(memberWithIdOnly));

        when(artistRepository.save(any(Artist.class)))
            .thenReturn(band)
            .thenReturn(band);
        when(artistRepository.findById(2L)).thenReturn(Optional.of(member1));

        Artist result = artistService.create(band);

        assertThat(result).isEqualTo(band);
        verify(artistRepository, times(2)).save(any(Artist.class));
        verify(artistRepository).findById(2L);
    }

    @Test
    void create_nullName_throwsValidationException() {
        Artist invalid = new Artist();
        invalid.setName(null);

        assertThatThrownBy(() -> artistService.create(invalid))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("muss einen Namen haben");
    }

    @Test
    void create_blankName_throwsValidationException() {
        Artist invalid = new Artist();
        invalid.setName("   ");

        assertThatThrownBy(() -> artistService.create(invalid))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("muss einen Namen haben");
    }

    @Test
    void create_nullIsBand_setsToFalse() {
        Artist artist = new Artist();
        artist.setName("Artist");
        artist.setIsBand(null);
        artist.setMembers(new ArrayList<>());

        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist saved = inv.getArgument(0);
            assertThat(saved.getIsBand()).isFalse();
            return saved;
        });

        artistService.create(artist);

        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void searchByName_findsMatchingArtists() {
        when(artistRepository.findByNameContainingIgnoreCase("Beatles"))
            .thenReturn(List.of(bandArtist));

        List<Artist> result = artistService.searchByName("Beatles");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("The Beatles");
        verify(artistRepository).findByNameContainingIgnoreCase("Beatles");
    }

    @Test
    void searchArtistsWithBands_includesBandsOfSoloArtists() {
        when(artistRepository.findByNameContainingIgnoreCase("Lennon"))
            .thenReturn(List.of(member1));
        when(artistRepository.findBandsByMemberId(2L))
            .thenReturn(List.of(bandArtist));

        List<Artist> result = artistService.searchArtistsWithBands("Lennon");

        assertThat(result).hasSize(2);
        assertThat(result).contains(member1, bandArtist);
        verify(artistRepository).findByNameContainingIgnoreCase("Lennon");
        verify(artistRepository).findBandsByMemberId(2L);
    }

    @Test
    void searchArtistsWithBands_bandArtist_returnsOnlyBand() {
        when(artistRepository.findByNameContainingIgnoreCase("Beatles"))
            .thenReturn(List.of(bandArtist));

        List<Artist> result = artistService.searchArtistsWithBands("Beatles");

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(bandArtist);
        verify(artistRepository, never()).findBandsByMemberId(any());
    }

    @Test
    void createFromDto_soloArtist_createsSuccessfully() {
        ArtistCreateDto dto = new ArtistCreateDto("Adele", false, null);

        Artist mappedArtist = new Artist();
        mappedArtist.setName("Adele");
        mappedArtist.setIsBand(false);
        mappedArtist.setMembers(new ArrayList<>());

        Artist savedArtist = new Artist();
        savedArtist.setId(10L);
        savedArtist.setName("Adele");
        savedArtist.setIsBand(false);

        SimpleArtistDto simpleDto = new SimpleArtistDto(10L, "Adele", false, List.of());

        when(artistMapper.fromCreateDto(dto)).thenReturn(mappedArtist);
        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);
        when(artistMapper.toSimple(savedArtist)).thenReturn(simpleDto);

        SimpleArtistDto result = artistService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Adele");
        assertThat(result.isBand()).isFalse();
        verify(artistMapper).fromCreateDto(dto);
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void createFromDto_bandWithMembers_resolvesMembers() {
        ArtistCreateDto dto = new ArtistCreateDto("New Band", true, List.of(2L, 3L));

        Artist mappedArtist = new Artist();
        mappedArtist.setName("New Band");
        mappedArtist.setMembers(new ArrayList<>());

        Artist savedArtist = new Artist();
        savedArtist.setId(20L);
        savedArtist.setName("New Band");
        savedArtist.setIsBand(true);
        savedArtist.setMembers(List.of(member1, member2));

        SimpleArtistDto simpleDto = new SimpleArtistDto(20L, "New Band", true, List.of(2L, 3L));

        when(artistMapper.fromCreateDto(dto)).thenReturn(mappedArtist);
        when(artistRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(artistRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(artistRepository.save(any(Artist.class)))
            .thenReturn(savedArtist)
            .thenReturn(savedArtist);
        when(artistMapper.toSimple(savedArtist)).thenReturn(simpleDto);

        SimpleArtistDto result = artistService.createFromDto(dto);

        assertThat(result.isBand()).isTrue();
        assertThat(result.memberIds()).containsExactly(2L, 3L);

        verify(artistRepository, times(2)).findById(2L);
        verify(artistRepository, times(2)).findById(3L);
        verify(artistRepository, times(2)).save(any(Artist.class));
    }

    @Test
    void createFromDto_memberNotFound_throwsNotFoundException() {
        ArtistCreateDto dto = new ArtistCreateDto("Band", true, List.of(999L));

        Artist mappedArtist = new Artist();
        mappedArtist.setName("Band");
        mappedArtist.setMembers(new ArrayList<>());

        when(artistMapper.fromCreateDto(dto)).thenReturn(mappedArtist);
        when(artistRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.createFromDto(dto))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchArtistsAsDto_withoutBands_searchesByName() {
        when(artistRepository.findByNameContainingIgnoreCase("Ed"))
            .thenReturn(List.of(soloArtist));

        SimpleArtistDto dto = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());
        when(artistMapper.toSimple(soloArtist)).thenReturn(dto);

        List<SimpleArtistDto> result = artistService.searchArtistsAsDto("Ed", false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Ed Sheeran");
        verify(artistRepository).findByNameContainingIgnoreCase("Ed");
        verify(artistRepository, never()).findBandsByMemberId(any());
    }

    @Test
    void searchArtistsAsDto_withBands_includesBands() {
        when(artistRepository.findByNameContainingIgnoreCase("John"))
            .thenReturn(List.of(member1));
        when(artistRepository.findBandsByMemberId(2L))
            .thenReturn(List.of(bandArtist));

        SimpleArtistDto dto1 = new SimpleArtistDto(2L, "John Lennon", false, List.of());
        SimpleArtistDto dto2 = new SimpleArtistDto(4L, "The Beatles", true, List.of(2L, 3L));

        when(artistMapper.toSimple(member1)).thenReturn(dto1);
        when(artistMapper.toSimple(bandArtist)).thenReturn(dto2);

        List<SimpleArtistDto> result = artistService.searchArtistsAsDto("John", true);

        assertThat(result).hasSize(2);
        verify(artistRepository).findBandsByMemberId(2L);
    }

    @Test
    void findAllAsDto_returnsAllAsDtos() {
        when(artistRepository.findAll()).thenReturn(List.of(soloArtist, bandArtist));

        SimpleArtistDto dto1 = new SimpleArtistDto(1L, "Ed Sheeran", false, List.of());
        SimpleArtistDto dto2 = new SimpleArtistDto(4L, "The Beatles", true, List.of(2L, 3L));

        when(artistMapper.toSimple(soloArtist)).thenReturn(dto1);
        when(artistMapper.toSimple(bandArtist)).thenReturn(dto2);

        List<SimpleArtistDto> result = artistService.findAllAsDto();

        assertThat(result).hasSize(2);
        verify(artistRepository).findAll();
    }

    @Test
    void findByIdAsDto_returnsDetailedDto() {
        when(artistRepository.findById(4L)).thenReturn(Optional.of(bandArtist));

        DetailedArtistDto detailedDto = new DetailedArtistDto(
            4L,
            "The Beatles",
            true,
            List.of(
                new SimpleArtistDto(2L, "John Lennon", false, List.of()),
                new SimpleArtistDto(3L, "Paul McCartney", false, List.of())
            ),
            List.of()
        );

        when(artistMapper.toDetailed(bandArtist)).thenReturn(detailedDto);

        DetailedArtistDto result = artistService.findByIdAsDto(4L);

        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.name()).isEqualTo("The Beatles");
        assertThat(result.members()).hasSize(2);
        verify(artistRepository).findById(4L);
        verify(artistMapper).toDetailed(bandArtist);
    }

    @Test
    void create_bandWithEmptyMembersList_savesWithoutResolvingMembers() {
        Artist band = new Artist();
        band.setName("Empty Band");
        band.setIsBand(true);
        band.setMembers(new ArrayList<>());

        when(artistRepository.save(band)).thenReturn(band);

        Artist result = artistService.create(band);

        assertThat(result).isEqualTo(band);
        verify(artistRepository, times(1)).save(band);
        verify(artistRepository, never()).findById(anyLong());
    }

    @Test
    void create_bandWithMemberWithoutId_skipsResolution() {
        Artist band = new Artist();
        band.setName("Band");
        band.setIsBand(true);

        Artist memberWithoutId = new Artist();
        memberWithoutId.setId(null);
        memberWithoutId.setName("NoId Member");
        band.setMembers(List.of(memberWithoutId));

        when(artistRepository.save(band)).thenReturn(band);

        Artist result = artistService.create(band);

        assertThat(result).isEqualTo(band);
        verify(artistRepository, never()).findById(anyLong());
    }

    @Test
    void searchArtistsWithBands_soloArtistWithNoBands_returnsOnlyArtist() {
        when(artistRepository.findByNameContainingIgnoreCase("Solo"))
            .thenReturn(List.of(soloArtist));
        when(artistRepository.findBandsByMemberId(1L))
            .thenReturn(List.of());

        List<Artist> result = artistService.searchArtistsWithBands("Solo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(soloArtist);
        verify(artistRepository).findBandsByMemberId(1L);
    }

    @Test
    void createFromDto_nullMemberIds_createsWithoutMembers() {
        ArtistCreateDto dto = new ArtistCreateDto("Solo Artist", false, null);

        Artist mappedArtist = new Artist();
        mappedArtist.setName("Solo Artist");
        mappedArtist.setMembers(new ArrayList<>());

        Artist savedArtist = new Artist();
        savedArtist.setId(99L);
        savedArtist.setName("Solo Artist");

        SimpleArtistDto simpleDto = new SimpleArtistDto(99L, "Solo Artist", false, List.of());

        when(artistMapper.fromCreateDto(dto)).thenReturn(mappedArtist);
        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);
        when(artistMapper.toSimple(savedArtist)).thenReturn(simpleDto);

        SimpleArtistDto result = artistService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(99L);
        verify(artistRepository, never()).findById(anyLong());
    }

    @Test
    void createFromDto_bandWithNullIsBand_defaultsToFalse() {
        ArtistCreateDto dto = new ArtistCreateDto("Artist", null, null);

        Artist mappedArtist = new Artist();
        mappedArtist.setName("Artist");
        mappedArtist.setMembers(new ArrayList<>());

        Artist savedArtist = new Artist();
        savedArtist.setId(88L);
        savedArtist.setName("Artist");
        savedArtist.setIsBand(false);

        SimpleArtistDto simpleDto = new SimpleArtistDto(88L, "Artist", false, List.of());

        when(artistMapper.fromCreateDto(dto)).thenReturn(mappedArtist);
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist arg = inv.getArgument(0);
            assertThat(arg.getIsBand()).isFalse();
            return savedArtist;
        });
        when(artistMapper.toSimple(savedArtist)).thenReturn(simpleDto);

        SimpleArtistDto result = artistService.createFromDto(dto);

        assertThat(result.isBand()).isFalse();
    }
}