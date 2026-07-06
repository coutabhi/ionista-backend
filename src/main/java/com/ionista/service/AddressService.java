package com.ionista.service;

import com.ionista.dto.request.AddressRequest;
import com.ionista.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> list();

    AddressResponse create(AddressRequest request);

    AddressResponse update(Long id, AddressRequest request);

    void delete(Long id);

    AddressResponse setDefault(Long id);
}
