package com.rajitha.ecommerce.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CartRepository {

    private static final String KEY_PREFIX = "cart:";

    private final StringRedisTemplate redisTemplate;

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    public void incrementItem(String userId, Integer variantId, double quantity) {
        redisTemplate.opsForHash().increment(key(userId), variantId.toString(), quantity);
    }

    public void setItem(String userId, Integer variantId, double quantity) {
        redisTemplate.opsForHash().put(key(userId), variantId.toString(), Double.toString(quantity));
    }

    public void removeItem(String userId, Integer variantId) {
        redisTemplate.opsForHash().delete(key(userId), variantId.toString());
    }

    public Map<Object, Object> getItems(String userId) {
        return redisTemplate.opsForHash().entries(key(userId));
    }

    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }
}
