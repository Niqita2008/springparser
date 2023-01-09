package com.vprave.niqitadev.parser.parse;

public class IllegalDocumentException extends Throwable {
    public final IllegalCause cause;

    public IllegalDocumentException(IllegalCause cause, Exception e) {
        super(cause.message);
        this.cause = cause;
        initCause(e);
    }

    public enum IllegalCause {
        INCORRECT_FORMAT("Неверный формат документа"), INVALID("Нечитаемый документ"),
        TOO_SHORT("Документ подозрительно короткий"), NO_OUTLINE("В документе ОКБ нет закладок");
        final String message;

        IllegalCause(String message) {
            this.message = message;
        }
    }
}
