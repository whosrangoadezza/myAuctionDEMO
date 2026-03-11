package com.auctiondemo.backend.service;

import com.auctiondemo.backend.dto.AuctionResultDTO;
import com.auctiondemo.backend.dto.AuctionResultDTO.BidDetail;
import com.auctiondemo.backend.dto.BidRequest;
import com.auctiondemo.backend.entity.AuctionItem;
import com.auctiondemo.backend.entity.Bid;
import com.auctiondemo.backend.entity.User;
import com.auctiondemo.backend.entity.UserCredit;
import com.auctiondemo.backend.repository.AuctionItemRepository;
import com.auctiondemo.backend.repository.BidRepository;
import com.auctiondemo.backend.repository.UserCreditRepository;
import com.auctiondemo.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    @Autowired
    private PaillierCryptoService cryptoService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionItemRepository auctionItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCreditRepository userCreditRepository;

    // ==================== 投标 ====================

    /**
     * 提交出价（核心流程）
     *
     * 流程：
     * 1. 校验拍品状态和用户
     * 2. 查询用户信用分
     * 3. 用 Paillier 公钥加密原始出价 → encrypted_price
     * 4. 用同态标量乘法计算 E(price × credit) → encrypted_effective_price
     * 5. 存入数据库（全程密文，服务端不保留明文）
     */
    @Transactional
    public Bid placeBid(BidRequest request) {
        Long itemId = request.getItemId();
        Long userId = request.getUserId();
        Long bidAmountFen = request.getBidAmountFen();

        // 1. 校验拍品
        AuctionItem item = auctionItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("拍品不存在: " + itemId));

        if (item.getStatus() != 1) {
            throw new RuntimeException("拍品不在竞拍中，当前状态: " + item.getStatus());
        }

        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new RuntimeException("拍卖已结束");
        }

        // 2. 校验用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        // 3. 校验出价金额
        if (bidAmountFen <= 0) {
            throw new RuntimeException("出价金额必须大于0");
        }
        if (bidAmountFen < item.getStartPrice()) {
            throw new RuntimeException("出价不能低于起拍价: " + item.getStartPrice() + " 分");
        }

        // 4. 检查是否重复出价（密封拍卖每人只能出价一次）
        if (bidRepository.existsByItemIdAndUserId(itemId, userId)) {
            throw new RuntimeException("您已经对该拍品出过价了");
        }

        // 5. 查询信用分
        UserCredit credit = userCreditRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户信用记录不存在"));
        int creditScore = credit.getTotalScore();

        log.info("【投标】用户 {} 对拍品 {} 出价，信用分 {}", user.getUsername(), itemId, creditScore);

        // ===== 核心：Paillier 同态加密 =====

        // 6. 加密原始出价：E(bidAmount)
        String encryptedPrice = cryptoService.encryptBid(bidAmountFen);
        log.info("【加密】原始出价已加密，密文长度: {} 字符", encryptedPrice.length());

        // 7. 同态标量乘法：E(bidAmount)^creditScore = E(bidAmount × creditScore)
        //    不需要解密原始出价！直接在密文上运算！
        String encryptedEffectivePrice = cryptoService.computeEncryptedEffectivePrice(
                encryptedPrice, creditScore);
        log.info("【同态运算】有效出价已在密文状态下计算完成");

        // 8. 保存到数据库（只有密文，没有明文）
        Bid bid = new Bid();
        bid.setItemId(itemId);
        bid.setUserId(userId);
        bid.setEncryptedPrice(encryptedPrice);
        bid.setEncryptedEffectivePrice(encryptedEffectivePrice);
        bid.setCreditSnapshot(creditScore);
        bid.setBidTime(LocalDateTime.now());

        bidRepository.save(bid);

        // 9. 更新拍品出价计数
        item.setBidCount((item.getBidCount() == null ? 0 : item.getBidCount()) + 1);
        auctionItemRepository.save(item);

        log.info("【投标成功】出价 ID: {}，密文已存储，明文已销毁", bid.getBidId());
        return bid;
    }

    // ==================== 开标 ====================

    /**
     * 开标：解密所有出价，确定中标人
     *
     * 流程：
     * 1. 查出该拍品的所有密文出价
     * 2. 用私钥逐一解密 encrypted_effective_price
     * 3. 比较有效出价（price × credit / 100），最高者中标
     * 4. 更新拍品状态为"已结束"
     * 5. 返回完整开标结果
     */
    @Transactional
    public AuctionResultDTO openAuction(Long itemId) {
        // 1. 校验拍品
        AuctionItem item = auctionItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("拍品不存在: " + itemId));

        // 2. 查出所有出价
        List<Bid> bids = bidRepository.findByItemIdOrderByBidTimeAsc(itemId);
        if (bids.isEmpty()) {
            throw new RuntimeException("该拍品无人出价");
        }

        log.info("========== 开标：拍品 [{}] {} ==========", itemId, item.getTitle());
        log.info("共 {} 条出价，开始解密...", bids.size());

        // 3. 逐一解密，找出最高有效出价
        long maxEffective = -1;
        int winnerIndex = -1;
        List<BidDetail> details = new ArrayList<>();

        for (int i = 0; i < bids.size(); i++) {
            Bid bid = bids.get(i);
            User bidder = userRepository.findById(bid.getUserId()).orElse(null);
            String username = bidder != null ? bidder.getUsername() : "未知用户";

            // 解密原始出价
            long originalPrice = cryptoService.decryptOriginalPrice(bid.getEncryptedPrice());

            // 解密有效出价（值 = originalPrice × creditScore）
            long effectiveRaw = cryptoService.decryptEffectivePrice(bid.getEncryptedEffectivePrice());

            log.info("  投标人 [{}]: 原始出价={} 分({}元), 信用分={}, " +
                            "有效出价原始值={}, 有效出价={} 分({}元)",
                    username, originalPrice, originalPrice / 100.0,
                    bid.getCreditSnapshot(),
                    effectiveRaw, effectiveRaw / 100, effectiveRaw / 100.0 / 100);

            // 构造详情
            BidDetail detail = new BidDetail();
            detail.setUserId(bid.getUserId());
            detail.setUsername(username);
            detail.setOriginalPriceFen(originalPrice);
            detail.setOriginalPriceYuan(originalPrice / 100.0);
            detail.setCreditScore(bid.getCreditSnapshot());
            detail.setEffectivePriceFen(effectiveRaw); // price × credit
            detail.setEffectivePriceYuan(effectiveRaw / 100.0 / 100); // 除以100得到真正的有效出价(元)
            details.add(detail);

            // 比较：effectiveRaw = price × credit，值越大越好
            if (effectiveRaw > maxEffective) {
                maxEffective = effectiveRaw;
                winnerIndex = i;
            }
        }

        // 标记中标人
        for (int i = 0; i < details.size(); i++) {
            details.get(i).setWinner(i == winnerIndex);
        }

        // 4. 更新拍品状态
        item.setStatus(2); // 已结束
        auctionItemRepository.save(item);

        // 5. 组装结果
        Bid winnerBid = bids.get(winnerIndex);
        BidDetail winnerDetail = details.get(winnerIndex);

        AuctionResultDTO result = new AuctionResultDTO();
        result.setItemId(itemId);
        result.setItemTitle(item.getTitle());
        result.setWinnerId(winnerBid.getUserId());
        result.setWinnerName(winnerDetail.getUsername());
        result.setWinningPriceFen(winnerDetail.getOriginalPriceFen());
        result.setWinningPriceYuan(winnerDetail.getOriginalPriceYuan());
        result.setWinningEffectiveFen(maxEffective);
        result.setWinningEffectiveYuan(maxEffective / 100.0 / 100);
        result.setWinnerCredit(winnerBid.getCreditSnapshot());
        result.setAllBids(details);
        result.setTotalBidders(bids.size());

        log.info("========== 开标结果 ==========");
        log.info("中标人: {}，原始出价: {} 元，信用分: {}，有效出价: {} 元",
                winnerDetail.getUsername(),
                winnerDetail.getOriginalPriceYuan(),
                winnerBid.getCreditSnapshot(),
                winnerDetail.getEffectivePriceYuan());

        return result;
    }

    // ==================== 查询 ====================

    /** 获取所有拍品 */
    public List<AuctionItem> getAllItems() {
        return auctionItemRepository.findAll();
    }

    /** 获取进行中的拍品 */
    public List<AuctionItem> getActiveItems() {
        return auctionItemRepository.findByStatus(1);
    }

    /** 获取拍品详情 */
    public AuctionItem getItem(Long itemId) {
        return auctionItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("拍品不存在"));
    }

    /** 获取拍品的出价数量（不泄露金额） */
    public long getBidCount(Long itemId) {
        return bidRepository.countByItemId(itemId);
    }
}
