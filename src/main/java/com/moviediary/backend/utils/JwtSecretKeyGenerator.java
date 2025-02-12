package com.moviediary.backend.utils;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;

public class JwtSecretKeyGenerator {
    public static void main(String[] args) {
        // ✅ 256비트(32바이트) Secret Key 생성
        Key key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);

        // ✅ Base64 인코딩하여 출력 (application.properties에 넣을 값)
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println("🔑 Generated JWT Secret Key: " + base64Key);
    }
}
