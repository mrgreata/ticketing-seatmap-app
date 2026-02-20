package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.entity.CancelledTicket;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket.CancelledTicketDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CancelledTicketMapper {

    @Mapping(target = "selected", constant = "false")
    CancelledTicketDto toDto(CancelledTicket entity);


}
