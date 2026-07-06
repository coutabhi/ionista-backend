package com.ionista.service.impl;

import com.ionista.common.SecurityUtils;
import com.ionista.dto.request.AddressRequest;
import com.ionista.dto.response.AddressResponse;
import com.ionista.entity.Address;
import com.ionista.entity.User;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.AddressMapper;
import com.ionista.repository.AddressRepository;
import com.ionista.repository.UserRepository;
import com.ionista.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> list() {
        User user = currentUser();
        return addressRepository.findByUserId(user.getId()).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse create(AddressRequest request) {
        User user = currentUser();
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || addressRepository.findByUserId(user.getId()).isEmpty();

        if (makeDefault) {
            unsetExistingDefault(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(makeDefault)
                .build();

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        User user = currentUser();
        Address address = findOwned(id, user.getId());

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            unsetExistingDefault(user.getId());
            address.setDefault(true);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        Address address = findOwned(id, user.getId());
        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long id) {
        User user = currentUser();
        Address address = findOwned(id, user.getId());
        unsetExistingDefault(user.getId());
        address.setDefault(true);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    private void unsetExistingDefault(Long userId) {
        addressRepository.findByUserId(userId).forEach(existing -> {
            if (existing.isDefault()) {
                existing.setDefault(false);
                addressRepository.save(existing);
            }
        });
    }

    private Address findOwned(Long id, Long userId) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
