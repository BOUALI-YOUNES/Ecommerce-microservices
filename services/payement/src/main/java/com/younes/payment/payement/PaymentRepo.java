package com.younes.payment.payement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PaymentRepo
 */
public interface PaymentRepo extends JpaRepository<Payment,Integer>{

}
