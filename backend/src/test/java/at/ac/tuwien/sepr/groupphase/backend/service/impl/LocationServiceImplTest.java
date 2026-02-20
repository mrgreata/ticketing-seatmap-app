package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.DetailedLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.LocationCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.location.SimpleLocationDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.sector.SimpleSectorDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.LocationMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.SectorMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Location;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.LocationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.SectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private SectorMapper sectorMapper;

    @Mock
    private SectorService sectorService;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location location1;
    private Location location2;
    private Sector sector1;
    private Sector sector2;

    @BeforeEach
    void setup() {
        location1 = new Location();
        location1.setId(1L);
        location1.setName("Stadthalle");
        location1.setCity("Vienna");
        location1.setZipCode(1150);
        location1.setStreet("Vogelweidplatz");
        location1.setStreetNumber("14");
        location1.setStagePosition("TOP");
        location1.setStageLabel("Stage");
        location1.setStageRowStart(1);
        location1.setStageRowEnd(5);
        location1.setStageColStart(1);
        location1.setStageColEnd(10);
        location1.setStageHeightPx(100);
        location1.setStageWidthPx(200);
        location1.setRunwayWidthPx(50);
        location1.setRunwayLengthPx(150);
        location1.setRunwayOffsetPx(25);

        location2 = new Location();
        location2.setId(2L);
        location2.setName("Musikverein");
        location2.setCity("Vienna");
        location2.setZipCode(1010);
        location2.setStreet("Musikvereinsplatz");
        location2.setStreetNumber("1");

        sector1 = new Sector();
        sector1.setId(10L);
        sector1.setName("VIP");
        sector1.setLocation(location1);

        sector2 = new Sector();
        sector2.setId(11L);
        sector2.setName("General");
        sector2.setLocation(location1);
    }

    @Test
    void findAll_returnsAllLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(location1, location2));

        List<Location> result = locationService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(location1, location2);
        verify(locationRepository).findAll();
    }

    @Test
    void findById_existingId_returnsLocation() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location1));

        Location result = locationService.findById(1L);

        assertThat(result).isEqualTo(location1);
        assertThat(result.getName()).isEqualTo("Stadthalle");
        verify(locationRepository).findById(1L);
    }

    @Test
    void findById_nonExistingId_throwsNotFoundException() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.findById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Location not found");
    }

    @Test
    void create_validLocation_savesSuccessfully() {
        Location newLocation = new Location();
        newLocation.setName("New Venue");
        newLocation.setCity("Graz");

        when(locationRepository.save(newLocation)).thenReturn(newLocation);

        Location result = locationService.create(newLocation);

        assertThat(result).isEqualTo(newLocation);
        verify(locationRepository).save(newLocation);
    }

    @Test
    void searchLocations_withName_findsMatchingLocations() {
        when(locationRepository.searchLocations("Stadthalle", null, null, null))
            .thenReturn(List.of(location1));

        List<Location> result = locationService.searchLocations("Stadthalle", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Stadthalle");
        verify(locationRepository).searchLocations("Stadthalle", null, null, null);
    }

    @Test
    void searchLocations_withCity_findsMatchingLocations() {
        when(locationRepository.searchLocations(null, null, "Vienna", null))
            .thenReturn(List.of(location1, location2));

        List<Location> result = locationService.searchLocations(null, null, "Vienna", null);

        assertThat(result).hasSize(2);
        verify(locationRepository).searchLocations(null, null, "Vienna", null);
    }

    @Test
    void searchLocations_withMultipleCriteria_findsMatchingLocations() {
        when(locationRepository.searchLocations("Stadthalle", "Vogelweidplatz", "Vienna", 1150))
            .thenReturn(List.of(location1));

        List<Location> result = locationService.searchLocations("Stadthalle", "Vogelweidplatz", "Vienna", 1150);

        assertThat(result).hasSize(1);
        verify(locationRepository).searchLocations("Stadthalle", "Vogelweidplatz", "Vienna", 1150);
    }

    @Test
    void findByIdAsDto_returnsDetailedDtoWithSectors() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location1));
        when(sectorService.findAll()).thenReturn(List.of(sector1, sector2));

        SimpleSectorDto sectorDto1 = new SimpleSectorDto(10L, "VIP", 1L);
        SimpleSectorDto sectorDto2 = new SimpleSectorDto(11L, "General", 1L);

        when(sectorMapper.toSimple(sector1)).thenReturn(sectorDto1);
        when(sectorMapper.toSimple(sector2)).thenReturn(sectorDto2);

        DetailedLocationDto result = locationService.findByIdAsDto(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Stadthalle");
        assertThat(result.city()).isEqualTo("Vienna");
        assertThat(result.sectors()).hasSize(2);
        assertThat(result.stagePosition()).isEqualTo("TOP");
        assertThat(result.stageLabel()).isEqualTo("Stage");
        assertThat(result.stageHeightPx()).isEqualTo(100);
        assertThat(result.stageWidthPx()).isEqualTo(200);
        assertThat(result.runwayWidthPx()).isEqualTo(50);
        assertThat(result.runwayLengthPx()).isEqualTo(150);
        assertThat(result.runwayOffsetPx()).isEqualTo(25);

        verify(locationRepository).findById(1L);
        verify(sectorService).findAll();
    }

    @Test
    void findByIdAsDto_filtersSectorsForLocation() {
        Sector sector3 = new Sector();
        sector3.setId(12L);
        sector3.setName("Balcony");
        sector3.setLocation(location2);

        when(locationRepository.findById(2L)).thenReturn(Optional.of(location2));
        when(sectorService.findAll()).thenReturn(List.of(sector1, sector2, sector3));

        SimpleSectorDto sectorDto3 = new SimpleSectorDto(12L, "Balcony", 2L);
        when(sectorMapper.toSimple(sector3)).thenReturn(sectorDto3);

        DetailedLocationDto result = locationService.findByIdAsDto(2L);

        assertThat(result.sectors()).hasSize(1);
        assertThat(result.sectors().get(0).name()).isEqualTo("Balcony");
    }

    @Test
    void findByIdAsDto_locationNotFound_throwsNotFoundException() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.findByIdAsDto(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createFromDto_createsLocationSuccessfully() {
        LocationCreateDto dto = new LocationCreateDto(
            "New Arena",
            9020,
            "Klagenfurt",
            "Messeplatz",
            "1"
        );

        Location mappedLocation = new Location();
        mappedLocation.setName("New Arena");
        mappedLocation.setCity("Klagenfurt");

        Location savedLocation = new Location();
        savedLocation.setId(50L);
        savedLocation.setName("New Arena");
        savedLocation.setCity("Klagenfurt");

        SimpleLocationDto simpleDto = new SimpleLocationDto(
            50L, "New Arena", 9020, "Klagenfurt", "Messeplatz", "1",
            null, null, null, null, null, null, null, null, null, null, null
        );

        when(locationMapper.fromCreateDto(dto)).thenReturn(mappedLocation);
        when(locationRepository.save(mappedLocation)).thenReturn(savedLocation);
        when(locationMapper.toSimple(savedLocation)).thenReturn(simpleDto);

        SimpleLocationDto result = locationService.createFromDto(dto);

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.name()).isEqualTo("New Arena");
        assertThat(result.city()).isEqualTo("Klagenfurt");
        verify(locationMapper).fromCreateDto(dto);
        verify(locationRepository).save(mappedLocation);
        verify(locationMapper).toSimple(savedLocation);
    }

    @Test
    void searchLocationsAsDto_returnsSimpleDtos() {
        when(locationRepository.searchLocations("Vienna", null, "Vienna", null))
            .thenReturn(List.of(location1, location2));

        SimpleLocationDto dto1 = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, 50, 150, 25
        );
        SimpleLocationDto dto2 = new SimpleLocationDto(
            2L, "Musikverein", 1010, "Vienna", "Musikvereinsplatz", "1",
            null, null, null, null, null, null, null, null, null, null, null
        );

        when(locationMapper.toSimple(location1)).thenReturn(dto1);
        when(locationMapper.toSimple(location2)).thenReturn(dto2);

        List<SimpleLocationDto> result = locationService.searchLocationsAsDto("Vienna", null, "Vienna", null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Stadthalle");
        assertThat(result.get(1).name()).isEqualTo("Musikverein");
        verify(locationRepository).searchLocations("Vienna", null, "Vienna", null);
    }

    @Test
    void searchLocationsAsDto_noResults_returnsEmptyList() {
        when(locationRepository.searchLocations("NonExistent", null, null, null))
            .thenReturn(List.of());

        List<SimpleLocationDto> result = locationService.searchLocationsAsDto("NonExistent", null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllAsDto_returnsAllAsSimpleDtos() {
        when(locationRepository.findAll()).thenReturn(List.of(location1, location2));

        SimpleLocationDto dto1 = new SimpleLocationDto(
            1L, "Stadthalle", 1150, "Vienna", "Vogelweidplatz", "14",
            "TOP", "Stage", 100, 200, 1, 5, 1, 10, 50, 150, 25
        );
        SimpleLocationDto dto2 = new SimpleLocationDto(
            2L, "Musikverein", 1010, "Vienna", "Musikvereinsplatz", "1",
            null, null, null, null, null, null, null, null, null, null, null
        );

        when(locationMapper.toSimple(location1)).thenReturn(dto1);
        when(locationMapper.toSimple(location2)).thenReturn(dto2);

        List<SimpleLocationDto> result = locationService.findAllAsDto();

        assertThat(result).hasSize(2);
        verify(locationRepository).findAll();
        verify(locationMapper, times(2)).toSimple(any(Location.class));
    }

    @Test
    void findAllAsDto_emptyRepository_returnsEmptyList() {
        when(locationRepository.findAll()).thenReturn(List.of());

        List<SimpleLocationDto> result = locationService.findAllAsDto();

        assertThat(result).isEmpty();
        verify(locationRepository).findAll();
    }
}