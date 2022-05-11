package wooteco.subway.exception;

import org.springframework.http.HttpStatus;

public class InvalidDistanceException extends ClientRuntimeException {

    public InvalidDistanceException(final String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
