package com.ionista.repository;

import com.ionista.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @Query("SELECT o FROM Offer o WHERE o.active = true AND :now BETWEEN o.startsAt AND o.endsAt")
    List<Offer> findActiveOffersNow(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT o FROM Offer o LEFT JOIN o.products p LEFT JOIN o.categories c " +
            "WHERE o.active = true AND :now BETWEEN o.startsAt AND o.endsAt " +
            "AND (p.id = :productId OR c.id = :categoryId)")
    List<Offer> findActiveOffersForProductOrCategory(@Param("productId") Long productId,
                                                       @Param("categoryId") Long categoryId,
                                                       @Param("now") LocalDateTime now);
}
