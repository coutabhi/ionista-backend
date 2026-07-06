package com.ionista.controller;

import com.ionista.dto.request.AddressRequest;
import com.ionista.dto.response.AddressResponse;
import com.ionista.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list() {
        return ResponseEntity.ok(addressService.list());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(id));
    }
}
