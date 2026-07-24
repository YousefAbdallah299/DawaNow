package com.example.dawanow.repo;

import com.example.dawanow.entity.PharmacyOfferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PharmacyOfferItemRepository extends JpaRepository<PharmacyOfferItem, Long> {

    Optional<PharmacyOfferItem> findByRequestItemId(Long requestItemId);

}
