package com.ionista.service;

import com.ionista.dto.response.ImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    ImageUploadResult upload(MultipartFile file, String folder);

    void delete(String publicId);
}
