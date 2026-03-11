package com.auctiondemo.backend.repository;

import com.auctiondemo.backend.entity.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {
    List<AuctionItem> findByStatus(Integer status);
}
