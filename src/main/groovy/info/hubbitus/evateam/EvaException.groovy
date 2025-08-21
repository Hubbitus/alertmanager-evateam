package info.hubbitus.evateam

import groovy.transform.CompileStatic

@CompileStatic
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
