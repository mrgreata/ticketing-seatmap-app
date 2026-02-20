package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;

import java.util.List;

public interface PriceCategoryService {

    List<PriceCategory> findAll();

    PriceCategory findById(Long id);

    PriceCategory create(PriceCategory priceCategory);
}