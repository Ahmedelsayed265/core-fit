package com.corefit.service;

import com.corefit.dto.GeneralResponse;
import com.corefit.dto.ProductDto;
import com.corefit.dto.ProductRequest;
import com.corefit.entity.*;
import com.corefit.enums.UserType;
import com.corefit.exceptions.GeneralException;
import com.corefit.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private MarketRepo marketRepo;
    @Autowired
    private SubCategoryRepo subCategoryRepo;
    @Autowired
    private FilesService filesService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private FavouritesRepo favouritesRepo;

    public GeneralResponse<?> findById(long id, HttpServletRequest httpRequest) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new GeneralException("Product not found"));

        long userId = Long.parseLong(authService.extractUserIdFromRequest(httpRequest));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GeneralException("User not found"));

        boolean isFavourite = false;
        if (user.getType() == UserType.GENERAL) {
            Optional<Favourites> favouritesOptional = favouritesRepo.findByUser_Id(userId);
            if (favouritesOptional.isPresent()) {
                isFavourite = favouritesOptional.get().getProducts()
                        .stream()
                        .anyMatch(favProduct -> favProduct.getId() == id);
            }
        }

        product.setFavourite(isFavourite);

        return new GeneralResponse<>("Success", product);
    }


    public Page<ProductDto> getAll(Integer page, Integer size, Long marketId, Long subCategoryId, String name, HttpServletRequest httpRequest) {
        size = (size == null || size <= 0) ? 5 : size;
        page = (page == null || page < 1) ? 1 : page;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").ascending());
        Page<Product> productsPage = productRepo.findAllByFilters(marketId, subCategoryId, name, pageable);

        long userId = -1;
        Set<Long> favouriteProductIds = new HashSet<>();

        try {
            userId = Long.parseLong(authService.extractUserIdFromRequest(httpRequest));

            long finalUserId = userId;
            userRepo.findById(userId).ifPresent(user -> {
                if (user.getType() == UserType.GENERAL) {
                    favouritesRepo.findByUser_Id(finalUserId).ifPresent(favourites -> {
                        favouriteProductIds.addAll(
                                favourites.getProducts().stream()
                                        .map(Product::getId)
                                        .collect(Collectors.toSet())
                        );
                    });
                }
            });

        } catch (Exception e) {
            System.out.println("Error retrieving user favorites: " + e.getMessage());
        }

        Set<Long> finalFavouriteProductIds = favouriteProductIds;

        return productsPage.map(product -> {
            ProductDto productDto = mapToDto(product);
            productDto.setFavourite(finalFavouriteProductIds.contains(product.getId()));
            return productDto;
        });
    }


    @Transactional
    public GeneralResponse<?> insert(ProductRequest productRequest, List<MultipartFile> images) {
        Market market = marketRepo.findById(productRequest.getMarketId())
                .orElseThrow(() -> new GeneralException("Market not found"));

        SubCategory subCategory = subCategoryRepo.findById(productRequest.getSubCategoryId())
                .orElseThrow(() -> new GeneralException("Sub category not found"));

        List<String> imageUrls = uploadImages(images);

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .offer(productRequest.getOffer())
                .price(productRequest.getPrice())
                .market(market)
                .subCategory(subCategory)
                .images(imageUrls)
                .build();

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
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .offer(product.getOffer())
                .subCategoryName(product.getSubCategory().getName())
                .marketName(product.getMarket().getName())
                .images(product.getImages())
                .isHidden(product.isHidden())
                .isFavourite(product.isFavourite())
                .build();
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
