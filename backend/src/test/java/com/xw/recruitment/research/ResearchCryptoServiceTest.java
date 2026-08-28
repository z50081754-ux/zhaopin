package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResearchCryptoServiceTest {
    private final TronAddressValidator validator = new TronAddressValidator();
    private final ResearchCryptoService crypto = new ResearchCryptoService(
        true, "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "wallet-hash-test-key", "privacy-hash-test-key");

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
    }
}
