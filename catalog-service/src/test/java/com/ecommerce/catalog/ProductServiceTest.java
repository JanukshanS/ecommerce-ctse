package com.ecommerce.catalog;

import com.ecommerce.catalog.dto.ProductRequest;
import com.ecommerce.catalog.dto.ProductResponse;
import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.catalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductService}.
 *
 * <p>All dependencies are mocked with Mockito so that tests run entirely
 * in memory without requiring a running MongoDB instance.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // -----------------------------------------------------------------------
    // Shared fixtures
    // -----------------------------------------------------------------------

    private static final String PRODUCT_ID  = "prod-001";
    private static final String SELLER_ID   = "seller-abc";
    private static final String OTHER_SELLER = "seller-xyz";

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Test Widget")
                .description("A test widget for unit tests")
                .price(19.99)
                .category("Widgets")
                .stock(100)
                .sellerId(SELLER_ID)
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        sampleRequest = new ProductRequest();
        sampleRequest.setName("Test Widget");
        sampleRequest.setDescription("A test widget for unit tests");
        sampleRequest.setPrice(19.99);
        sampleRequest.setCategory("Widgets");
        sampleRequest.setStock(100);
    }

    // -----------------------------------------------------------------------
    // createProduct
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should persist and return a new product when name is unique")
        void createProduct_success() {
            // arrange
            when(productRepository.existsByName("Test Widget")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

            // act
            ProductResponse response = productService.createProduct(sampleRequest, SELLER_ID);

            // assert — response fields match saved entity
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getName()).isEqualTo("Test Widget");
            assertThat(response.getPrice()).isEqualTo(19.99);
            assertThat(response.getCategory()).isEqualTo("Widgets");
            assertThat(response.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(response.isActive()).isTrue();

            // assert — repository save was called exactly once
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(1)).save(captor.capture());
            Product captured = captor.getValue();
            assertThat(captured.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(captured.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when product name already exists")
        void createProduct_duplicateName_throwsException() {
            // arrange
            when(productRepository.existsByName("Test Widget")).thenReturn(true);

            // act & assert
            assertThatThrownBy(() -> productService.createProduct(sampleRequest, SELLER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test Widget");

            // repository save must never be called when name is duplicate
            verify(productRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // getProductById
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return the product when it exists")
        void getProductById_found() {
            // arrange
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act
            ProductResponse response = productService.getProductById(PRODUCT_ID);

            // assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getName()).isEqualTo("Test Widget");
            assertThat(response.getStock()).isEqualTo(100);
            verify(productRepository, times(1)).findById(PRODUCT_ID);
        }

        @Test
        @DisplayName("should throw RuntimeException when product does not exist")
        void getProductById_notFound() {
            // arrange
            when(productRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> productService.getProductById("nonexistent-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("nonexistent-id");

            verify(productRepository, times(1)).findById("nonexistent-id");
        }
    }

    // -----------------------------------------------------------------------
    // getAllProducts
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return a page of active products")
        void getAllProducts_returnsPage() {
            // arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(sampleProduct), pageable, 1);
            when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

            // act
            Page<ProductResponse> result = productService.getAllProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(PRODUCT_ID);
        }
    }

    // -----------------------------------------------------------------------
    // getProductsByCategory
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getProductsByCategory")
    class GetProductsByCategory {

        @Test
        @DisplayName("should return a page of products filtered by category and active=true")
        void getProductsByCategory_success() {
            // arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(sampleProduct), pageable, 1);
            when(productRepository.findByCategoryAndActive("Widgets", true, pageable))
                    .thenReturn(productPage);

            // act
            Page<ProductResponse> result = productService.getProductsByCategory("Widgets", pageable);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Widgets");
        }
    }

    // -----------------------------------------------------------------------
    // updateProduct
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update and return the product when seller is the owner")
        void updateProduct_success() {
            // arrange
            ProductRequest updateRequest = new ProductRequest();
            updateRequest.setName("Updated Widget");
            updateRequest.setDescription("Updated description");
            updateRequest.setPrice(24.99);
            updateRequest.setCategory("Gadgets");
            updateRequest.setStock(50);

            Product updatedProduct = Product.builder()
                    .id(PRODUCT_ID)
                    .name("Updated Widget")
                    .description("Updated description")
                    .price(24.99)
                    .category("Gadgets")
                    .stock(50)
                    .sellerId(SELLER_ID)
                    .active(true)
                    .createdAt(sampleProduct.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            // act
            ProductResponse response = productService.updateProduct(PRODUCT_ID, updateRequest, SELLER_ID);

            // assert
            assertThat(response.getName()).isEqualTo("Updated Widget");
            assertThat(response.getPrice()).isEqualTo(24.99);
            assertThat(response.getCategory()).isEqualTo("Gadgets");
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw RuntimeException when product is not found")
        void updateProduct_productNotFound() {
            // arrange
            when(productRepository.findById("missing-id")).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() ->
                    productService.updateProduct("missing-id", sampleRequest, SELLER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("missing-id");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException when seller is not the owner")
        void updateProduct_unauthorisedSeller() {
            // arrange
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act & assert
            assertThatThrownBy(() ->
                    productService.updateProduct(PRODUCT_ID, sampleRequest, OTHER_SELLER))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(OTHER_SELLER);

            verify(productRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // deleteProduct
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should soft-delete the product when seller is the owner")
        void deleteProduct_success() {
            // arrange
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

            // act
            productService.deleteProduct(PRODUCT_ID, SELLER_ID);

            // assert — product was saved with active=false
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw RuntimeException when product is not found")
        void deleteProduct_productNotFound() {
            // arrange
            when(productRepository.findById("missing-id")).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() ->
                    productService.deleteProduct("missing-id", SELLER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("missing-id");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw RuntimeException when seller is not the owner")
        void deleteProduct_unauthorisedSeller() {
            // arrange
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act & assert
            assertThatThrownBy(() ->
                    productService.deleteProduct(PRODUCT_ID, OTHER_SELLER))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(OTHER_SELLER);

            verify(productRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // checkStock
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkStock")
    class CheckStock {

        @Test
        @DisplayName("should return true when requested quantity is within available stock")
        void checkStock_sufficient() {
            // arrange — sampleProduct has stock = 100
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act
            boolean result = productService.checkStock(PRODUCT_ID, 50);

            // assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when requested quantity exceeds available stock")
        void checkStock_insufficient() {
            // arrange — sampleProduct has stock = 100
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act
            boolean result = productService.checkStock(PRODUCT_ID, 150);

            // assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when quantity exactly equals available stock")
        void checkStock_exactMatch() {
            // arrange — sampleProduct has stock = 100
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(sampleProduct));

            // act
            boolean result = productService.checkStock(PRODUCT_ID, 100);

            // assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should throw RuntimeException when product is not found")
        void checkStock_productNotFound() {
            // arrange
            when(productRepository.findById("missing-id")).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> productService.checkStock("missing-id", 1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("missing-id");
        }

        @Test
        @DisplayName("should throw RuntimeException when product is inactive")
        void checkStock_inactiveProduct() {
            // arrange — create an inactive product
            Product inactive = Product.builder()
                    .id(PRODUCT_ID)
                    .name("Inactive Widget")
                    .description("This product is inactive")
                    .price(9.99)
                    .category("Widgets")
                    .stock(50)
                    .sellerId(SELLER_ID)
                    .active(false)
                    .build();

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inactive));

            // act & assert
            assertThatThrownBy(() -> productService.checkStock(PRODUCT_ID, 1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(PRODUCT_ID);
        }
    }
}
