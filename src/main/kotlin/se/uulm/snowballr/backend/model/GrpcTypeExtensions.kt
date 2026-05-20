package se.uulm.snowballr.backend.model

import snowballr.ProjectOuterClass.SnowballingType

/**
 * Returns true if this [SnowballingType] is BACKWARD or BOTH; otherwise false.
 */
fun SnowballingType.isBackwardOrBoth() = this == SnowballingType.SNOWBALLING_TYPE_BACKWARD ||
    this == SnowballingType.SNOWBALLING_TYPE_BOTH

/**
 * Returns true if this [SnowballingType] is FORWARD or BOTH; otherwise false.
 */
fun SnowballingType.isForwardOrBoth() = this == SnowballingType.SNOWBALLING_TYPE_FORWARD ||
    this == SnowballingType.SNOWBALLING_TYPE_BOTH
