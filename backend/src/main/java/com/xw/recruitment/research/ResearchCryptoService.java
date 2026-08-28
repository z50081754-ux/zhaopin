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
    private final boolean available;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResearchCryptoService(
            @Value("${xw.research.enabled:false}") boolean enabled,
            @Value("${xw.research.wallet-encryption-key:}") String encryptionKey,
            @Value("${xw.research.wallet-hash-key:}") String walletHashKey,
            @Value("${xw.research.privacy-hash-key:}") String privacyHashKey) {
        boolean anyKeyConfigured = !encryptionKey.isBlank()
            || !walletHashKey.isBlank() || !privacyHashKey.isBlank();
        if (!enabled && !anyKeyConfigured) {
            this.encryptionKey = null;
            this.walletHashKey = null;
            this.privacyHashKey = null;
            this.available = false;
            return;
        }

        if (encryptionKey.isBlank() || walletHashKey.isBlank() || privacyHashKey.isBlank()) {
            throw new IllegalStateException("All research crypto keys must be configured together");
        }
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(encryptionKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Research wallet encryption key must be Base64", exception);
        }
        if (decodedKey.length != 32) {
            throw new IllegalStateException("Research wallet encryption key must decode to 32 bytes");
        }
        byte[] walletKeyBytes = walletHashKey.getBytes(StandardCharsets.UTF_8);
        byte[] privacyKeyBytes = privacyHashKey.getBytes(StandardCharsets.UTF_8);
        if (walletKeyBytes.length < 32 || privacyKeyBytes.length < 32) {
            throw new IllegalStateException("Research HMAC keys must each contain at least 32 bytes");
        }
        if (walletHashKey.equals(privacyHashKey)
                || encryptionKey.equals(walletHashKey)
                || encryptionKey.equals(privacyHashKey)) {
            throw new IllegalStateException("Research crypto keys must be independent");
        }

        this.encryptionKey = new SecretKeySpec(decodedKey, "AES");
        this.walletHashKey = walletKeyBytes;
        this.privacyHashKey = privacyKeyBytes;
        this.available = true;
    }

    public record EncryptedWallet(String ciphertext, String nonce) {}

    public boolean available() {
        return available;
    }

    public EncryptedWallet encryptWallet(String address) {
        requireAvailable();
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
        requireAvailable();
        try {
            byte[] nonce = Base64.getDecoder().decode(encryptedWallet.nonce());
            byte[] encrypted = Base64.getDecoder().decode(encryptedWallet.ciphertext());
            return new String(cipher(Cipher.DECRYPT_MODE, nonce).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to decrypt wallet address", e);
        }
    }

    public String walletHash(String value) {
        requireAvailable();
        return hmac(value, walletHashKey);
    }

    public String privacyHash(String value) {
        requireAvailable();
        return hmac(value, privacyHashKey);
    }

    private void requireAvailable() {
        if (!available) throw new IllegalStateException("Research crypto keys are unavailable");
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
