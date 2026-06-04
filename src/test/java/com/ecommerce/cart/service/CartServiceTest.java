package com.ecommerce.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.cart.dto.CartItemAddRequest;
import com.ecommerce.cart.dto.CartItemMutationResponse;
import com.ecommerce.cart.dto.CartItemQuantityUpdateRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartItemQuantityExceededException;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productRepository);
    }

    @Test
    void addItemCreatesCartWhenCustomerHasNoCart() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            ReflectionTestUtils.setField(cart, "id", 1L);
            return cart;
        });

        CartItemMutationResponse response = cartService.addItem(7L, new CartItemAddRequest(1L, 2L));

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.quantity()).isEqualTo(2);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemIncreasesQuantityWhenProductAlreadyExistsInCart() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 10);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        CartItemMutationResponse response = cartService.addItem(7L, new CartItemAddRequest(1L, 3L));

        assertThat(response.cartItemId()).isEqualTo(1L);
        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    void addItemThrowsExceptionWhenQuantityExceedsCurrentStock() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 3);
        Cart cart = cartFixture(7L);
        cart.addItem(product, 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(7L, new CartItemAddRequest(1L, 2L)))
                .isInstanceOf(CartItemQuantityExceededException.class);
    }

    @Test
    void updateItemQuantityChangesQuantity() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 10);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        CartItemMutationResponse response = cartService.updateItemQuantity(
                7L,
                1L,
                new CartItemQuantityUpdateRequest(4L)
        );

        assertThat(response.quantity()).isEqualTo(4);
    }

    @Test
    void updateItemQuantityThrowsExceptionWhenItemDoesNotBelongToCustomer() {
        Cart cart = cartFixture(7L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(
                7L,
                99L,
                new CartItemQuantityUpdateRequest(1L)
        ))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void deleteItemRemovesItemFromCart() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 10);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        cartService.deleteItem(7L, 1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void getCartShowsNotSelectableReasonWhenProductIsStopped() {
        Product product = productFixture(1L, ProductStatus.STOPPED, 10);
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(7L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).selectable()).isFalse();
        assertThat(response.items().get(0).notSelectableReason()).isEqualTo("PRODUCT_NOT_ON_SALE");
    }

    @Test
    void getCartIncludesInstantDiscountAndDiscountedPrice() {
        Product product = productFixture(1L, ProductStatus.ON_SALE, 10);
        product.applyInstantDiscount(
                "드롭 오픈 할인",
                new BigDecimal("3000.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
        Cart cart = cartFixture(7L);
        CartItem cartItem = cart.addItem(product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(7L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).price()).isEqualByComparingTo("35000.00");
        assertThat(response.items().get(0).instantDiscountAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.items().get(0).discountedPrice()).isEqualByComparingTo("32000.00");
    }

    private Cart cartFixture(Long customerId) {
        Cart cart = Cart.create(customerId);
        ReflectionTestUtils.setField(cart, "id", 1L);

        return cart;
    }

    private Product productFixture(Long productId, ProductStatus status, long stockQuantity) {
        Product product = Product.create(
                "달빛 상점 한정판 아트북",
                new BigDecimal("35000.00"),
                status,
                "달빛 상점",
                ContentType.WEBTOON,
                "ARTBOOK",
                "웹툰 달빛 상점의 시즌 1 한정판 아트북"
        );
        Stock stock = Stock.create(product, stockQuantity);
        product.registerStock(stock);
        ReflectionTestUtils.setField(product, "id", productId);

        return product;
    }
}
