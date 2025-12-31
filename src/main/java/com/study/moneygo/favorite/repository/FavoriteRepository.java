package com.study.moneygo.favorite.repository;

import com.study.moneygo.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 사용자의 즐겨찾기 목록 조회
    @Query("SELECT f FROM Favorite f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserId(@Param("userId") Long userId);

    // 특정 계좌번호가 즐겨찾기에 있는지 확인
    @Query("SELECT f FROM Favorite f WHERE f.user.id = :userId AND f.accountNumber = :accountNumber")
    Optional<Favorite> findByUserIdAndAccountNumber(
            @Param("userId") Long userId,
            @Param("accountNumber") String accountNumber
    );

    // 즐겨찾기 존재 여부 확인
    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);

    // 즐겨찾기 개수 조회
    long countByUserId(Long userId);
}
