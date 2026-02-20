package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RewardRedeemRequestDto(
    @NotEmpty @Valid List<MerchandisePurchaseItemDto> items
) { }
