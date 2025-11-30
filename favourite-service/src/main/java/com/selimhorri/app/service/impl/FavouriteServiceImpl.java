package com.selimhorri.app.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

	private final FavouriteRepository favouriteRepository;
	private final RestTemplate restTemplate;

	private static final String SERVICE_CB = "favouriteService";

	@Override
	@CircuitBreaker(name = SERVICE_CB, fallbackMethod = "findAllFallback")
	public List<FavouriteDto> findAll() {
		log.info("*** FavouriteDto List, service; fetch all favourites *");
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(this.restTemplate
							.getForObject(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + f.getUserId(),
									UserDto.class));
					f.setProductDto(this.restTemplate
							.getForObject(
									AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + f.getProductId(),
									ProductDto.class));
					return f;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	// Fallback para findAll
	public List<FavouriteDto> findAllFallback(Throwable t) {
		log.error("Circuit Breaker activado (findAll). Error: {}", t.getMessage());
		return Collections.emptyList();
	}

	@Override
	@CircuitBreaker(name = SERVICE_CB, fallbackMethod = "findByIdFallback")
	public FavouriteDto findById(final FavouriteId favouriteId) {
		log.info("*** FavouriteDto, service; fetch favourite by id *");
		return this.favouriteRepository.findById(favouriteId)
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(this.restTemplate
							.getForObject(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + f.getUserId(),
									UserDto.class));
					f.setProductDto(this.restTemplate
							.getForObject(
									AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + f.getProductId(),
									ProductDto.class));
					return f;
				})
				.orElseThrow(() -> new FavouriteNotFoundException(
						String.format("Favourite with id: [%s] not found!", favouriteId)));
	}

	// --- CORRECCIÓN AQUÍ ---
	public FavouriteDto findByIdFallback(final FavouriteId favouriteId, Throwable t) {
		log.error("Circuit Breaker activado (findById) para ID {}. Error: {}", favouriteId, t.getMessage());

		FavouriteDto fallbackDto = new FavouriteDto();
		// Mapeo manual de los campos del ID al DTO
		fallbackDto.setUserId(favouriteId.getUserId());
		fallbackDto.setProductId(favouriteId.getProductId());
		fallbackDto.setLikeDate(favouriteId.getLikeDate());

		// Los objetos complejos quedan en null indicando que no se pudieron recuperar
		fallbackDto.setUserDto(null);
		fallbackDto.setProductDto(null);

		return fallbackDto;
	}

	@Override
	public FavouriteDto save(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}

	@Override
	public FavouriteDto update(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}

	@Override
	public void deleteById(final FavouriteId favouriteId) {
		this.favouriteRepository.deleteById(favouriteId);
	}
}