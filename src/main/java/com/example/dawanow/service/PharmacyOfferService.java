package com.example.dawanow.service;

import com.example.dawanow.dtos.request.CreateOfferItemRequest;
import com.example.dawanow.dtos.request.CreateOfferRequest;
import com.example.dawanow.dtos.response.MedicineRequestItemResponse;
import com.example.dawanow.dtos.response.MedicineRequestResponse;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.PharmacyOfferResponse;
import com.example.dawanow.entity.*;
import com.example.dawanow.exception.ResourceNotFoundException;
import com.example.dawanow.mapper.PharmacyOfferMapper;
import com.example.dawanow.repo.PharmacyAssignmentRepository;
import com.example.dawanow.repo.PharmacyOfferRepository;
import com.example.dawanow.repo.ProductRepository;
import com.example.dawanow.repo.RequestItemRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyOfferService {

    private final MedicineRequestService medicineRequestService;
    private final CurrentUserProvider currentUserProvider;
    private final PharmacyAssignmentRepository pharmacyAssignmentRepository;
    private final ProductRepository productRepository;
    private final RequestItemRepository requestItemRepository;
    private final PharmacyOfferRepository pharmacyOfferRepository;
    private final PharmacyOfferMapper pharmacyOfferMapper;

    public PaginatedResponse<PharmacyOfferResponse> getOffersByPharmacy(Long pharmacyId, Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    @Transactional
    public PharmacyOfferResponse createOffer(
            Long requestId,
            CreateOfferRequest request
    ) throws AccessDeniedException, BadRequestException {
        Pharmacist pharmacist = (Pharmacist) currentUserProvider.get();


        if(pharmacyOfferRepository.existsByPharmacyIdAndRequestId(
                pharmacist.getPharmacy().getId(), requestId)){
            throw new BadRequestException("You have already made an offer for this request");
        }

        MedicineRequest medicineRequest = medicineRequestService.getEntity(requestId);


        PharmacyAssignment assignment = pharmacyAssignmentRepository
                .findByPharmacyIdAndMedicineRequestId(pharmacist.getPharmacy().getId(), requestId)
                .orElseThrow(() -> new AccessDeniedException("This request was not assigned to your pharmacy"));

        PharmacyOffer offer = new PharmacyOffer();
        offer.setRequest(medicineRequest);
        offer.setPharmacy(pharmacist.getPharmacy());
        offer.setPharmacist(pharmacist);
        offer.setDistanceKm(assignment.getDistanceKm());

        BigDecimal totalOfferPrice = BigDecimal.ZERO;
        for (CreateOfferItemRequest itemDto : request.items()) {
            PharmacyOfferItem pharmacyOfferItem = new PharmacyOfferItem();
            pharmacyOfferItem.setOffer(offer);
            pharmacyOfferItem.setQuantity(itemDto.quantity());
            pharmacyOfferItem.setUnitPrice(itemDto.unitPrice());

            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            RequestItem requestItem = requestItemRepository.findById(itemDto.requestItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Request item not found"));

            if (!requestItem.getRequest().getId().equals(requestId)) {
                throw new BadRequestException("Request item does not belong to this request");
            }
            pharmacyOfferItem.setRequestItem(requestItem);

            pharmacyOfferItem.setProduct(product);
            offer.getItems().add(pharmacyOfferItem);

            totalOfferPrice = totalOfferPrice.add(
                    itemDto.unitPrice()
                            .multiply(BigDecimal.valueOf(itemDto.quantity()))
            );
        }
        offer.setTotalPrice(totalOfferPrice);
        pharmacyOfferRepository.save(offer);
        return pharmacyOfferMapper.toResponse(offer);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PharmacyOfferResponse> getRequestOffers(Long requestId, Pageable pageable){

        Page<PharmacyOffer> offers =
                pharmacyOfferRepository.findByRequestIdOrderByDistanceKmAsc(requestId, pageable);

        System.out.println(offers.getTotalElements());
        System.out.println(offers.getContent());
        return PaginatedResponse.from(
                pharmacyOfferRepository.findByRequestIdOrderByDistanceKmAsc(requestId, pageable).map(pharmacyOfferMapper::toResponse)
        );
    }

    public PharmacyOfferResponse getOfferById(Long id) {
        PharmacyOffer pharmacyOffer = pharmacyOfferRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Offer Not Found"));
        return pharmacyOfferMapper.toResponse(pharmacyOffer);
    }

    public PharmacyOfferResponse acceptOffer(Long id) {
        return null;
    }

    public PharmacyOfferResponse rejectOffer(Long id) {
        return null;
    }

    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double EARTH_RADIUS = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
