package com.study.moneygo.favorite.service;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.favorite.dto.request.FavoriteRequest;
import com.study.moneygo.favorite.dto.request.FavoriteUpdateRequest;
import com.study.moneygo.favorite.dto.response.FavoriteResponse;
import com.study.moneygo.favorite.entity.Favorite;
import com.study.moneygo.favorite.repository.FavoriteRepository;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_FAVORITES = 50;  // 최대 즐겨찾기 개수

    @Transactional
    public FavoriteResponse addFavorite(FavoriteRequest request) {
        // 현재 사용자 조회
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 본인 계좌인지 확인
        Account myAccount = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        if (myAccount.getAccountNumber().equals(request.getAccountNumber())) {
            throw new IllegalArgumentException("본인 계좌는 즐겨찾기에 추가할 수 없습니다.");
        }

        // 계좌 존재 확인
        Account targetAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다."));

        // 이미 즐겨찾기에 있는지 확인
        if (favoriteRepository.existsByUserIdAndAccountNumber(user.getId(), request.getAccountNumber())) {
            throw new IllegalArgumentException("이미 즐겨찾기에 추가된 계좌입니다.");
        }

        // 최대 개수 확인
        long favoriteCount = favoriteRepository.countByUserId(user.getId());
        if (favoriteCount >= MAX_FAVORITES) {
            throw new IllegalArgumentException("즐겨찾기는 최대 " + MAX_FAVORITES + "개까지 추가할 수 있습니다.");
        }

        // 즐겨찾기 추가
        Favorite favorite = Favorite.builder()
                .user(user)
                .accountNumber(request.getAccountNumber())
                .accountOwnerName(targetAccount.getUser().getName())
                .nickname(request.getNickname())
                .memo(request.getMemo())
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);

        log.info("즐겨찾기 추가: userId={}, accountNumber={}", user.getId(), request.getAccountNumber());

        return FavoriteResponse.of(savedFavorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Favorite> favorites = favoriteRepository.findByUserId(user.getId());
        return favorites.stream()
                .map(FavoriteResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FavoriteResponse getFavoriteDetail(Long favoriteId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기를 찾을 수 없습니다."));

        // 소유권 확인
        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return FavoriteResponse.of(favorite);
    }

    @Transactional
    public FavoriteResponse updateFavorite(Long favoriteId, FavoriteUpdateRequest request) {
        try {
            log.info("즐겨찾기 수정 시작: favoriteId={}", favoriteId);

            String email = getCurrentUserEmail();
            log.info("현재 사용자 이메일: {}", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            log.info("사용자 조회 완료: userId={}", user.getId());

            Favorite favorite = favoriteRepository.findById(favoriteId)
                    .orElseThrow(() -> new IllegalArgumentException("즐겨찾기를 찾을 수 없습니다."));
            log.info("즐겨찾기 조회 완료: favoriteId={}, userId={}", favorite.getId(), favorite.getUser().getId());

            // 소유권 확인
            if (!favorite.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("접근 권한이 없습니다.");
            }
            log.info("소유권 확인 완료");

            // 업데이트 - null이 아닐 때만 업데이트
            if (request.getNickname() != null && !request.getNickname().isEmpty()) {
                log.info("별칭 업데이트: {} -> {}", favorite.getNickname(), request.getNickname());
                favorite.updateNickname(request.getNickname());
            }
            if (request.getMemo() != null && !request.getMemo().isEmpty()) {
                log.info("메모 업데이트: {} -> {}", favorite.getMemo(), request.getMemo());
                favorite.updateMemo(request.getMemo());
            }

            log.info("즐겨찾기 수정 완료: favoriteId={}", favoriteId);

            FavoriteResponse response = FavoriteResponse.of(favorite);
            log.info("Response 생성 완료");

            return response;

        } catch (Exception e) {
            log.error("즐겨찾기 수정 중 오류 발생: favoriteId={}, error={}", favoriteId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteFavorite(Long favoriteId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기를 찾을 수 없습니다."));

        // 소유권 확인
        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        favoriteRepository.delete(favorite);

        log.info("즐겨찾기 삭제: favoriteId={}", favoriteId);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
