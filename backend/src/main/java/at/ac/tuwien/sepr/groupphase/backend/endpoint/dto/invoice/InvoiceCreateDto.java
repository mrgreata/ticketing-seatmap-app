
package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;



public record InvoiceCreateDto(


    @NotNull Long userId,

    @NotNull @Size(max = 255)
    String userFirstName,

    @NotNull @Size(max = 255)
    String userLastName,


    @NotNull @Size(max = 255)
    String userAddress,

    @NotNull
    LocalDateTime eventDate,


    //because we need a description ("Concert, Sec A,..), quantity, netprice, taxrate, grossprice
    @NotNull List<Long> ticketIds
) {

}
