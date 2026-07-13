package se.uulm.snowballr.backend.model.dto.project

/**
 * The type of snowballing, according to Wohlin (2014).
 *
 * It defines which references (forward or backward) are fetched when a paper is accepted:
 * - A backward reference is defined as a paper that is cited by the paper in question.
 * - A forward reference is defined as a paper that cites the paper in question, i.e., a paper where the paper in
 * question is a backward reference.
 *
 * Roughly speaking, one could say that "backward" and "forward" are an analogy to the order of the publication dates of
 * two papers, i.e., a backward reference was most probably published before the paper in question and a forward
 * reference afterward.
 */
enum class SnowballingType {
    /**
     * All forward references are fetched.
     */
    FORWARD,

    /**
     * All backward references are fetched.
     */
    BACKWARD,

    /**
     * All forward and backward references are fetched.
     */
    BOTH,

    ;

    /**
     * Returns true if this [SnowballingType] is [BACKWARD] or [BOTH]; otherwise false.
     */
    fun isBackwardOrBoth() = this == BACKWARD || this == BOTH

    /**
     * Returns true if this [SnowballingType] is [FORWARD] or [BOTH]; otherwise false.
     */
    fun isForwardOrBoth() = this == FORWARD || this == BOTH
}
