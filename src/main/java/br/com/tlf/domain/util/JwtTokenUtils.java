package br.com.tlf.domain.util;


import br.com.tlf.configuration.common.rest.exceptionhandler.error.InvalidTokenException;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.TokenNullException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;


@Component
public class JwtTokenUtils {

    public static String cpfToken(String token) {
        LogUtils.log("Decoding JWT token to extract CPF");
        String cpf;
        try {
            LogUtils.log("Token received for decoding: {}", token);
            cpf = decodeJwt(token);
            LogUtils.log("Decoded CPF: {}", cpf);
        } catch (JsonProcessingException e) {
            throw new InvalidTokenException("Error trying to decode token");
        }
        return cpf;
    }

    public String generateToken(String customerId) {

        String encryptedCustomerId = null;

        try {
            encryptedCustomerId = HmacUtils.generateHmacSha256(customerId.getBytes());
        } catch (Exception ex) {
            throw new InvalidTokenException("Error trying to create token");
        }

        if (encryptedCustomerId != null) {
            Key signingKey = getSigningKey(encryptedCustomerId);

            LinkedHashMap<String, Object> claims = new LinkedHashMap<>();
            claims.put("term-id", customerId);

            String token = Jwts.builder().setClaims(claims)
                    .setSubject(customerId)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                    .signWith(signingKey, SignatureAlgorithm.HS256)
                    .setId(UUID.randomUUID().toString())
                    .setHeaderParam("kid","VivoCustomerDomain")
                    .compact();

            return token;
        }

        return null;
    }

    private static String decodeJwt(String token) throws JsonProcessingException {

        if (Objects.isNull(token)) {
            throw new TokenNullException("Authorization is mandatory");
        }

        if (token.startsWith("Bearer")) {
            token = token.substring(7);
        }

        DecodedJWT jwt;
        try {
            jwt = JWT.decode(token);
        } catch (JWTDecodeException e) {
            throw new InvalidTokenException("Invalid JWT format");
        }
        var claims = jwt.getClaims();
        var claimsMap = new HashMap<>();

        claims.forEach((key, value) -> claimsMap.put(key, value.as(Object.class)));

        var mapper = new ObjectMapper();
        var jsonString = mapper.writeValueAsString(claimsMap);

        var rootNode = mapper.readTree(jsonString);
        var idNode = rootNode.path("cpf");

        return idNode.asText();
    }

    private Key getSigningKey(String encodedString) {
        byte[] keyBytes = Decoders.BASE64.decode(encodedString);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
