package com.ruoyiliteflow.common.utils.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * AES 加解密（Agent API Key 等敏感配置入库用）
 */
public final class AesEncryptUtils
{
    private static final String ALG = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private AesEncryptUtils()
    {
    }

    public static String encrypt(String plain, String secret)
    {
        if (StringUtils.isEmpty(plain))
        {
            return plain;
        }
        try
        {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(secret));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherText, String secret)
    {
        if (StringUtils.isEmpty(cipherText))
        {
            return cipherText;
        }
        try
        {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(secret));
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(bytes, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }

    private static SecretKeySpec keySpec(String secret) throws Exception
    {
        String seed = StringUtils.isEmpty(secret) ? "ruoyi-liteflow-aes" : secret;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hash, 0, key, 0, 16);
        return new SecretKeySpec(key, ALG);
    }
}
