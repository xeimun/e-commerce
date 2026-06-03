package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartItemAddRequest;
import com.ecommerce.cart.dto.CartItemMutationResponse;
import com.ecommerce.cart.dto.CartItemQuantityUpdateRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartItemQuantityExceededException;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartResponse getCart(Long customerId) {
        Cart cart = findOrCreateCart(requirePositiveCustomerId(customerId));

        return CartResponse.from(cart);
    }

    @Transactional
    public CartItemMutationResponse addItem(Long customerId, CartItemAddRequest request) {
        Long validCustomerId = requirePositiveCustomerId(customerId);
        Product product = findProduct(request.productId());
        Cart cart = findOrCreateCart(validCustomerId);
        long quantity = requirePositiveQuantity(request.quantity());
        long targetQuantity = cart.findItemByProductId(product.getId())
                .map(cartItem -> Math.addExact(cartItem.getQuantity(), quantity))
                .orElse(quantity);
        validateStock(product, targetQuantity);

        CartItem cartItem = cart.findItemByProductId(product.getId())
                .map(existingItem -> {
                    existingItem.changeQuantity(targetQuantity);
                    return existingItem;
                })
                .orElseGet(() -> cart.addItem(product, quantity));

        return CartItemMutationResponse.from(cartItem);
    }

    @Transactional
    public CartItemMutationResponse updateItemQuantity(
            Long customerId,
            Long cartItemId,
            CartItemQuantityUpdateRequest request
    ) {
        Cart cart = findCart(requirePositiveCustomerId(customerId), cartItemId);
        CartItem cartItem = findCartItem(cart, cartItemId);
        long quantity = requirePositiveQuantity(request.quantity());
        validateStock(cartItem.getProduct(), quantity);

        cartItem.changeQuantity(quantity);

        return CartItemMutationResponse.from(cartItem);
    }

    @Transactional
    public void deleteItem(Long customerId, Long cartItemId) {
        Cart cart = findCart(requirePositiveCustomerId(customerId), cartItemId);
        CartItem cartItem = findCartItem(cart, cartItemId);

        cart.removeItem(cartItem);
    }

    private Cart findOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.create(customerId)));
    }

    private Cart findCart(Long customerId, Long cartItemId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartItemNotFoundException(customerId, cartItemId));
    }

    private CartItem findCartItem(Cart cart, Long cartItemId) {
        return cart.findItemById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cart.getCustomerId(), cartItemId));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validateStock(Product product, long quantity) {
        if (!product.getStock().hasEnough(quantity)) {
            throw new CartItemQuantityExceededException(product.getId(), quantity, product.getStock().getQuantity());
        }
    }

    private static Long requirePositiveCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 1 이상이어야 합니다.");
        }

        return customerId;
    }

    private static long requirePositiveQuantity(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("장바구니 상품 수량은 1 이상이어야 합니다.");
        }

        return quantity;
    }
}
