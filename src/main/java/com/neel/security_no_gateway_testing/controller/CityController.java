package com.neel.security_no_gateway_testing.controller;

import com.neel.security_no_gateway_testing.annotation.ValidatePermission;
import com.neel.security_no_gateway_testing.dto.CityByCountryInfo;
import com.neel.security_no_gateway_testing.dto.CityInfo;
import com.neel.security_no_gateway_testing.service.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.EDIT_PROFILE;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @ValidatePermission({EDIT_PROFILE})
    @GetMapping("/countries/{countryName}/cities")
    public List<CityByCountryInfo> getCityNamesByCountry(
            @PathVariable String countryName,
            @RequestParam String cityName) {
        return cityService.getCityNamesByCountry(countryName, cityName);
    }

    @ValidatePermission({EDIT_PROFILE})
    @GetMapping("/countries")
    public List<String> getCountries(@RequestParam String countryName) {
        return cityService.getCountries(countryName);
    }

    @ValidatePermission({EDIT_PROFILE})
    @GetMapping("/cities/{cityId}")
    public CityInfo getCityCoordinates(
            @PathVariable Long cityId) {
        return cityService.getCityCoordinates(cityId);
    }

}
