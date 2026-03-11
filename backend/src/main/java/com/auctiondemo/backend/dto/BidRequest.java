package com.auctiondemo.backend.dto;

/**
 * 出价请求 DTO
 */
public class BidRequest {

    /** 拍品 ID */
    private Long itemId;

    /** 用户 ID */
    private Long userId;

    /** 出价金额（单位：分） */
    private Long bidAmountFen;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBidAmountFen() { return bidAmountFen; }
    public void setBidAmountFen(Long bidAmountFen) { this.bidAmountFen = bidAmountFen; }
}
