package com.xw.recruitment.research;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResearchCryptoService {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey encryptionKey;
    private final byte[] walletHashKey;
    private final byte[] privacyHashKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResearchCryptoService(
            @Value("${xw.research.enabled:false}") boolean enabled,
            @Value("${xw.research.wallet-encryption-key:}") String encryptionKey,
            @Value("${xw.research.wallet-hash-key:}") String walletHashKey,
            @Value("${xw.research.privacy-hash-key:}") String privacyHashKey) {
        if (enabled) {
            byte[] decodedKey;
            try {
                decodedKey = Base64.getDecoder().decode(encryptionKey);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Research wallet encryption key must be Base64", e);
            }
            if (decodedKey.length != 32) {
                throw new IllegalStateException("Research wallet encryption key must decode to 32 bytes");
            }
            if (walletHashKey.isBlank() || privacyHashKey.isBlank()) {
                throw new IllegalStateException("Research hash keys must not be blank");
            }
            this.encryptionKey = new SecretKeySpec(decodedKey, "AES");
        } else {
            this.encryptionKey = null;
        }
        this.walletHashKey = walletHashKey.getBytes(StandardCharsets.UTF_8);
        this.privacyHashKey = privacyHashKey.getBytes(StandardCharsets.UTF_8);
    }

    public record EncryptedWallet(String ciphertext, String nonce) {}

    public EncryptedWallet encryptWallet(String address) {
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
            byte[] encrypted = cipher.doFinal(address.getBytes(StandardCharsets.UTF_8));
            return new EncryptedWallet(Base64.getEncoder().encodeToString(encrypted),
                Base64.getEncoder().encodeToString(nonce));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt wallet address", e);
        }
    }

    public String decryptWallet(EncryptedWallet encryptedWallet) {
        try {
            byte[] nonce = Base64.getDecoder().decode(encryptedWallet.nonce());
            byte[] encrypted = Base64.getDecoder().decode(encryptedWallet.ciphertext());
            return new String(cipher(Cipher.DECRYPT_MODE, nonce).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to decrypt wallet address", e);
        }
    }

    public String walletHash(String value) {
        return hmac(value, walletHashKey);
    }

    public String privacyHash(String value) {
        return hmac(value, privacyHashKey);
    }

    private Cipher cipher(int mode, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        return cipher;
    }

    private String hmac(String value, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash research value", e);
        }
    }
}
