package com.auctiondemo.backend.dto;

import java.util.List;

/**
 * 开标结果 DTO
 */
public class AuctionResultDTO {

    private Long itemId;
    private String itemTitle;

    /** 中标人信息 */
    private Long winnerId;
    private String winnerName;
    private Long winningPriceFen;       // 原始出价（分）
    private Long winningEffectiveFen;   // 有效出价×100（分×信用分）
    private Double winningPriceYuan;    // 原始出价（元）
    private Double winningEffectiveYuan;// 有效出价（元）
    private Integer winnerCredit;

    /** 所有出价详情（开标后公开） */
    private List<BidDetail> allBids;
    private Integer totalBidders;

    /** 单条出价详情 */
    public static class BidDetail {
        private Long userId;
        private String username;
        private Long originalPriceFen;      // 原始出价（分）
        private Double originalPriceYuan;   // 原始出价（元）
        private Integer creditScore;        // 信用分
        private Long effectivePriceFen;     // 有效出价×100
        private Double effectivePriceYuan;  // 有效出价（元）
        private boolean isWinner;

        // ===== Getters & Setters =====
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Long getOriginalPriceFen() { return originalPriceFen; }
        public void setOriginalPriceFen(Long originalPriceFen) { this.originalPriceFen = originalPriceFen; }
        public Double getOriginalPriceYuan() { return originalPriceYuan; }
        public void setOriginalPriceYuan(Double originalPriceYuan) { this.originalPriceYuan = originalPriceYuan; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public Long getEffectivePriceFen() { return effectivePriceFen; }
        public void setEffectivePriceFen(Long effectivePriceFen) { this.effectivePriceFen = effectivePriceFen; }
        public Double getEffectivePriceYuan() { return effectivePriceYuan; }
        public void setEffectivePriceYuan(Double effectivePriceYuan) { this.effectivePriceYuan = effectivePriceYuan; }
        public boolean isWinner() { return isWinner; }
        public void setWinner(boolean winner) { isWinner = winner; }
    }

    // ===== Getters & Setters =====
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public Long getWinningPriceFen() { return winningPriceFen; }
    public void setWinningPriceFen(Long winningPriceFen) { this.winningPriceFen = winningPriceFen; }
    public Long getWinningEffectiveFen() { return winningEffectiveFen; }
    public void setWinningEffectiveFen(Long winningEffectiveFen) { this.winningEffectiveFen = winningEffectiveFen; }
    public Double getWinningPriceYuan() { return winningPriceYuan; }
    public void setWinningPriceYuan(Double winningPriceYuan) { this.winningPriceYuan = winningPriceYuan; }
    public Double getWinningEffectiveYuan() { return winningEffectiveYuan; }
    public void setWinningEffectiveYuan(Double winningEffectiveYuan) { this.winningEffectiveYuan = winningEffectiveYuan; }
    public Integer getWinnerCredit() { return winnerCredit; }
    public void setWinnerCredit(Integer winnerCredit) { this.winnerCredit = winnerCredit; }
    public List<BidDetail> getAllBids() { return allBids; }
    public void setAllBids(List<BidDetail> allBids) { this.allBids = allBids; }
    public Integer getTotalBidders() { return totalBidders; }
    public void setTotalBidders(Integer totalBidders) { this.totalBidders = totalBidders; }
}
