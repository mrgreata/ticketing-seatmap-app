package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.DetailedUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.SimpleUserDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.user.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    SimpleUserDto toSimple(User user);

    @Mapping(source = "userRole", target = "userRole")
    @Mapping(source = "address", target = "address")
    DetailedUserDto toDetailed(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "loginFailCount", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "rewardPoints", ignore = true)
    @Mapping(target = "totalCentsSpent", ignore = true)
    @Mapping(target = "adminLocked", ignore = true)
    User fromRegisterDto(UserRegisterDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "loginFailCount", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "rewardPoints", ignore = true)
    @Mapping(target = "totalCentsSpent", ignore = true)
    @Mapping(target = "adminLocked", ignore = true)
    User fromCreateDto(UserCreateDto dto);
}