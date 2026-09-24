package com.younes.order.models;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * OrderLineService
 */
@Service 
@RequiredArgsConstructor 
public class OrderLineService {
    private final OrderlineRepo repo;
    private final OrderLineMapper mapper;
    public Integer saveOrderLine(OrderLineRequest orderLineRequest) {
       var order = mapper.toOrderLine(orderLineRequest);
       return repo.save(order).getId();
    }


}
