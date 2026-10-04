package com.younes.eccomerc.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ProductRepo
 */
public interface ProductRepo extends JpaRepository<Product , Integer>{

    List<Product> findAllByIdInOrderById(List<Integer> productIds);

    @EntityGraph(attributePaths = "category")
    @Override
    List<Product> findAll();

    @EntityGraph(attributePaths = "category")
    @Override
    Optional<Product> findById(Integer id);

    /**
     * Decrements stock in a single conditional statement. The check on
     * availableQuantity and the write happen atomically inside the database, so two
     * concurrent purchases of the last units cannot both succeed. Returns the number
     * of updated rows: 0 means there was not enough stock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
               set p.availableQuantity = p.availableQuantity - :quantity
             where p.id = :id
               and p.availableQuantity >= :quantity
            """)
    int decrementStock(@Param("id") Integer id, @Param("quantity") double quantity);

    /**
     * Gives stock back, used to compensate an order that was rolled back after the
     * reservation succeeded.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.availableQuantity = p.availableQuantity + :quantity where p.id = :id")
    int incrementStock(@Param("id") Integer id, @Param("quantity") double quantity);
}