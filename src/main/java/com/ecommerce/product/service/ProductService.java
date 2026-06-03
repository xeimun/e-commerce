package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductStatusUpdateRequest;
import com.ecommerce.product.dto.ProductStockUpdateRequest;
import com.ecommerce.product.dto.ProductUpdateRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Stock;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAllByOrderByIdAsc()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(Long productId) {
        Product product = findProduct(productId);

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = Product.create(
                request.name(),
                request.price(),
                request.status(),
                request.contentTitle(),
                request.contentType(),
                request.category(),
                request.description()
        );
        Stock stock = Stock.create(product, request.stockQuantity());
        product.registerStock(stock);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findProduct(productId);
        product.update(
                request.name(),
                request.price(),
                request.contentTitle(),
                request.contentType(),
                request.category(),
                request.description()
        );

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, ProductStatusUpdateRequest request) {
        Product product = findProduct(productId);
        product.changeStatus(request.status());

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStock(Long productId, ProductStockUpdateRequest request) {
        Product product = findProduct(productId);
        product.getStock().setQuantity(request.stockQuantity());

        return ProductResponse.from(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
