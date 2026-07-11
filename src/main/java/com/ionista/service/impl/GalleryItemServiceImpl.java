package com.ionista.service.impl;

import com.ionista.dto.request.GalleryItemRequest;
import com.ionista.dto.response.GalleryItemResponse;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.entity.GalleryItem;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.GalleryItemRepository;
import com.ionista.service.CloudinaryService;
import com.ionista.service.GalleryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GalleryItemServiceImpl implements GalleryItemService {

    private final GalleryItemRepository galleryItemRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public List<GalleryItemResponse> listActive() {
        return galleryItemRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GalleryItemResponse> listAll() {
        return galleryItemRepository.findAllByOrderBySortOrderAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public GalleryItemResponse create(GalleryItemRequest request) {
        GalleryItem item = GalleryItem.builder()
                .title(request.getTitle())
                .caption(request.getCaption())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(galleryItemRepository.save(item));
    }

    @Override
    @Transactional
    public GalleryItemResponse update(Long id, GalleryItemRequest request) {
        GalleryItem item = findItem(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle());
        }
        if (request.getCaption() != null) {
            item.setCaption(request.getCaption());
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }

        return toResponse(galleryItemRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GalleryItem item = findItem(id);
        if (item.getImagePublicId() != null) {
            cloudinaryService.delete(item.getImagePublicId());
        }
        galleryItemRepository.delete(item);
    }

    @Override
    @Transactional
    public GalleryItemResponse uploadImage(Long id, MultipartFile file) {
        GalleryItem item = findItem(id);
        if (item.getImagePublicId() != null) {
            cloudinaryService.delete(item.getImagePublicId());
        }
        ImageUploadResult result = cloudinaryService.upload(file, "ionista/gallery");
        item.setImageUrl(result.getUrl());
        item.setImagePublicId(result.getPublicId());
        return toResponse(galleryItemRepository.save(item));
    }

    private GalleryItem findItem(Long id) {
        return galleryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery item not found with id: " + id));
    }

    private GalleryItemResponse toResponse(GalleryItem item) {
        return GalleryItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .caption(item.getCaption())
                .imageUrl(item.getImageUrl())
                .sortOrder(item.getSortOrder())
                .active(item.isActive())
                .build();
    }
}
