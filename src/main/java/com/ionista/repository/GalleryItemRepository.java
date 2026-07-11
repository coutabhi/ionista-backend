package com.ionista.repository;

import com.ionista.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {

    List<GalleryItem> findByActiveTrueOrderBySortOrderAsc();

    List<GalleryItem> findAllByOrderBySortOrderAsc();
}
