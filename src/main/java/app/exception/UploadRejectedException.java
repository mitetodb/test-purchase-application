package app.exception;

import lombok.Getter;

@Getter
public class UploadRejectedException extends RuntimeException {

    public enum Reason {
        EMPTY,
        TYPE_NOT_ALLOWED,
        EXTENSION_MISMATCH,
        TOO_LARGE,
        READ_ERROR
    }

    private final Reason reason;
    private final UploadTarget target;

    public enum UploadTarget {
        ATTACHMENT,
        AVATAR
    }

    public UploadRejectedException(Reason reason, UploadTarget target) {
        super(reason.name());
        this.reason = reason;
        this.target = target;
    }
}
