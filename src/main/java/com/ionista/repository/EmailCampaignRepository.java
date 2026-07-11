package com.ionista.repository;

import com.ionista.entity.EmailCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    List<EmailCampaign> findAllByOrderByCreatedAtDesc();
}
