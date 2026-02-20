package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.PriceCategory;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PriceCategoryRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PriceCategoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class PriceCategoryServiceImpl implements PriceCategoryService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final PriceCategoryRepository priceCategoryRepository;

    public PriceCategoryServiceImpl(PriceCategoryRepository priceCategoryRepository) {
        this.priceCategoryRepository = priceCategoryRepository;
    }

    @Override
    public List<PriceCategory> findAll() {
        LOGGER.debug("Find all price categories");
        return priceCategoryRepository.findAll();
    }

    @Override
    public PriceCategory findById(Long id) {
        LOGGER.debug("Find price category by id {}", id);
        return priceCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Price category not found: " + id));
    }

    @Override
    public PriceCategory create(PriceCategory priceCategory) {
        LOGGER.debug("Create price category {}", priceCategory);
        return priceCategoryRepository.save(priceCategory);
    }
}