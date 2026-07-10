package com.ionista.service.impl;

import com.ionista.dto.request.AddressRequest;
import com.ionista.dto.response.AddressResponse;
import com.ionista.entity.Address;
import com.ionista.entity.User;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.mapper.AddressMapper;
import com.ionista.repository.AddressRepository;
import com.ionista.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("jane@example.com").build();
        user.setId(1L);

        var principal = org.springframework.security.core.userdetails.User
                .withUsername("jane@example.com").password("x").authorities("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        lenient().when(addressMapper.toResponse(any(Address.class))).thenReturn(AddressResponse.builder().build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Address buildAddress(Long id, boolean isDefault) {
        Address address = Address.builder().user(user).fullName("Jane Doe").phone("123")
                .line1("Street 1").city("City").state("State").postalCode("100000").country("India")
                .isDefault(isDefault).build();
        address.setId(id);
        return address;
    }

    private AddressRequest buildRequest(Boolean isDefault) {
        return AddressRequest.builder().fullName("Jane Doe").phone("123").line1("Street 1")
                .city("City").state("State").postalCode("100000").country("India").isDefault(isDefault).build();
    }

    @Test
    void create_makesFirstAddressDefault_automatically() {
        when(addressRepository.findByUserId(1L)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.create(buildRequest(null));

        verify(addressRepository).save(argThat(Address::isDefault));
    }

    @Test
    void create_doesNotMakeDefault_whenNotRequestedAndAddressesExist() {
        Address existing = buildAddress(1L, true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.create(buildRequest(null));

        verify(addressRepository).save(argThat(a -> !a.isDefault()));
    }

    @Test
    void create_unsetsExistingDefault_whenNewAddressRequestedAsDefault() {
        Address existing = buildAddress(1L, true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.create(buildRequest(true));

        assertThat(existing.isDefault()).isFalse();
        verify(addressRepository, times(2)).save(any(Address.class));
    }

    @Test
    void update_updatesFields() {
        Address existing = buildAddress(1L, false);
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest request = buildRequest(null);
        request.setCity("New City");
        addressService.update(1L, request);

        assertThat(existing.getCity()).isEqualTo("New City");
    }

    @Test
    void update_throws_whenAddressNotOwned() {
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(1L, buildRequest(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesOwnedAddress() {
        Address existing = buildAddress(1L, false);
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        addressService.delete(1L);

        verify(addressRepository).delete(existing);
    }

    @Test
    void setDefault_unsetsOldDefault_andSetsNewOne() {
        Address oldDefault = buildAddress(1L, true);
        Address newDefault = buildAddress(2L, false);
        when(addressRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(newDefault));
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(oldDefault, newDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.setDefault(2L);

        assertThat(oldDefault.isDefault()).isFalse();
        assertThat(newDefault.isDefault()).isTrue();
    }

    @Test
    void list_returnsMappedAddresses() {
        Address existing = buildAddress(1L, true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(existing));

        List<AddressResponse> result = addressService.list();

        assertThat(result).hasSize(1);
    }
}
