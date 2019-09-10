package com.voysis.events

open class VoysisException : Exception {

    constructor(message: String) : super(message)

    constructor(throwable: Throwable) : super(throwable)

    constructor(message: String, throwable: Throwable) : super(message, throwable)
}

class PermissionDeniedException : VoysisException {

    constructor(message: String) : super(message)

    constructor(throwable: Throwable) : super(throwable)

    constructor(message: String, throwable: Throwable) : super(message, throwable)
}

/**
 * The class {@code NativeException} and its subclasses are a form of
 * {@code VoysisException} and therefore subsclasses {@code Throwable} of  that indicates conditions that a reasonable
 * application might want to catch.
 *
 * The class {@code NativeException}  represents exceptions caused by an internal error in the native code integrated for the local execution mode
 */
class NativeException : VoysisException {

    constructor(message: String) : super(message)

    constructor(throwable: Throwable) : super(throwable)

    constructor(message: String, throwable: Throwable) : super(message, throwable)
}