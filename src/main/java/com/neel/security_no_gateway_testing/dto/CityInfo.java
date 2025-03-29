package com.neel.security_no_gateway_testing.dto;

public record CityInfo(
        Long cityId,
        String cityName,
        Double longitude,
        Double latitude
) {
}
