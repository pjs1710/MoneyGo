package com.study.moneygo.favorite.controller;

import com.study.moneygo.favorite.dto.request.FavoriteRequest;
import com.study.moneygo.favorite.dto.request.FavoriteUpdateRequest;
import com.study.moneygo.favorite.dto.response.FavoriteResponse;
import com.study.moneygo.favorite.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /*
     즐겨찾기 추가
     */
    @PostMapping
    public ResponseEntity<FavoriteResponse> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        FavoriteResponse response = favoriteService.addFavorite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     내 즐겨찾기 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getMyFavorites() {
        List<FavoriteResponse> favorites = favoriteService.getMyFavorites();
        return ResponseEntity.ok(favorites);
    }

    /*
     즐겨찾기 상세 조회
     */
    @GetMapping("/{favoriteId}")
    public ResponseEntity<FavoriteResponse> getFavoriteDetail(@PathVariable Long favoriteId) {
        FavoriteResponse response = favoriteService.getFavoriteDetail(favoriteId);
        return ResponseEntity.ok(response);
    }

    /*
     즐겨찾기 수정 (별칭, 메모)
     */
    @PatchMapping("/{favoriteId}")
    public ResponseEntity<FavoriteResponse> updateFavorite(
            @PathVariable Long favoriteId,
            @Valid @RequestBody FavoriteUpdateRequest request) {
        FavoriteResponse response = favoriteService.updateFavorite(favoriteId, request);
        return ResponseEntity.ok(response);
    }

    /*
     즐겨찾기 삭제
     */
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> deleteFavorite(@PathVariable Long favoriteId) {
        favoriteService.deleteFavorite(favoriteId);
        return ResponseEntity.noContent().build();
    }
}
