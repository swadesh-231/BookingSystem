package com.bookingsystem.service;

import com.bookingsystem.dto.HotelRequest;
import com.bookingsystem.dto.HotelResponse;

import java.util.List;

public interface HotelService {
    HotelResponse createNewHotel(HotelRequest hotelRequest);
    HotelResponse getHotelById(Long id);
    List<HotelResponse> findAllHotels();
    HotelResponse updateHotelById(Long id, HotelRequest hotelRequest);
    void deleteHotelById(Long id);
    HotelResponse updateHotelStatus(Long id, Boolean status);
}
