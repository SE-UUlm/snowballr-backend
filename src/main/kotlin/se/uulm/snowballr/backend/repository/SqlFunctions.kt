package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.FloatColumnType

/**
 * Trigram similarity between [left] and [right] as provided by the `pg_trgm` extension.
 *
 * The result ranges from 0 (nothing in common) to 1 (identical).
 */
fun similarity(left: Expression<String>, right: Expression<String>): CustomFunction<Float> =
    CustomFunction("similarity", FloatColumnType(), left, right)

/**
 * Largest value of the passed [expressions].
 */
@Suppress("SpreadOperator")
fun greatest(vararg expressions: Expression<Float>): CustomFunction<Float> =
    CustomFunction("GREATEST", FloatColumnType(), *expressions)
