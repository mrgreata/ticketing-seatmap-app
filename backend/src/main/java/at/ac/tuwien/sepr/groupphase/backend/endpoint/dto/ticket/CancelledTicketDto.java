package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CancelledTicketDto(
     Long id,
     String eventName,
     LocalDateTime eventDate,
     LocalDate cancellationDate,
     List<String> seats,
     Long creditInvoiceId,
     boolean selected
) {
}
