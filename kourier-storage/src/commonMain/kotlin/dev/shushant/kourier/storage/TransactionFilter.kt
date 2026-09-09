package dev.shushant.kourier.storage

import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.model.TransactionStatus

class TransactionFilter(
    val query: String = "",
    val status: String? = null,
    val method: String? = null
) {
    fun matches(transaction: HttpTransaction): Boolean = transaction.matches(query, status, method)

    fun filter(transactions: List<HttpTransaction>): List<HttpTransaction> {
        return transactions.filter { matches(it) }
    }
}

fun HttpTransaction.matches(
    query: String,
    statusFilter: String?,
    methodFilter: String?
): Boolean {
    val matchesQuery = query.isBlank() || searchableValues().any { it.contains(query, ignoreCase = true) }
    val matchesMethod = methodFilter.isNullOrBlank() ||
        methodFilter.equals("ALL", ignoreCase = true) ||
        request.method.equals(methodFilter, ignoreCase = true)
    val statusCode = response?.statusCode
    val matchesStatus = when (statusFilter?.uppercase()) {
        null, "", "ALL" -> true
        "2XX" -> statusCode in 200..299
        "3XX" -> statusCode in 300..399
        "4XX" -> statusCode in 400..499
        "5XX" -> statusCode in 500..599
        "ERRORS" -> status == TransactionStatus.FAILED || (statusCode != null && statusCode >= 400)
        "PENDING" -> status == TransactionStatus.PENDING
        else -> true
    }
    return matchesQuery && matchesMethod && matchesStatus
}

private fun HttpTransaction.searchableValues(): Sequence<String> = sequence {
    yield(request.url)
    yield(request.host)
    yield(request.path)
    request.queryParams.forEach { (name, value) -> yield(name); yield(value) }
    request.headers.forEach { yield(it.name); yield(it.value) }
    response?.headers?.forEach { yield(it.name); yield(it.value) }
}
