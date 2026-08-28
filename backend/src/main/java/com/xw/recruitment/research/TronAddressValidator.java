package com.xw.recruitment.research;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TronAddressValidator {
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public boolean isValid(String value) {
        if (value == null || value.length() != 34 || value.charAt(0) != 'T') return false;
        byte[] decoded = decodeBase58(value);
        if (decoded.length != 25 || decoded[0] != 0x41) return false;
        byte[] payload = Arrays.copyOf(decoded, 21);
        byte[] checksum = Arrays.copyOfRange(decoded, 21, 25);
        byte[] expected = Arrays.copyOf(sha256(sha256(payload)), 4);
        return MessageDigest.isEqual(checksum, expected);
    }

    private byte[] decodeBase58(String value) {
        BigInteger decoded = BigInteger.ZERO;
        for (int i = 0; i < value.length(); i++) {
            int digit = BASE58_ALPHABET.indexOf(value.charAt(i));
            if (digit < 0) return new byte[0];
            decoded = decoded.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(digit));
        }
        byte[] bytes = decoded.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) return Arrays.copyOfRange(bytes, 1, bytes.length);
        return bytes;
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
