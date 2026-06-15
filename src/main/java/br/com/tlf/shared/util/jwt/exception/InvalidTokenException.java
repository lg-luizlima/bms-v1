package br.com.tlf.shared.util.jwt.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Object... args) {
        super(String.format(message, args));
    }

}