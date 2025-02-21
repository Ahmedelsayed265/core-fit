package com.corefit.service;

import com.corefit.dto.GeneralResponse;
import com.corefit.dto.ProductDto;
import com.corefit.dto.ProductRequest;
import com.corefit.entity.Market;
import com.corefit.entity.Product;
import com.corefit.entity.SubCategory;
import com.corefit.exceptions.GeneralException;
import com.corefit.repository.MarketRepo;
import com.corefit.repository.ProductRepo;
import com.corefit.repository.SubCategoryRepo;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepo productRepo;
    private final MarketRepo marketRepo;
    private final SubCategoryRepo subCategoryRepo;
    private final FilesService filesService;

    public ProductService(ProductRepo productRepo, MarketRepo marketRepo, SubCategoryRepo subCategoryRepo, FilesService filesService) {
        this.productRepo = productRepo;
        this.marketRepo = marketRepo;
        this.subCategoryRepo = subCategoryRepo;
        this.filesService = filesService;
    }

    public GeneralResponse<?> findById(long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new GeneralException("Product not found"));

        return new GeneralResponse<>("Success", product);
    }

    public Page<Product> getAll(Integer page, Integer size, Long marketId, Long subCategoryId, String name) {
        size = (size == null || size <= 0) ? 5 : size;
        page = (page == null || page < 1) ? 1 : page;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").ascending());
        return productRepo.findAllByFilters(marketId, subCategoryId, name, pageable);
    }


    @Transactional
    public GeneralResponse<?> insert(ProductRequest productRequest, List<MultipartFile> images) {
        Market market = marketRepo.findById(productRequest.getMarketId())
                .orElseThrow(() -> new GeneralException("Market not found"));

        SubCategory subCategory = subCategoryRepo.findById(productRequest.getSubCategoryId())
                .orElseThrow(() -> new GeneralException("Sub category not found"));

        List<String> imageUrls = uploadImages(images);

        Product product = new Product(productRequest.getName(), productRequest.getDescription(), productRequest.getPrice()
                , productRequest.getOffer(), market, subCategory, imageUrls);

        productRepo.save(product);

        return new GeneralResponse<>("Product added successfully", product);
    }

    @Transactional
    public GeneralResponse<?> update(ProductRequest productRequest, List<MultipartFile> images) {
        Product product = productRepo.findById(productRequest.getId())
                .orElseThrow(() -> new GeneralException("Product not found"));

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setOffer(productRequest.getOffer());
        product.setSubCategory(subCategoryRepo.findById(productRequest.getSubCategoryId())
                .orElseThrow(() -> new GeneralException("Sub category not found")));

        if (images != null && !images.isEmpty()) {
            deleteImages(product.getImages());
            product.setImages(uploadImages(images));
        }

        productRepo.save(product);

        return new GeneralResponse<>("Product updated successfully", product);
    }

    @Transactional
    public GeneralResponse<?> delete(long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new GeneralException("Product not found"));

        deleteImages(product.getImages());
        productRepo.deleteById(id);

        return new GeneralResponse<>("Product deleted successfully");
    }

    @Transactional
    public GeneralResponse<?> changeStatus(long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new GeneralException("Product not found"));

        product.setHidden(!product.isHidden());
        productRepo.save(product);

        return new GeneralResponse<>("Product status changed successfully");
    }


    // Helper methods
    private ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getOffer(),
                product.getSubCategory().getName(),
                product.getMarket().getName(),
                product.getImages(),
                product.isHidden());
    }

    private List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }
        return images.stream().map(image -> {
            try {
                return filesService.saveImage(image);
            } catch (IOException e) {
                throw new GeneralException("Failed to upload image: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    private void deleteImages(List<String> images) {
        if (images != null && !images.isEmpty()) {
            images.forEach(image -> {
                try {
                    filesService.deleteImage(image);
                } catch (IOException e) {
                    throw new GeneralException("Failed to delete image: " + e.getMessage());
                }
            });
        }
    }
}
