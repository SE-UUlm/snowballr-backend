package se.uulm.snowballr.backend.model.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

fun interface WhereQuery {
    fun toOp(): Op<Boolean>
}

class SingleColumnQuery<T : Any?>(val column: Column<T>, val value: T) : WhereQuery {
    override fun toOp(): Op<Boolean> = column eq value
}

class DualColumnQuery<T1, T2>(val column1: Column<T1>, val value1: T1, val column2: Column<T2>, val value2: T2) :
    WhereQuery {
    override fun toOp(): Op<Boolean> = (column1 eq value1) and (column2 eq value2)
}

class DualMixedIdColumnQuery<T1 : Any, T2>(
    val column1: Column<EntityID<T1>>,
    val value1: T1,
    val column2: Column<T2>,
    val value2: T2,
) : WhereQuery {
    override fun toOp(): Op<Boolean> = (column1 eq value1) and (column2 eq value2)
}

class DualIdColumnQuery<T1 : Any, T2 : Any>(
    val column1: Column<EntityID<T1>>,
    val value1: T1,
    val column2: Column<EntityID<T2>>,
    val value2: T2,
) : WhereQuery {
    override fun toOp(): Op<Boolean> = (column1 eq value1) and (column2 eq value2)
}
