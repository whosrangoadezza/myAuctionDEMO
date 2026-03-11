package com.auctiondemo.backend.repository;

import com.auctiondemo.backend.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    /** 查询某拍品的所有出价（按时间排序） */
    List<Bid> findByItemIdOrderByBidTimeAsc(Long itemId);

    /** 查询某用户对某拍品是否已出价 */
    boolean existsByItemIdAndUserId(Long itemId, Long userId);

    /** 查询某拍品的出价数量 */
    long countByItemId(Long itemId);
}
