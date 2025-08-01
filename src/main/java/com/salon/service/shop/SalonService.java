package com.salon.service.shop;

import com.salon.dto.management.master.ShopImageDto;

import com.salon.dto.shop.RecommendDesignerDto;
import com.salon.dto.shop.ShopDesignerProfileDto;
import com.salon.dto.shop.ShopListDto;
import com.salon.dto.user.ShopMapDto;

import com.salon.dto.user.ShopRecommendListDto;
import com.salon.entity.Review;
import com.salon.entity.ReviewImage;
import com.salon.entity.management.ShopDesigner;
import com.salon.entity.shop.Shop;

import com.salon.repository.ReviewImageRepo;
import com.salon.repository.ReviewRepo;
import com.salon.repository.management.ShopDesignerRepo;
import com.salon.repository.shop.ShopRepo;
import com.salon.service.management.master.CouponService;

import com.salon.service.user.ReviewService;
import com.salon.util.DateTimeUtil;
import com.salon.util.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SalonService {
// shopService, 미용실 헤어 엔티티를 shopService 로 사용했기 때문에 salonService로 이름 변경.
    private final ShopRepo shopRepo;
    private final ShopImageService shopImageService;
    private final ReviewService reviewService;
    private final CouponService couponService;
    private final ShopDesignerRepo shopDesignerRepo;
    private final ReviewRepo reviewRepo;
    private final ReviewImageRepo reviewImageRepo;



    // 메인 맵에서 지도에 표시되는 거리에 존재하는 샵을 불러오는 메서드

    public List<ShopMapDto> getAllShopsForMap(BigDecimal userLat, BigDecimal userLon) {
        List<Shop> shops = shopRepo.findAll();

        return shops.stream()
                .filter(shop->shop.getLatitude() != null && shop.getLongitude() != null) // null 값 필터
                .map(shop -> {
                    BigDecimal distance = DistanceUtil.calculateDistance(userLat, userLon, shop.getLatitude(), shop.getLongitude()

                    );

                    return new ShopMapDto(
                            shop.getId(),
                            shop.getName(),
                            shop.getLatitude(),
                            shop.getLongitude(),
                            distance
                    );
                })
                .sorted(Comparator.comparing(ShopMapDto::getDistance))
                .toList();
    }

    // 사용자가 있는 지역 기반 샵 불러오기
    public List<ShopListDto> getShopByRegion(String region, BigDecimal userLat, BigDecimal userLon, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Shop> shopPage = shopRepo.findByAddressContaining(region, pageable);

        List<ShopListDto> result = new ArrayList<>();

        for (Shop shop : shopPage.getContent()) {
            ShopImageDto shopImageDto = shopImageService.findThumbnailByShopId(shop.getId());
            boolean hasCoupon = couponService.hasActiveCoupon(shop.getId());


            int reviewCount = reviewService.getReviewCountByShop(shop.getId());
            float avgRating = reviewService.getAverageRatingByShop(shop.getId());

            ShopListDto dto = ShopListDto.from(shop, shopImageDto, avgRating, reviewCount, hasCoupon);

            // 거리 계산
            if (userLat != null && userLon != null &&
                    shop.getLatitude() != null && shop.getLongitude() != null) {

                BigDecimal distance = DistanceUtil.calculateDistance(
                        userLat, userLon,
                        shop.getLatitude(), shop.getLongitude()
                );

                dto.setDistance(distance.setScale(2, RoundingMode.HALF_UP));
            }

            dto.setLatitude(shop.getLatitude());
            dto.setLongitude(shop.getLongitude());

            List<ShopDesigner> designers = shopDesignerRepo.findByShopIdAndIsActiveTrue(shop.getId());

            List<ShopDesignerProfileDto> designerProfileDtos = designers.stream()
                            .map(designer -> {
                                ShopDesignerProfileDto profileDto = new ShopDesignerProfileDto();
                                profileDto .setDesignerId(designer.getDesigner().getId());
                                profileDto .setImgUrl(designer.getDesigner().getImgUrl());
                                return profileDto ;
                            }).toList();
            if (!designerProfileDtos.isEmpty()) {
                dto.setDesignerList(designerProfileDtos);
            }


            result.add(dto);
        }

        result.sort(Comparator.comparing(ShopListDto::getDistance, Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    //추천 샵 불러오기
    public List<ShopRecommendListDto> getRecommendedShops(String region, BigDecimal userLat, BigDecimal userLon) {
        List<Shop> shops = shopRepo.findByAddressContaining(region);
        List<ShopRecommendListDto> recommendList = new ArrayList<>();



        for(Shop shop : shops) {
            int reviewCount = reviewService.getReviewCountByShop(shop.getId());
            float avgRating = reviewService.getAverageRatingByShop(shop.getId());
            ShopRecommendListDto dto = new ShopRecommendListDto();

            if (userLat != null && userLon != null &&
                    shop.getLatitude() != null && shop.getLongitude() != null) {

                BigDecimal distance = DistanceUtil.calculateDistance(
                        userLat, userLon,
                        shop.getLatitude(), shop.getLongitude()
                );

                dto.setDistance(distance.setScale(2, RoundingMode.HALF_UP));
            }


            ShopImageDto shopImageDto = shopImageService.findThumbnailByShopId(shop.getId());

            dto.setShopImageDto(shopImageDto);
            dto.setId(shop.getId());
            dto.setShopName(shop.getName());
            dto.setAvgRating(Math.round(avgRating * 10)/10f);
            dto.setReviewCount(reviewCount);

            recommendList.add(dto);

        }

        // 정렬
        Collections.sort(recommendList, new Comparator<ShopRecommendListDto>() {
            @Override
            public int compare(ShopRecommendListDto a, ShopRecommendListDto b) {
                int ratingCompare = Float.compare(b.getAvgRating(), a.getAvgRating());
                if (ratingCompare != 0) return ratingCompare;
                return Integer.compare(b.getReviewCount(), a.getReviewCount());
            }
        });

        // 최대 8개까지만 반환
        if (recommendList.size() > 8) {
            return recommendList.subList(0, 8);
        }

        return recommendList;



    }


    // 디자이너 추천


    public List<RecommendDesignerDto> getRecommendedDesignersByRegion(String region) {
        List<Shop> shopList = shopRepo.findByAddressContaining(region);
        if (shopList.isEmpty()) return Collections.emptyList();

        List<RecommendDesignerDto> result = new ArrayList<>();

        for (Shop shop : shopList) {
            List<ShopDesigner> designerList = shopDesignerRepo.findByShopIdAndIsActiveTrue(shop.getId());

            for (ShopDesigner sd : designerList) {
                Long designerId = sd.getDesigner().getId();

                // 1. 평균 평점
                Float avgRating = reviewRepo.findAvgRatingByDesignerId(designerId);
                // 2. 리뷰 수
                int reviewCount = reviewRepo.countByReservation_ShopDesigner_Id(designerId);

                RecommendDesignerDto dto = RecommendDesignerDto.from(sd, avgRating != null ? avgRating : 0f, reviewCount);

                reviewService.getRecentReviewWithImageByDesigner(designerId).ifPresent(review -> {
                    dto.setReviewRating(review.getRating());
                    dto.setCreateAt(DateTimeUtil.getTimeAgo(review.getCreateAt(), false));
                    dto.setComment(review.getComment());


                    reviewService.getFirstImageByReviewId(review.getId()).ifPresent(img -> {
                        dto.setReviewImg(img.getImgUrl());
                    });
                });

                result.add(dto);
            }
        }

        // 리뷰 수 → 평점 순 정렬
        result.sort((a, b) -> {
            int compareRating = Float.compare(b.getRating(), a.getRating());
            if (compareRating != 0) {
                return compareRating;
            } else {
                return b.getReviewCount() - a.getReviewCount();
            }
        });

        return result.size() > 8 ? result.subList(0, 8) : result;
    }


}
