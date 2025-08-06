package info.hubbitus.evateam

class EvaException extends RuntimeException {
    EvaException(String message) {
        super(message)
    }
    EvaException(String message, Throwable cause) {
        super(message, cause)
    }
    EvaException(Throwable cause) {
        super(cause)
    }
}
