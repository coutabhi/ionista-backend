package com.ionista.service;

import com.ionista.dto.request.OfferRequest;
import com.ionista.dto.response.OfferResponse;

import java.util.List;

public interface OfferService {

    List<OfferResponse> listAll();

    List<OfferResponse> listActive();

    OfferResponse create(OfferRequest request);

    OfferResponse update(Long id, OfferRequest request);

    void delete(Long id);
}
