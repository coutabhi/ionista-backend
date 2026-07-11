package com.ionista.service;

import java.util.Map;

public interface SiteContentService {

    Map<String, String> getAll();

    Map<String, String> update(Map<String, String> updates);
}
