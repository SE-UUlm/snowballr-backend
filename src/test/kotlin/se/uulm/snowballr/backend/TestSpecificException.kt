package se.uulm.snowballr.backend

/**
 * An exception that can be thrown for mocking errors in sub-calls. This way, this specific exception can be expected,
 * which won't be thrown from production code, and therefore it can only be raised by the mocking.
 */
class TestSpecificException : Exception("This is a test exception")
