package com.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductInstantDiscountRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductStatusUpdateRequest;
import com.ecommerce.product.dto.ProductStockUpdateRequest;
import com.ecommerce.product.dto.ProductUpdateRequest;
import com.ecommerce.product.entity.ContentType;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createProductCreatesProductWithStock() {
        ProductCreateRequest request = new ProductCreateRequest(
                "달빛 상점 한정판 아트북",
                new BigDecimal("35000.00"),
                ProductStatus.ON_SALE,
                "달빛 상점",
                ContentType.WEBTOON,
                "ARTBOOK",
                "웹툰 달빛 상점의 시즌 1 한정판 아트북",
                100L
        );
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertThat(response.name()).isEqualTo("달빛 상점 한정판 아트북");
        assertThat(response.price()).isEqualByComparingTo("35000.00");
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.contentTitle()).isEqualTo("달빛 상점");
        assertThat(response.contentType()).isEqualTo(ContentType.WEBTOON);
        assertThat(response.category()).isEqualTo("ARTBOOK");
        assertThat(response.stockQuantity()).isEqualTo(100);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductUsesOnSaleAsDefaultStatus() {
        ProductCreateRequest request = new ProductCreateRequest(
                "밤의 OST 앨범",
                new BigDecimal("25000.00"),
                null,
                "밤의 노래",
                ContentType.MUSIC,
                "OST",
                null,
                20L
        );
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void updateProductChangesBasicProductFields() {
        Product product = productFixture();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        ProductUpdateRequest request = new ProductUpdateRequest(
                "달빛 상점 한정판 포스터",
                new BigDecimal("12000.00"),
                "달빛 상점",
                ContentType.WEBTOON,
                "POSTER",
                "달빛 상점 포스터"
        );

        ProductResponse response = productService.updateProduct(1L, request);

        assertThat(response.name()).isEqualTo("달빛 상점 한정판 포스터");
        assertThat(response.price()).isEqualByComparingTo("12000.00");
        assertThat(response.category()).isEqualTo("POSTER");
    }

    @Test
    void updateStatusChangesProductStatus() {
        Product product = productFixture();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateStatus(1L, new ProductStatusUpdateRequest(ProductStatus.STOPPED));

        assertThat(response.status()).isEqualTo(ProductStatus.STOPPED);
    }

    @Test
    void updateStockChangesStockQuantity() {
        Product product = productFixture();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateStock(1L, new ProductStockUpdateRequest(7L));

        assertThat(response.stockQuantity()).isEqualTo(7);
    }

    @Test
    void upsertInstantDiscountAppliesActiveDiscountToProductResponse() {
        Product product = productFixture();
        LocalDateTime now = LocalDateTime.now();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.upsertInstantDiscount(
                1L,
                new ProductInstantDiscountRequest(
                        "드롭 오픈 할인",
                        new BigDecimal("3000.00"),
                        now.minusDays(1),
                        now.plusDays(1)
                )
        );

        assertThat(response.instantDiscountAmount()).isEqualByComparingTo("3000.00");
        assertThat(product.getProductDiscount()).isNotNull();
        assertThat(product.getProductDiscount().isActive()).isTrue();
    }

    @Test
    void deactivateInstantDiscountExcludesDiscountFromProductResponse() {
        Product product = productFixture();
        LocalDateTime now = LocalDateTime.now();
        product.applyInstantDiscount("드롭 오픈 할인", new BigDecimal("3000.00"), now.minusDays(1), now.plusDays(1));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.deactivateInstantDiscount(1L);

        assertThat(response.instantDiscountAmount()).isZero();
        assertThat(product.getProductDiscount().isActive()).isFalse();
    }

    @Test
    void getProductThrowsExceptionWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private Product productFixture() {
        Product product = Product.create(
                "달빛 상점 한정판 아트북",
                new BigDecimal("35000.00"),
                ProductStatus.ON_SALE,
                "달빛 상점",
                ContentType.WEBTOON,
                "ARTBOOK",
                "웹툰 달빛 상점의 시즌 1 한정판 아트북"
        );
        Stock stock = Stock.create(product, 100);
        product.registerStock(stock);

        return product;
    }
}
