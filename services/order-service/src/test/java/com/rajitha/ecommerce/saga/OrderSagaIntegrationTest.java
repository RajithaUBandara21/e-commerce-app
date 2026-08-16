package com.rajitha.ecommerce.saga;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// Replaces pure-Mockito coverage for exactly the saga-critical path this
// service owns: a real Postgres + real Kafka broker (Testcontainers), the
// actual @KafkaListener beans (not called directly), real JSON
// (de)serialization, and real DB transactions — the things
// StockReservationConsumerTest/PaymentResultConsumerTest's Mockito unit tests
// deliberately bypass to stay fast. Both styles are kept: this doesn't replace
// those tests, it adds the layer they can't cover (see PLAN.md Phase 16).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext
class OrderSagaIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
        KAFKA.start();
    }

    @AfterAll
    static void stopContainers() {
        KAFKA.stop();
        POSTGRES.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
        registry.add("application.config.customer-url", () -> "http://localhost:8090");
        registry.add("application.config.payment-url", () -> "http://localhost:8091");

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.consumer.key-deserializer", () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    }

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderLineRepository orderLineRepository;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    // The one deliberate synchronous cross-service call createOrder makes
    // (customer validation) — mocked rather than pulled in as a container,
    // since this test's scope is order-service's own saga wiring, not
    // customer-service's.
    @MockitoBean
    CustomerClient customerClient;

    @Test
    void stockReservationSuccessBackfillsSellerIdAndPublishesOrderConfirmation() throws InterruptedException {
        stubCustomer();

        var reference = "saga-it-" + System.nanoTime();
        var orderId = orderService.createOrder(
                OrderRequestDTO.builder()
                        .reference(reference)
                        .totalAmount(new BigDecimal("150.00"))
                        .paymentMethode(PaymentMethode.CREDIT_CARD)
                        .customerId("customer-1")
                        .products(List.of(new PurchaseRequestDTO(42, 2.0)))
                        .build(),
                null
        );

        assertPersistedAsPendingPayment(orderId);

        kafkaTemplate.send(withTopic("stock-topic",
                StockReservationResultEventDTO.builder()
                        .orderReference(reference)
                        .success(true)
                        .totalAmount(new BigDecimal("150.00"))
                        .paymentMethode(PaymentMethode.CREDIT_CARD)
                        .customer(CustomerResponseDTO.builder().id("customer-1").email("a@b.com").build())
                        .products(List.of(PurchaseResponseDTO.builder()
                                .variantId(42).productId(7).name("Tee").price(new BigDecimal("75.00"))
                                .quantity(2.0).sellerId("seller-9").build()))
                        .build()));

        List<OrderLine> lines = awaitCondition(
                () -> orderLineRepository.findOrderLinesByOrderId(orderId),
                result -> !result.isEmpty() && result.get(0).getSellerId() != null);

        Assertions.assertEquals(1, lines.size());
        Assertions.assertEquals("seller-9", lines.get(0).getSellerId());
    }

    @Test
    void paymentSuccessConfirmsOrder() throws InterruptedException {
        stubCustomer();

        var reference = "saga-it-payment-" + System.nanoTime();
        var orderId = orderService.createOrder(
                OrderRequestDTO.builder()
                        .reference(reference)
                        .totalAmount(new BigDecimal("50.00"))
                        .paymentMethode(PaymentMethode.PAYPAL)
                        .customerId("customer-1")
                        .products(List.of(new PurchaseRequestDTO(1, 1.0)))
                        .build(),
                null
        );

        assertPersistedAsPendingPayment(orderId);

        kafkaTemplate.send(withTopic("payment-topic",
                PaymentResultEventDTO.builder().orderReference(reference).success(true).build()));

        Order confirmed = awaitCondition(
                () -> orderRepository.findByReference(reference).orElse(null),
                order -> order != null && order.getStatus() == OrderStatus.CONFIRMED);

        Assertions.assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
    }

    private void assertPersistedAsPendingPayment(Integer orderId) {
        Optional<Order> saved = orderRepository.findById(orderId);
        Assertions.assertTrue(saved.isPresent());
        Assertions.assertEquals(OrderStatus.PENDING_PAYMENT, saved.get().getStatus());
    }

    private void stubCustomer() {
        Mockito.when(customerClient.findCustomerById(Mockito.anyString()))
                .thenReturn(Optional.of(CustomerResponseDTO.builder().id("customer-1").email("a@b.com").build()));
    }

    // Kafka consumption is async — polls the real DB state a real @KafkaListener
    // writes to, rather than sleeping a fixed duration.
    private static <T> T awaitCondition(Supplier<T> fetch, java.util.function.Predicate<T> satisfied) throws InterruptedException {
        var deadline = System.currentTimeMillis() + 15_000;
        T last = null;
        while (System.currentTimeMillis() < deadline) {
            last = fetch.get();
            if (satisfied.test(last)) {
                return last;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Condition not met within 15s, last value: " + last);
    }

    private static <T> Message<T> withTopic(String topic, T payload) {
        return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, topic).build();
    }
}
