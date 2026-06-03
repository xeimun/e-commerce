package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductStatusUpdateRequest;
import com.ecommerce.product.dto.ProductStockUpdateRequest;
import com.ecommerce.product.dto.ProductUpdateRequest;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(productId, request);
    }

    @PatchMapping("/{productId}/status")
    public ProductResponse updateProductStatus(@PathVariable Long productId, @Valid @RequestBody ProductStatusUpdateRequest request) {
        return productService.updateStatus(productId, request);
    }

    @PatchMapping("/{productId}/stock")
    public ProductResponse updateProductStock(@PathVariable Long productId, @Valid @RequestBody ProductStockUpdateRequest request) {
        return productService.updateStock(productId, request);
    }
}
