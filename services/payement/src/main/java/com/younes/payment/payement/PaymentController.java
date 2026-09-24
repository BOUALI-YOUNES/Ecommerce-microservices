package com.younes.payment.payement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor 
public class PaymentController {
    private final PaymentService paymentService;

    public ResponseEntity<Integer> createPayment(
        @RequestBody @Valid PaymentRequest reqeust
    ) {
        return ResponseEntity.ok(paymentService.createPayement(reqeust));
    }

}
