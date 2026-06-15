package br.com.tlf.shared.util.jwt.exception;

public class TokenNullException extends RuntimeException {
    public TokenNullException(String message, Object... args) {
        super(String.format(message, args));
    }
}