package com.ionista.service;

import com.ionista.dto.request.GalleryItemRequest;
import com.ionista.dto.response.GalleryItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GalleryItemService {

    List<GalleryItemResponse> listActive();

    List<GalleryItemResponse> listAll();

    GalleryItemResponse create(GalleryItemRequest request);

    GalleryItemResponse update(Long id, GalleryItemRequest request);

    void delete(Long id);

    GalleryItemResponse uploadImage(Long id, MultipartFile file);
}
