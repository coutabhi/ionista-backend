package com.ionista.service.impl;

import com.ionista.dto.request.BannerRequest;
import com.ionista.dto.response.BannerResponse;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.entity.Banner;
import com.ionista.exception.ResourceNotFoundException;
import com.ionista.repository.BannerRepository;
import com.ionista.service.BannerService;
import com.ionista.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listActive() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listAll() {
        return bannerRepository.findAllByOrderBySortOrderAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .ctaText(request.getCtaText())
                .ctaLink(request.getCtaLink())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = findBanner(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            banner.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            banner.setSubtitle(request.getSubtitle());
        }
        if (request.getCtaText() != null) {
            banner.setCtaText(request.getCtaText());
        }
        if (request.getCtaLink() != null) {
            banner.setCtaLink(request.getCtaLink());
        }
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }

        return toResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Banner banner = findBanner(id);
        if (banner.getImagePublicId() != null) {
            cloudinaryService.delete(banner.getImagePublicId());
        }
        bannerRepository.delete(banner);
    }

    @Override
    @Transactional
    public BannerResponse uploadImage(Long id, MultipartFile file) {
        Banner banner = findBanner(id);
        if (banner.getImagePublicId() != null) {
            cloudinaryService.delete(banner.getImagePublicId());
        }
        ImageUploadResult result = cloudinaryService.upload(file, "ionista/banners");
        banner.setImageUrl(result.getUrl());
        banner.setImagePublicId(result.getPublicId());
        return toResponse(bannerRepository.save(banner));
    }

    private Banner findBanner(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .ctaText(banner.getCtaText())
                .ctaLink(banner.getCtaLink())
                .imageUrl(banner.getImageUrl())
                .sortOrder(banner.getSortOrder())
                .active(banner.isActive())
                .build();
    }
}
