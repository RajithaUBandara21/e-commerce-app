package com.rajitha.ecommerce.repository;
import com.rajitha.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant,Integer>{
    List<ProductVariant> findAllByIdInOrderById(List<Integer> variantIds);

    List<ProductVariant> findAllByProduct_Id(Integer productId);
}
