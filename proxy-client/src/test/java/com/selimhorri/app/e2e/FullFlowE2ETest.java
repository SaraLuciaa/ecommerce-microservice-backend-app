package com.selimhorri.app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.order.service.OrderClientService;
import com.selimhorri.app.business.payment.service.PaymentClientService;
import com.selimhorri.app.business.product.service.ProductClientService;
import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.business.user.service.CredentialClientService;
import com.selimhorri.app.business.favourite.service.FavouriteClientService;
import com.selimhorri.app.business.favourite.model.FavouriteDto;
import com.selimhorri.app.business.favourite.model.response.FavouriteFavouriteServiceCollectionDtoResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.payment.model.response.PaymentPaymentServiceDtoCollectionResponse;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.payment.model.PaymentStatus;
import com.selimhorri.app.business.auth.model.request.AuthenticationRequest;
import com.selimhorri.app.business.auth.model.response.AuthenticationResponse;
import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.payment.model.PaymentDto;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.UserDto;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.openfeign.enabled=false",
    "spring.config.import=",
    "SPRING_CONFIG_IMPORT=",
    "spring.cloud.config.import-check.enabled=false",
    "server.servlet.context-path="
})
@AutoConfigureMockMvc
class FullFlowE2ETest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserClientService userClientService;
    @MockBean private CredentialClientService credentialClientService;
    @MockBean private ProductClientService productClientService;
    @MockBean private OrderClientService orderClientService;
    @MockBean private PaymentClientService paymentClientService;
    @MockBean private FavouriteClientService favouriteClientService;

    private String jwt;

    private final Map<Integer, UserDto> users = new ConcurrentHashMap<>();
    private final AtomicInteger userId = new AtomicInteger();
    private final Map<Integer, ProductDto> products = new ConcurrentHashMap<>();
    private final AtomicInteger productId = new AtomicInteger();
    private final Map<Integer, OrderDto> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderId = new AtomicInteger();
    private final Map<Integer, PaymentDto> payments = new ConcurrentHashMap<>();
    private final AtomicInteger paymentId = new AtomicInteger();
    private final Map<String, FavouriteDto> favourites = new ConcurrentHashMap<>();

    @BeforeEach
    void authenticate() throws Exception {
        Mockito.when(userClientService.findAll())
            .thenAnswer(inv -> ResponseEntity.ok(UserUserServiceCollectionDtoResponse.builder().collection(new ArrayList<>(users.values())).build()));
        Mockito.when(userClientService.findById(Mockito.anyString()))
            .thenAnswer(inv -> ResponseEntity.ok(users.get(Integer.valueOf(inv.getArgument(0)))));
        Mockito.when(userClientService.findByUsername(Mockito.anyString()))
            .thenAnswer(inv -> ResponseEntity.ok(users.values().stream().filter(u -> u.getCredentialDto() != null && inv.getArgument(0).equals(u.getCredentialDto().getUsername())).findFirst().orElse(null)));
        Mockito.when(userClientService.save(Mockito.any(UserDto.class)))
            .thenAnswer(inv -> {
                UserDto req = inv.getArgument(0);
                int id = userId.incrementAndGet();
                UserDto saved = UserDto.builder()
                    .userId(id)
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .email(req.getEmail())
                    .phone(req.getPhone())
                    .credentialDto(req.getCredentialDto() == null ? null : CredentialDto.builder()
                        .credentialId(id)
                        .username(req.getCredentialDto().getUsername())
                        .password(req.getCredentialDto().getPassword())
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                    .build();
                users.put(id, saved);
                return ResponseEntity.ok(saved);
            });

        Mockito.when(credentialClientService.findByUsername(Mockito.anyString()))
            .thenAnswer(inv -> {
                String uname = inv.getArgument(0);
                UserDto u = users.values().stream().filter(us -> us.getCredentialDto() != null && uname.equals(us.getCredentialDto().getUsername())).findFirst().orElse(null);
                return ResponseEntity.ok(u == null ? null : u.getCredentialDto());
            });

        // Products
        if (products.isEmpty()) {
            int id1 = productId.incrementAndGet();
            products.put(id1, ProductDto.builder().productId(id1).productTitle("Laptop").priceUnit(1200.0).sku("LPT-1").quantity(10).build());
            int id2 = productId.incrementAndGet();
            products.put(id2, ProductDto.builder().productId(id2).productTitle("Headphones").priceUnit(150.0).sku("HPH-2").quantity(50).build());
        }
        Mockito.when(productClientService.findAll())
            .thenAnswer(inv -> ResponseEntity.ok(ProductProductServiceCollectionDtoResponse.builder().collection(new ArrayList<>(products.values())).build()));
        Mockito.when(productClientService.save(Mockito.any(ProductDto.class)))
            .thenAnswer(inv -> {
                ProductDto req = inv.getArgument(0);
                int id = productId.incrementAndGet();
                ProductDto saved = ProductDto.builder().productId(id).productTitle(req.getProductTitle()).priceUnit(req.getPriceUnit()).sku(req.getSku()).quantity(req.getQuantity()).build();
                products.put(id, saved);
                return ResponseEntity.ok(saved);
            });

        // Orders
        Mockito.when(orderClientService.save(Mockito.any(OrderDto.class)))
            .thenAnswer(inv -> {
                OrderDto req = inv.getArgument(0);
                int id = orderId.incrementAndGet();
                OrderDto saved = OrderDto.builder().orderId(id).orderDate(req.getOrderDate()).orderDesc(req.getOrderDesc()).orderFee(req.getOrderFee()).cartDto(req.getCartDto()).build();
                orders.put(id, saved);
                return ResponseEntity.ok(saved);
            });
        Mockito.when(orderClientService.findAll())
            .thenAnswer(inv -> ResponseEntity.ok(OrderOrderServiceDtoCollectionResponse.builder().collection(new ArrayList<>(orders.values())).build()));

        // Payments
        Mockito.when(paymentClientService.save(Mockito.any(PaymentDto.class)))
            .thenAnswer(inv -> {
                PaymentDto req = inv.getArgument(0);
                int id = paymentId.incrementAndGet();
                PaymentDto saved = PaymentDto.builder().paymentId(id).isPayed(Boolean.TRUE).paymentStatus(PaymentStatus.COMPLETED).orderDto(req.getOrderDto()).build();
                payments.put(id, saved);
                return ResponseEntity.ok(saved);
            });
        Mockito.when(paymentClientService.findAll())
            .thenAnswer(inv -> ResponseEntity.ok(PaymentPaymentServiceDtoCollectionResponse.builder().collection(new ArrayList<>(payments.values())).build()));

        // Favourites
        Mockito.when(favouriteClientService.findAll())
            .thenAnswer(inv -> ResponseEntity.ok(FavouriteFavouriteServiceCollectionDtoResponse.builder().collection(new ArrayList<>(favourites.values())).build()));
        Mockito.when(favouriteClientService.save(Mockito.any(FavouriteDto.class)))
            .thenAnswer(inv -> {
                FavouriteDto req = inv.getArgument(0);
                String key = req.getUserId() + ":" + req.getProductId() + ":" + (req.getLikeDate() == null ? "now" : req.getLikeDate().toString());
                favourites.put(key, req);
                return ResponseEntity.ok(req);
            });

        AuthenticationRequest req = AuthenticationRequest.builder()
            .username("test")
            .password("password")
            .build();
        MvcResult res = this.mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();
        AuthenticationResponse auth = this.objectMapper.readValue(res.getResponse().getContentAsString(StandardCharsets.UTF_8), AuthenticationResponse.class);
        assertThat(auth.getJwtToken()).isNotBlank();
        this.jwt = auth.getJwtToken();
    }

    @Test
    void signup_signin() throws Exception {
        UserDto newUser = UserDto.builder()
            .firstName("Juan")
            .lastName("Pérez")
            .email("juan.perez@example.com")
            .phone("3110001111")
            .credentialDto(CredentialDto.builder()
                .username("juan")
                .password("123456")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .build())
            .build();

        this.mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(newUser)))
            .andExpect(status().isOk());

        AuthenticationRequest req = AuthenticationRequest.builder().username("test").password("password").build();
        MvcResult res = this.mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();
        AuthenticationResponse auth = this.objectMapper.readValue(res.getResponse().getContentAsString(StandardCharsets.UTF_8), AuthenticationResponse.class);
        assertThat(auth.getJwtToken()).isNotBlank();
    }

    @Test
    void signin_create_order() throws Exception {
        OrderDto order = OrderDto.builder()
            .orderDate(LocalDateTime.now())
            .orderDesc("Pedido de prueba")
            .orderFee(99.99)
            .cartDto(CartDto.builder().userId(1).build())
            .build();

        MvcResult res = this.mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(order)))
            .andExpect(status().isOk())
            .andReturn();
        OrderDto created = this.objectMapper.readValue(res.getResponse().getContentAsString(StandardCharsets.UTF_8), OrderDto.class);
        assertThat(created.getOrderId()).isNotNull();
    }

    @Test
    void order_payment() throws Exception {
        OrderDto order = OrderDto.builder()
            .orderDesc("Pedido para pagar")
            .orderFee(49.50)
            .cartDto(CartDto.builder().userId(1).build())
            .build();
        MvcResult createdOrderRes = this.mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(order)))
            .andExpect(status().isOk())
            .andReturn();
        OrderDto createdOrder = this.objectMapper.readValue(createdOrderRes.getResponse().getContentAsString(StandardCharsets.UTF_8), OrderDto.class);

        PaymentDto payment = PaymentDto.builder()
            .orderDto(com.selimhorri.app.business.payment.model.OrderDto.builder()
                .orderId(createdOrder.getOrderId())
                .orderDesc(createdOrder.getOrderDesc())
                .orderFee(createdOrder.getOrderFee())
                .orderDate(createdOrder.getOrderDate())
                .build())
            .build();

        MvcResult payRes = this.mockMvc.perform(post("/api/payments")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(payment)))
            .andExpect(status().isOk())
            .andReturn();
        PaymentDto createdPayment = this.objectMapper.readValue(payRes.getResponse().getContentAsString(StandardCharsets.UTF_8), PaymentDto.class);
        assertThat(createdPayment.getPaymentId()).isNotNull();
        assertThat(createdPayment.getIsPayed()).isTrue();
    }

    @Test
    void list_products_add_and_order() throws Exception {
        this.mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk());

        ProductDto p = ProductDto.builder().productTitle("Mouse").priceUnit(25.0).sku("MS-9").quantity(5).build();
        this.mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(p)))
            .andExpect(status().isOk());

        OrderDto order = OrderDto.builder()
            .orderDesc("Pedido con mouse")
            .orderFee(25.0)
            .cartDto(CartDto.builder().userId(1).build())
            .build();
        this.mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(order)))
            .andExpect(status().isOk());
    }

    @Test
    void complete_flow_signup_signin_order_payment_list_payments() throws Exception {
        UserDto newUser = UserDto.builder()
            .firstName("Ana")
            .lastName("García")
            .email("ana@example.com")
            .phone("3112223333")
            .credentialDto(CredentialDto.builder()
                .username("ana")
                .password("pwd")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .build())
            .build();

        this.mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(newUser)))
            .andExpect(status().isOk());

        AuthenticationRequest req = AuthenticationRequest.builder().username("test").password("password").build();
        this.mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());

        OrderDto order = OrderDto.builder()
            .orderDesc("Pedido Ana")
            .orderFee(10.0)
            .cartDto(CartDto.builder().userId(1).build())
            .build();
        MvcResult createdOrderRes = this.mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(order)))
            .andExpect(status().isOk())
            .andReturn();
        OrderDto createdOrder = this.objectMapper.readValue(createdOrderRes.getResponse().getContentAsString(StandardCharsets.UTF_8), OrderDto.class);

        PaymentDto payment = PaymentDto.builder()
            .orderDto(com.selimhorri.app.business.payment.model.OrderDto.builder()
                .orderId(createdOrder.getOrderId())
                .orderDesc(createdOrder.getOrderDesc())
                .orderFee(createdOrder.getOrderFee())
                .orderDate(createdOrder.getOrderDate())
                .build())
            .build();

        this.mockMvc.perform(post("/api/payments")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(payment)))
            .andExpect(status().isOk());

        MvcResult listRes = this.mockMvc.perform(get("/api/payments")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andReturn();
        String body = listRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("collection");
    }

    @Test
    void favourite_add_and_list() throws Exception {
        FavouriteDto fav = FavouriteDto.builder()
            .userId(1)
            .productId(1)
            .likeDate(java.time.LocalDateTime.now())
            .build();

        this.mockMvc.perform(post("/api/favourites")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(fav)))
            .andExpect(status().isOk());

        MvcResult listRes = this.mockMvc.perform(get("/api/favourites")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andReturn();
        String body = listRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("collection");
    }
}