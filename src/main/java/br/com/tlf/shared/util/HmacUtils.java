package br.com.tlf.shared.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HmacUtils {

    private HmacUtils() {}

    private static final Logger log = LoggerFactory.getLogger(HmacUtils.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String generateHmacSha256(String data, String secret) {
        log.info("Generating HMAC-SHA256 for data: {}", data);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            String formatHex = HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            log.info("Generated HMAC-SHA256: {}", formatHex);
            return formatHex;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256", e);
        }
    }

    public static String generateHmacSha256(byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {
        String defaultMessage = "test";
        byte[] bytes = hmac(HMAC_SHA256, key, defaultMessage.getBytes());
        return bytesToHex(bytes);
    }

    private static byte[] hmac(String algorithm, byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(message);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
