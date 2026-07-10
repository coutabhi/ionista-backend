package com.ionista.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ionista.dto.response.ImageUploadResult;
import com.ionista.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryServiceImpl(cloudinary);
    }

    @Test
    void upload_throws_whenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> cloudinaryService.upload(file, "folder"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void upload_throws_whenFileIsNull() {
        assertThatThrownBy(() -> cloudinaryService.upload(null, "folder"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void upload_returnsUrlAndPublicId_onSuccess() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "http://cdn/img.jpg", "public_id", "ionista/products/1/abc"));

        ImageUploadResult result = cloudinaryService.upload(file, "ionista/products/1");

        assertThat(result.getUrl()).isEqualTo("http://cdn/img.jpg");
        assertThat(result.getPublicId()).isEqualTo("ionista/products/1/abc");
    }

    @Test
    void upload_wrapsIOException_asBadRequestException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> cloudinaryService.upload(file, "folder"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Failed to upload image");
    }

    @Test
    void delete_callsUploaderDestroy() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        cloudinaryService.delete("public-id-1");

        verify(uploader).destroy(eq("public-id-1"), anyMap());
    }

    @Test
    void delete_wrapsIOException_asBadRequestException() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network error"));

        assertThatThrownBy(() -> cloudinaryService.delete("public-id-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Failed to delete image");
    }
}
