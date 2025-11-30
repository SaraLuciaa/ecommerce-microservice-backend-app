package com.selimhorri.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeatureToggleService {

    @Value("${app.feature.fetch-details:true}")
    private boolean fetchDetailsEnabled;

    public boolean isFetchDetailsEnabled() {
        log.info("Checking feature toggle 'fetch-details': {}", fetchDetailsEnabled);
        return fetchDetailsEnabled;
    }
}
