package at.ac.tuwien.sepr.groupphase.backend.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.DetailedArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.artist.SimpleArtistDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ArtistMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ArtistMapperTest {

    @Autowired
    private ArtistMapper artistMapper;

    @Test
    void toSimple_mapsEntityToSimpleDto() {
        Artist member1 = new Artist();
        member1.setId(2L);
        member1.setName("John Lennon");
        member1.setIsBand(false);

        Artist member2 = new Artist();
        member2.setId(3L);
        member2.setName("Paul McCartney");
        member2.setIsBand(false);

        Artist band = new Artist();
        band.setId(1L);
        band.setName("The Beatles");
        band.setIsBand(true);
        band.setMembers(List.of(member1, member2));

        SimpleArtistDto dto = artistMapper.toSimple(band);


        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("The Beatles", dto.name());
        assertTrue(dto.isBand());
        assertEquals(2, dto.memberIds().size());
        assertTrue(dto.memberIds().contains(2L));
        assertTrue(dto.memberIds().contains(3L));
    }

    @Test
    void toSimple_withNullMembers_returnsEmptyMemberIds() {
        Artist artist = new Artist();
        artist.setId(1L);
        artist.setName("Solo Artist");
        artist.setIsBand(false);
        artist.setMembers(null);

        SimpleArtistDto dto = artistMapper.toSimple(artist);


        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("Solo Artist", dto.name());
        assertFalse(dto.isBand());
        assertTrue(dto.memberIds().isEmpty());
    }

    @Test
    void toSimple_withEmptyMembers_returnsEmptyMemberIds() {
        Artist artist = new Artist();
        artist.setId(5L);
        artist.setName("New Artist");
        artist.setIsBand(false);
        artist.setMembers(new ArrayList<>());

        SimpleArtistDto dto = artistMapper.toSimple(artist);


        assertNotNull(dto);
        assertEquals(5L, dto.id());
        assertTrue(dto.memberIds().isEmpty());
    }

    @Test
    void toSimple_withNullArtist_returnsNull() {
        SimpleArtistDto dto = artistMapper.toSimple(null);

        assertNull(dto);
    }

    @Test
    void toDetailed_mapsEntityToDetailedDto() {
        Artist member1 = new Artist();
        member1.setId(2L);
        member1.setName("John Lennon");
        member1.setIsBand(false);
        member1.setMembers(new ArrayList<>());
        member1.setBandsWhereMember(new ArrayList<>());

        Artist member2 = new Artist();
        member2.setId(3L);
        member2.setName("Paul McCartney");
        member2.setIsBand(false);
        member2.setMembers(new ArrayList<>());
        member2.setBandsWhereMember(new ArrayList<>());

        Artist band = new Artist();
        band.setId(1L);
        band.setName("The Beatles");
        band.setIsBand(true);
        band.setMembers(List.of(member1, member2));
        band.setBandsWhereMember(new ArrayList<>());

        DetailedArtistDto dto = artistMapper.toDetailed(band);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("The Beatles", dto.name());
        assertTrue(dto.isBand());
        assertEquals(2, dto.members().size());
        assertEquals("John Lennon", dto.members().get(0).name());
        assertEquals("Paul McCartney", dto.members().get(1).name());
        assertNotNull(dto.bandsWhereMember());
        assertTrue(dto.bandsWhereMember().isEmpty());
    }

    @Test
    void toDetailed_withBandsWhereMember_mapsCorrectly() {
        Artist soloArtist = new Artist();
        soloArtist.setId(5L);
        soloArtist.setName("Dave Grohl");
        soloArtist.setIsBand(false);
        soloArtist.setMembers(new ArrayList<>());

        Artist band1 = new Artist();
        band1.setId(10L);
        band1.setName("Foo Fighters");
        band1.setIsBand(true);

        Artist band2 = new Artist();
        band2.setId(11L);
        band2.setName("Nirvana");
        band2.setIsBand(true);

        soloArtist.setBandsWhereMember(List.of(band1, band2));

        DetailedArtistDto dto = artistMapper.toDetailed(soloArtist);

        assertNotNull(dto);
        assertEquals(5L, dto.id());
        assertEquals("Dave Grohl", dto.name());
        assertFalse(dto.isBand());
        assertEquals(2, dto.bandsWhereMember().size());
        assertEquals("Foo Fighters", dto.bandsWhereMember().get(0).name());
        assertEquals("Nirvana", dto.bandsWhereMember().get(1).name());
    }

    @Test
    void toDetailed_withNullArtist_returnsNull() {
        DetailedArtistDto dto = artistMapper.toDetailed(null);

        assertNull(dto);
    }

    @Test
    void fromCreateDto_mapsCreateDtoToEntity() {
        ArtistCreateDto dto = new ArtistCreateDto(
            "Adele",
            false,
            null
        );

        Artist entity = artistMapper.fromCreateDto(dto);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals("Adele", entity.getName());
        assertFalse(entity.getIsBand());
    }

    @Test
    void fromCreateDto_withBandData_mapsCorrectly() {
        ArtistCreateDto dto = new ArtistCreateDto(
            "New Band",
            true,
            List.of(1L, 2L)
        );

        Artist entity = artistMapper.fromCreateDto(dto);

        assertNotNull(entity);
        assertNull(entity.getId());
        assertEquals("New Band", entity.getName());
        assertTrue(entity.getIsBand());
        assertNotNull(entity.getMembers());
        assertTrue(entity.getMembers().isEmpty());
    }

    @Test
    void fromCreateDto_withNullIsBand_mapsCorrectly() {
        ArtistCreateDto dto = new ArtistCreateDto(
            "Artist Name",
            null,
            null
        );

        Artist entity = artistMapper.fromCreateDto(dto);

        assertNotNull(entity);
        assertEquals("Artist Name", entity.getName());
    }

    @Test
    void getMemberIds_withValidArtist_returnsCorrectIds() {
        Artist member1 = new Artist();
        member1.setId(5L);

        Artist member2 = new Artist();
        member2.setId(10L);

        Artist band = new Artist();
        band.setMembers(List.of(member1, member2));

        List<Long> memberIds = artistMapper.getMemberIds(band);

        assertEquals(2, memberIds.size());
        assertTrue(memberIds.contains(5L));
        assertTrue(memberIds.contains(10L));
    }

    @Test
    void getMemberIds_withNullArtist_returnsEmptyList() {
        List<Long> memberIds = artistMapper.getMemberIds(null);

        assertNotNull(memberIds);
        assertTrue(memberIds.isEmpty());
    }

    @Test
    void getMemberIds_withNullMembers_returnsEmptyList() {
        Artist artist = new Artist();
        artist.setMembers(null);

        List<Long> memberIds = artistMapper.getMemberIds(artist);

        assertNotNull(memberIds);
        assertTrue(memberIds.isEmpty());
    }

    @Test
    void getMemberIds_withEmptyMembers_returnsEmptyList() {
        Artist artist = new Artist();
        artist.setMembers(new ArrayList<>());

        List<Long> memberIds = artistMapper.getMemberIds(artist);

        assertNotNull(memberIds);
        assertTrue(memberIds.isEmpty());
    }
}