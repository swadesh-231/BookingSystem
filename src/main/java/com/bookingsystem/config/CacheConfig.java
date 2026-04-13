package com.bookingsystem.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String HOTEL_SEARCH = "hotelSearch";
    public static final String HOTEL_INFO = "hotelInfo";
    public static final String PLATFORM_STATS = "platformStats";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(HOTEL_SEARCH,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());
        cacheManager.registerCustomCache(HOTEL_INFO,
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .build());
        cacheManager.registerCustomCache(PLATFORM_STATS,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .build());
        return cacheManager;
    }
}
