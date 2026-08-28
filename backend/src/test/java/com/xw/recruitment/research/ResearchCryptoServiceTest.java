package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResearchCryptoServiceTest {
    private static final String ENCRYPTION_KEY =
        "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
    private static final String WALLET_HASH_KEY =
        "wallet-hash-test-key-material-00000001";
    private static final String PRIVACY_HASH_KEY =
        "privacy-hash-test-key-material-000001";
    private final TronAddressValidator validator = new TronAddressValidator();
    private final ResearchCryptoService crypto = new ResearchCryptoService(
        true, ENCRYPTION_KEY, WALLET_HASH_KEY, PRIVACY_HASH_KEY);

    @Test void acceptsValidTronBase58CheckAndRejectsBadChecksum() {
        assertTrue(validator.isValid("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8"));
        assertFalse(validator.isValid("TJRabPrwbZy45sbavfcjinPJC18kjpRTv7"));
        assertFalse(validator.isValid("0x0123456789abcdef"));
    }

    @Test void encryptsWithFreshNonceAndHashesDeterministically() {
        var first = crypto.encryptWallet("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8");
        var second = crypto.encryptWallet("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8");
        assertNotEquals(first.ciphertext(), second.ciphertext());
        assertEquals("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8", crypto.decryptWallet(first));
        assertEquals(crypto.walletHash("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8"),
            crypto.walletHash("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8"));
        assertNotEquals(crypto.walletHash("same-value"), crypto.privacyHash("same-value"));
    }

    @Test void keepsAdminCryptoAvailableWhenPublicIntakeIsDisabled() {
        ResearchCryptoService adminCrypto = new ResearchCryptoService(
            false, ENCRYPTION_KEY, WALLET_HASH_KEY, PRIVACY_HASH_KEY);

        var encrypted = adminCrypto.encryptWallet("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8");

        assertEquals("TJRabPrwbZy45sbavfcjinPJC18kjpRTv8",
            adminCrypto.decryptWallet(encrypted));
    }

    @Test void rejectsWeakPartialAndRepeatedKeyConfiguration() {
        assertThrows(IllegalStateException.class, () -> new ResearchCryptoService(
            true, "not-base64", WALLET_HASH_KEY, PRIVACY_HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new ResearchCryptoService(
            true, ENCRYPTION_KEY, "weak", PRIVACY_HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new ResearchCryptoService(
            true, ENCRYPTION_KEY, WALLET_HASH_KEY, WALLET_HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new ResearchCryptoService(
            true, ENCRYPTION_KEY, ENCRYPTION_KEY, PRIVACY_HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new ResearchCryptoService(
            false, ENCRYPTION_KEY, "", ""));
    }
}
