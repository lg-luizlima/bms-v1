package br.com.tlf.shared.configuration.common.rest.exceptionhandler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorField {
    private String field;
    private String message;
    private String code;
    private String title;
    private String detail;

    public static ErrorFieldBuilder builder() {
        return new ErrorFieldBuilder();
    }

    public static ErrorFieldBuilder errorFieldBuilder(String code, String title, String detail) {
        return ErrorField.builder().code(code).title(title).detail(detail);
    }

    @Getter
    public static class ErrorFieldBuilder {
        private String field;
        private String message;
        private String code;
        private String title;
        private String detail;

        public ErrorFieldBuilder() {
        }

        public ErrorFieldBuilder field(String field) {
            this.field = field;
            return this;
        }

        public ErrorFieldBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorFieldBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorFieldBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ErrorFieldBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public ErrorField build() {
            return new ErrorField(field, message, code, title, detail);
        }
    }

}
