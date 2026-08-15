package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.OrderClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.exception.CartEmptyException;
import com.rajitha.ecommerce.repository.CartRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @InjectMocks
    CartServiceImpl cartServiceImpl;
    @Mock CartRepository cartRepository;
    @Mock OrderClient orderClient;

    @Test
    void shouldAddItemAsIncrement(){
        var request = AddCartItemRequestDTO.builder().variantId(1).quantity(2).build();

        cartServiceImpl.addItem("user-1", request);

        Mockito.verify(cartRepository, Mockito.times(1)).incrementItem("user-1", 1, 2.0);
    }

    @Test
    void shouldSetItemQuantity(){
        cartServiceImpl.setItemQuantity("user-1", 1, 5.0);

        Mockito.verify(cartRepository, Mockito.times(1)).setItem("user-1", 1, 5.0);
        Mockito.verify(cartRepository, Mockito.never()).removeItem(Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    void shouldRemoveItemWhenQuantitySetToZero(){
        cartServiceImpl.setItemQuantity("user-1", 1, 0.0);

        Mockito.verify(cartRepository, Mockito.times(1)).removeItem("user-1", 1);
        Mockito.verify(cartRepository, Mockito.never()).setItem(Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble());
    }

    @Test
    void shouldReturnCartItemsFromRedisHash(){
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put("1", "2.0");
        raw.put("2", "1.0");
        Mockito.when(cartRepository.getItems("user-1")).thenReturn(raw);

        CartResponseDTO cart = cartServiceImpl.getCart("user-1");

        Assertions.assertEquals("user-1", cart.userId());
        Assertions.assertEquals(2, cart.items().size());
    }

    @Test
    void shouldThrowWhenCheckoutWithEmptyCart(){
        Mockito.when(cartRepository.getItems("user-1")).thenReturn(Map.of());

        var request = CartCheckoutRequestDTO.builder()
                .reference("ref-1")
                .totalAmount(new BigDecimal("100"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .build();

        Assertions.assertThrows(CartEmptyException.class, () -> cartServiceImpl.checkout("user-1", request, null));

        Mockito.verify(orderClient, Mockito.never()).createOrder(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldCreateOrderAndClearCartOnCheckout(){
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put("1", "2.0");
        Mockito.when(cartRepository.getItems("user-1")).thenReturn(raw);
        Mockito.when(orderClient.createOrder(Mockito.any(OrderRequestDTO.class), Mockito.eq("idem-1"))).thenReturn(42);

        var request = CartCheckoutRequestDTO.builder()
                .reference("ref-1")
                .totalAmount(new BigDecimal("100"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .stripePaymentMethodId("pm_123")
                .build();

        var orderId = cartServiceImpl.checkout("user-1", request, "idem-1");

        Assertions.assertEquals(42, orderId);

        var captor = ArgumentCaptor.forClass(OrderRequestDTO.class);
        Mockito.verify(orderClient, Mockito.times(1)).createOrder(captor.capture(), Mockito.eq("idem-1"));
        Assertions.assertEquals("user-1", captor.getValue().customerId());
        Assertions.assertEquals(1, captor.getValue().products().size());
        Assertions.assertEquals(1, captor.getValue().products().get(0).variantId());
        Assertions.assertEquals(2.0, captor.getValue().products().get(0).quantity());

        Mockito.verify(cartRepository, Mockito.times(1)).clear("user-1");
    }
}
