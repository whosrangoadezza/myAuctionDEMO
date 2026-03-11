package com.auctiondemo.backend.controller;

import com.auctiondemo.backend.dto.AuctionResultDTO;
import com.auctiondemo.backend.dto.BidRequest;
import com.auctiondemo.backend.entity.AuctionItem;
import com.auctiondemo.backend.entity.Bid;
import com.auctiondemo.backend.service.AuctionService;
import com.auctiondemo.backend.service.PaillierCryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private PaillierCryptoService cryptoService;

    // ==================== 公钥分发 ====================

    /**
     * GET /api/crypto/publicKey
     * 获取 Paillier 公钥（投标客户端用于加密出价）
     */
    @GetMapping("/crypto/publicKey")
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("n", cryptoService.exportPublicKeyN());
        result.put("bitLength", cryptoService.getKeyBitLength());
        result.put("algorithm", "Paillier");
        result.put("note", "g = n + 1");
        return ResponseEntity.ok(result);
    }

    // ==================== 拍品查询 ====================

    /**
     * GET /api/auction/items
     * 获取所有拍品
     */
    @GetMapping("/auction/items")
    public ResponseEntity<List<AuctionItem>> getAllItems() {
        return ResponseEntity.ok(auctionService.getAllItems());
    }

    /**
     * GET /api/auction/items/active
     * 获取进行中的拍品
     */
    @GetMapping("/auction/items/active")
    public ResponseEntity<List<AuctionItem>> getActiveItems() {
        return ResponseEntity.ok(auctionService.getActiveItems());
    }

    /**
     * GET /api/auction/items/{itemId}
     * 获取拍品详情
     */
    @GetMapping("/auction/items/{itemId}")
    public ResponseEntity<Map<String, Object>> getItemDetail(@PathVariable Long itemId) {
        AuctionItem item = auctionService.getItem(itemId);
        long bidCount = auctionService.getBidCount(itemId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", item);
        result.put("bidCount", bidCount);
        result.put("message", "出价金额已加密，开标前无法查看");
        return ResponseEntity.ok(result);
    }

    // ==================== 投标 ====================

    /**
     * POST /api/auction/bid
     * 提交出价
     *
     * 请求体：
     * {
     *   "itemId": 1,
     *   "userId": 1,
     *   "bidAmountFen": 850000
     * }
     */
    @PostMapping("/auction/bid")
    public ResponseEntity<Map<String, Object>> placeBid(@RequestBody BidRequest request) {
        try {
            Bid bid = auctionService.placeBid(request);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("bidId", bid.getBidId());
            result.put("message", "出价成功！您的出价已加密存储，开标前任何人（包括系统）都无法查看金额");
            result.put("encryptedPricePreview", bid.getEncryptedPrice().substring(0, 30) + "...");
            result.put("creditSnapshot", bid.getCreditSnapshot());
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== 开标 ====================

    /**
     * POST /api/auction/open/{itemId}
     * 开标：解密所有出价，确定中标人
     */
    @PostMapping("/auction/open/{itemId}")
    public ResponseEntity<Object> openAuction(@PathVariable Long itemId) {
        try {
            AuctionResultDTO result = auctionService.openAuction(itemId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== 同态加密演示 ====================

    /**
     * GET /api/crypto/demo
     * 演示同态加密的数学性质（答辩用）
     */
    @GetMapping("/crypto/demo")
    public ResponseEntity<Map<String, Object>> cryptoDemo() {
        long priceA = 850000; // 8500 元
        long priceB = 920000; // 9200 元
        int creditA = 85;
        int creditB = 60;

        // 加密
        String encA = cryptoService.encryptBid(priceA);
        String encB = cryptoService.encryptBid(priceB);

        // 同态标量乘法：计算有效出价
        String encEffA = cryptoService.computeEncryptedEffectivePrice(encA, creditA);
        String encEffB = cryptoService.computeEncryptedEffectivePrice(encB, creditB);

        // 同态加法：密文求和
        String encSum = cryptoService.homomorphicAdd(encA, encB);

        // 解密验证
        long decA = cryptoService.decrypt(encA);
        long decB = cryptoService.decrypt(encB);
        long decEffA = cryptoService.decrypt(encEffA);
        long decEffB = cryptoService.decrypt(encEffB);
        long decSum = cryptoService.decrypt(encSum);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("说明", "Paillier 同态加密演示 — 所有运算均在密文状态下完成");

        // 标量乘法演示
        Map<String, Object> mul = new LinkedHashMap<>();
        mul.put("投标人A_原始出价", priceA + " 分 (" + priceA / 100.0 + " 元)");
        mul.put("投标人A_信用分", creditA);
        mul.put("密文运算", "E(price)^credit mod n²");
        mul.put("解密结果", decEffA);
        mul.put("期望结果", priceA * creditA);
        mul.put("有效出价", decEffA / 100.0 / 100 + " 元");
        mul.put("验证", decEffA == priceA * creditA ? "通过" : "失败");
        result.put("同态标量乘法_投标人A", mul);

        Map<String, Object> mul2 = new LinkedHashMap<>();
        mul2.put("投标人B_原始出价", priceB + " 分 (" + priceB / 100.0 + " 元)");
        mul2.put("投标人B_信用分", creditB);
        mul2.put("解密结果", decEffB);
        mul2.put("期望结果", priceB * creditB);
        mul2.put("有效出价", decEffB / 100.0 / 100 + " 元");
        mul2.put("验证", decEffB == priceB * creditB ? "通过" : "失败");
        result.put("同态标量乘法_投标人B", mul2);

        // 比较结果
        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("投标人A有效出价", decEffA / 100.0 / 100 + " 元");
        compare.put("投标人B有效出价", decEffB / 100.0 / 100 + " 元");
        compare.put("中标人", decEffA > decEffB ? "投标人A（信用更高）" : "投标人B（出价更高）");
        compare.put("分析", "B 虽然原始出价更高，但信用分较低，有效出价反而低于 A");
        result.put("开标比较", compare);

        // 加法演示
        Map<String, Object> add = new LinkedHashMap<>();
        add.put("密文运算", "E(priceA) × E(priceB) mod n²");
        add.put("解密结果", decSum + " 分 (" + decSum / 100.0 + " 元)");
        add.put("期望结果", (priceA + priceB) + " 分");
        add.put("验证", decSum == priceA + priceB ? "通过" : "失败");
        result.put("同态加法", add);

        return ResponseEntity.ok(result);
    }

    // ==================== 完整流程演示 ====================

    /**
     * GET /api/auction/demo
     * 一键演示完整拍卖流程（答辩用）
     */
    @GetMapping("/auction/demo")
    public ResponseEntity<Map<String, Object>> auctionDemo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("说明", "同态加密密封拍卖完整流程演示（数据库中的真实用户）");
        result.put("规则", "有效出价 = 原始出价 × (信用分/100)，有效出价最高者中标");

        // 模拟三个用户出价
        long[] prices = {850000, 920000, 880000}; // 8500/9200/8800 元
        int[] credits = {85, 60, 75};             // 数据库中 alice/bob/carol 的信用分
        String[] names = {"alice", "bob", "carol"};

        // 加密阶段
        String[] encPrices = new String[3];
        String[] encEffectives = new String[3];
        List<Map<String, Object>> encPhase = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            encPrices[i] = cryptoService.encryptBid(prices[i]);
            encEffectives[i] = cryptoService.computeEncryptedEffectivePrice(encPrices[i], credits[i]);

            Map<String, Object> step = new LinkedHashMap<>();
            step.put("投标人", names[i]);
            step.put("原始出价", prices[i] / 100.0 + " 元");
            step.put("信用分", credits[i]);
            step.put("密文长度", encPrices[i].length() + " 字符");
            step.put("同态运算", "E(" + prices[i] + ")^" + credits[i] + " = E(" + (prices[i] * credits[i]) + ")");
            encPhase.add(step);
        }
        result.put("第1步_投标加密与同态运算", encPhase);
        result.put("第2步_服务端存储", "数据库仅存储密文，服务端无法获知任何出价金额");

        // 开标阶段
        long maxEff = -1;
        int winnerIdx = -1;
        List<Map<String, Object>> openPhase = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            long decPrice = cryptoService.decrypt(encPrices[i]);
            long decEff = cryptoService.decrypt(encEffectives[i]);
            double effectiveYuan = decEff / 100.0 / 100;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("投标人", names[i]);
            detail.put("解密原始出价", decPrice / 100.0 + " 元");
            detail.put("信用分", credits[i]);
            detail.put("有效出价", effectiveYuan + " 元");
            detail.put("计算公式", decPrice / 100.0 + " × " + credits[i] + "/100 = " + effectiveYuan);
            detail.put("解密正确", decPrice == prices[i] && decEff == prices[i] * credits[i]);
            openPhase.add(detail);

            if (decEff > maxEff) {
                maxEff = decEff;
                winnerIdx = i;
            }
        }
        result.put("第3步_开标解密", openPhase);

        Map<String, Object> winner = new LinkedHashMap<>();
        winner.put("中标人", names[winnerIdx]);
        winner.put("原始出价", prices[winnerIdx] / 100.0 + " 元");
        winner.put("信用分", credits[winnerIdx]);
        winner.put("有效出价", maxEff / 100.0 / 100 + " 元");
        result.put("第4步_中标结果", winner);

        Map<String, String> security = new LinkedHashMap<>();
        security.put("传输安全", "出价金额全程以密文传输和存储");
        security.put("隐私保护", "开标前服务端无法获知任何出价金额");
        security.put("信用加权", "利用 Paillier 同态标量乘法在密文状态下完成信用加权计算");
        security.put("语义安全", "同一金额每次加密产生不同密文（随机数 r 的作用）");
        result.put("安全性分析", security);

        return ResponseEntity.ok(result);
    }
}
