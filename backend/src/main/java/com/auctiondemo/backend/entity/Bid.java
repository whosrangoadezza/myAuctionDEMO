package com.auctiondemo.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long bidId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 加密的原始出价（Paillier 密文 Base64）
     */
    @Column(name = "encrypted_price", nullable = false, columnDefinition = "TEXT")
    private String encryptedPrice;

    /**
     * 加密的有效出价（= 原始出价 × 信用分，Paillier 同态标量乘法计算）
     * 开标时解密此字段再除以 100 即为有效出价
     */
    @Column(name = "encrypted_effective_price", nullable = false, columnDefinition = "TEXT")
    private String encryptedEffectivePrice;

    /**
     * 出价时的信用分快照（明文保存，用于审计追溯）
     */
    @Column(name = "credit_snapshot", nullable = false)
    private Integer creditSnapshot;

    @Column(name = "bid_time")
    private LocalDateTime bidTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private AuctionItem auctionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public Bid() {}

    // ===== Getters & Setters =====

    public Long getBidId() { return bidId; }
    public void setBidId(Long bidId) { this.bidId = bidId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEncryptedPrice() { return encryptedPrice; }
    public void setEncryptedPrice(String encryptedPrice) { this.encryptedPrice = encryptedPrice; }

    public String getEncryptedEffectivePrice() { return encryptedEffectivePrice; }
    public void setEncryptedEffectivePrice(String encryptedEffectivePrice) { this.encryptedEffectivePrice = encryptedEffectivePrice; }

    public Integer getCreditSnapshot() { return creditSnapshot; }
    public void setCreditSnapshot(Integer creditSnapshot) { this.creditSnapshot = creditSnapshot; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public AuctionItem getAuctionItem() { return auctionItem; }
    public void setAuctionItem(AuctionItem auctionItem) { this.auctionItem = auctionItem; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
