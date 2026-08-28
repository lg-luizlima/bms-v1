package br.com.tlf.shared.util.jwt;

import java.util.Objects;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import br.com.tlf.core.domain.exception.InvalidTokenException;
import br.com.tlf.core.domain.exception.TokenNullException;


public final class JwtTokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CPF_CLAIM = "cpf";

    private JwtTokenUtils() {
    }

    public static String cpfToken(String token) {
        if (Objects.isNull(token)) {
            throw new TokenNullException("Authorization is mandatory");
        }

        try {
            DecodedJWT jwt = JWT.decode(stripBearerPrefix(token));
            return cpfClaimValue(jwt.getClaim(CPF_CLAIM));
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Invalid JWT format");
        }
    }

    private static String stripBearerPrefix(String token) {
        return token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;
    }

    private static String cpfClaimValue(Claim claim) {
        if (claim.isMissing() || claim.isNull()) {
            return null;
        }
        String asString = claim.asString();
        return asString != null ? asString : String.valueOf(claim.as(Object.class));
    }
}
