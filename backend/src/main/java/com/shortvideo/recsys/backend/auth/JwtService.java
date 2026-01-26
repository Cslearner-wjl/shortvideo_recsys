package com.shortvideo.recsys.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 最小 JWT（HS256）实现：不引入额外 JWT 依赖。
 */
@Service
public class JwtService {
    private final ObjectMapper objectMapper;
    private final String secret;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl-seconds}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(long userId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + ttlSeconds;

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("iss", issuer);
            payload.put("iat", now);
            payload.put("exp", exp);

            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256Base64Url(signingInput, secret);
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("JWT 生成失败", e);
        }
    }

    public long verifyAndGetUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
            }
            String signingInput = parts[0] + "." + parts[1];
            String signature = parts[2];
            String expected = hmacSha256Base64Url(signingInput, secret);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII))) {
                throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, Map.class);
            Object expObj = payload.get("exp");
            Object subObj = payload.get("sub");
            if (expObj == null || subObj == null) {
                throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
            }
            long exp = Long.parseLong(expObj.toString());
            long now = Instant.now().getEpochSecond();
            if (now >= exp) {
                throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
            }
            return Long.parseLong(subObj.toString());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacSha256Base64Url(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        return base64Url(sig);
    }
}

