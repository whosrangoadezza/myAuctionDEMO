package com.auctiondemo.backend.service;

import com.paillier.crypto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;

/**
 * Paillier 同态加密服务
 *
 * 调用独立库 paillier-crypto-lib 提供的 API，
 * 在拍卖系统中充当"可信第三方"角色。
 *
 * 核心同态操作：
 *   有效出价 = 原始出价 × 信用分
 *   在密文状态下：E(有效出价) = E(原始出价)^信用分 mod n²
 *   开标解密后：有效出价(元) = decrypt(E(有效出价)) / 100
 */
@Service
public class PaillierCryptoService {

    private static final Logger log = LoggerFactory.getLogger(PaillierCryptoService.class);

    /** 密钥位长：1024 位（毕设演示足够，生产环境用 2048） */
    private static final int KEY_BIT_LENGTH = 1024;

    private PaillierKeyPair keyPair;

    @PostConstruct
    public void init() {
        log.info("========== 生成 Paillier 密钥对（{}位）==========", KEY_BIT_LENGTH);
        long start = System.currentTimeMillis();
        this.keyPair = PaillierUtil.generateKeyPair(KEY_BIT_LENGTH);
        log.info("========== 密钥对生成完毕，耗时 {} ms ==========", System.currentTimeMillis() - start);
    }

    // ==================== 公钥分发 ====================

    public PaillierPublicKey getPublicKey() {
        return keyPair.getPublicKey();
    }

    public String exportPublicKeyN() {
        return PaillierUtil.exportPublicKey(keyPair.getPublicKey());
    }

    public int getKeyBitLength() {
        return KEY_BIT_LENGTH;
    }

    // ==================== 加密 ====================

    /**
     * 加密出价金额
     *
     * @param amountFen 金额（分）
     * @return 密文 Base64 字符串
     */
    public String encryptBid(long amountFen) {
        BigInteger cipher = PaillierUtil.encrypt(amountFen, keyPair.getPublicKey());
        return PaillierUtil.toBase64(cipher);
    }

    // ==================== 同态运算 ====================

    /**
     * 同态标量乘法：计算 E(price × creditScore)
     *
     * 这是本系统的核心！在不解密原始出价的情况下，
     * 利用 Paillier 同态性质计算信用加权后的有效出价。
     *
     * 数学原理：E(m)^k mod n² = E(m × k)
     *
     * @param encryptedPrice 加密的原始出价（Base64）
     * @param creditScore    信用分（0-100 的整数）
     * @return 加密的有效出价（Base64），解密后值为 price × creditScore
     */
    public String computeEncryptedEffectivePrice(String encryptedPrice, int creditScore) {
        BigInteger cipher = PaillierUtil.fromBase64(encryptedPrice);
        BigInteger result = PaillierUtil.multiply(cipher, (long) creditScore, keyPair.getPublicKey());
        return PaillierUtil.toBase64(result);
    }

    /**
     * 同态加法：密文状态下两个出价相加
     */
    public String homomorphicAdd(String enc1, String enc2) {
        BigInteger c1 = PaillierUtil.fromBase64(enc1);
        BigInteger c2 = PaillierUtil.fromBase64(enc2);
        BigInteger sum = PaillierUtil.add(c1, c2, keyPair.getPublicKey());
        return PaillierUtil.toBase64(sum);
    }

    // ==================== 解密（开标阶段） ====================

    /**
     * 解密密文
     *
     * @param encryptedBase64 密文 Base64
     * @return 明文 long 值
     */
    public long decrypt(String encryptedBase64) {
        BigInteger cipher = PaillierUtil.fromBase64(encryptedBase64);
        return PaillierUtil.decrypt(cipher, keyPair.getPrivateKey()).longValue();
    }

    /**
     * 解密原始出价（分）
     */
    public long decryptOriginalPrice(String encryptedPrice) {
        return decrypt(encryptedPrice);
    }

    /**
     * 解密有效出价
     *
     * 存储的是 price × creditScore，需要除以 100 得到实际有效出价（分）
     * 注意：这里返回的是 price × creditScore 的原始值，
     * 调用方根据需要自行除以 100
     */
    public long decryptEffectivePrice(String encryptedEffectivePrice) {
        return decrypt(encryptedEffectivePrice);
    }
}
