package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record ProductImageResponseDTO(Integer id, String url, int position) {
}
