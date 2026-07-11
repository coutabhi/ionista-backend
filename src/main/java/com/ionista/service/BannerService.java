package com.ionista.service;

import com.ionista.dto.request.BannerRequest;
import com.ionista.dto.response.BannerResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {

    List<BannerResponse> listActive();

    List<BannerResponse> listAll();

    BannerResponse create(BannerRequest request);

    BannerResponse update(Long id, BannerRequest request);

    void delete(Long id);

    BannerResponse uploadImage(Long id, MultipartFile file);
}
