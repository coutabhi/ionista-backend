package com.ionista.service.impl;

import com.cloudinary.Cloudinary;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.exception.BadRequestException;
import com.ionista.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public ImageUploadResult upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of("folder", folder)
            );
            return ImageUploadResult.builder()
                    .url((String) result.get("secure_url"))
                    .publicId((String) result.get("public_id"))
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of());
        } catch (IOException e) {
            throw new BadRequestException("Failed to delete image: " + e.getMessage());
        }
    }
}
