package se.uulm.snowballr.backend.model.dto.project

import snowballr.ProjectOuterClass

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
    SNOWBALLING_TYPE_FORWARD,

    /**
     * All backward references are fetched.
     */
    SNOWBALLING_TYPE_BACKWARD,

    /**
     * All forward and backward references are fetched.
     */
    SNOWBALLING_TYPE_BOTH,

    ;

    companion object {
        fun fromGrpc(type: ProjectOuterClass.SnowballingType): SnowballingType = when (type) {
            ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD -> SNOWBALLING_TYPE_FORWARD
            ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BACKWARD -> SNOWBALLING_TYPE_BACKWARD
            ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH -> SNOWBALLING_TYPE_BOTH
            ProjectOuterClass.SnowballingType.UNRECOGNIZED,
            ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc(): ProjectOuterClass.SnowballingType = when (this) {
        SNOWBALLING_TYPE_FORWARD -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD
        SNOWBALLING_TYPE_BACKWARD -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BACKWARD
        SNOWBALLING_TYPE_BOTH -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH
    }

    /**
     * Returns true if this [SnowballingType] is [SNOWBALLING_TYPE_BACKWARD] or [SNOWBALLING_TYPE_BOTH];
     * otherwise false.
     */
    fun isBackwardOrBoth() = this == SNOWBALLING_TYPE_BACKWARD || this == SNOWBALLING_TYPE_BOTH

    /**
     * Returns true if this [SnowballingType] is [SNOWBALLING_TYPE_FORWARD] or [SNOWBALLING_TYPE_BOTH]; otherwise false.
     */
    fun isForwardOrBoth() = this == SNOWBALLING_TYPE_FORWARD || this == SNOWBALLING_TYPE_BOTH
}
