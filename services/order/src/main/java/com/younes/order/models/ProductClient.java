package com.younes.order.models;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.younes.order.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ProductClient
 */
@Service 
@RequiredArgsConstructor 
public class ProductClient {

    @Value("${application.config.product-url}")
    private String productUrl;
    private final RestTemplate restTemplate;

    public List<PurchaseResponse> purchaseProduct(List<PurchaseRequest> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json"); 
        HttpEntity<List<PurchaseRequest>> requestEntity = new HttpEntity<>(requestBody , headers);
        ParameterizedTypeReference<List<PurchaseResponse>> responseType = new ParameterizedTypeReference<>(){};
        ResponseEntity<List<PurchaseResponse>> responseEntity = restTemplate.exchange(productUrl, HttpMethod.POST , requestEntity , responseType);
        if(responseEntity.getStatusCode().isError()) {
            throw new BusinessException("An error occured while processing the products purchases : " + responseEntity.getStatusCode());
        }
        return responseEntity.getBody( );
    } 
}
