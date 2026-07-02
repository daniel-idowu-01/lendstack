package ng.lendstack.common.crypto;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM encryption for PII columns (BVN, NIN, bank account numbers).
 * The key comes from the PII_ENCRYPTION_KEY environment variable (Base64).
 * Ciphertext layout: base64( IV[12] || ciphertext+tag ).
 */
@Service
public class PiiEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private SecretKeySpec key;

    @Value("${lendstack.security.encryption.pii-key}")
    private String base64Key;

    @PostConstruct
    void init() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "PII_ENCRYPTION_KEY is not set. Refusing to start without PII encryption (NDPC).");
        }
        byte[] raw = normalizeKey(Base64.getDecoder().decode(base64Key));
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Pads/truncates to 32 bytes so dev keys of any length work; prod should supply exactly 32. */
    private byte[] normalizeKey(byte[] input) {
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            out[i] = input[i % input.length];
        }
        return out;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ct, 0, combined, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("PII encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(GCM_TAG_BITS, combined, 0, GCM_IV_LENGTH));
            byte[] pt = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PII decryption failed", e);
        }
    }
}
