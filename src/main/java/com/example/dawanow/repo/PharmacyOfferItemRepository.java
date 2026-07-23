package com.example.dawanow.repo;

import com.example.dawanow.entity.PharmacyOfferItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyOfferItemRepository extends JpaRepository<PharmacyOfferItem, Long> {

    @EntityGraph(attributePaths = {
            "offer",
            "offer.request",
            "offer.pharmacy",
            "offer.pharmacist",
            "requestItem",
            "requestItem.product"
    })
    List<PharmacyOfferItem> findByIdIn(Collection<Long> ids);
}
