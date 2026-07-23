package com.example.dawanow.repo;

import com.example.dawanow.entity.PharmacyOffer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyOfferRepository extends JpaRepository<PharmacyOffer, Long> {

    List<PharmacyOffer> findByRequestId(Long requestId);
}
