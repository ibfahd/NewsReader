package com.yourname.newsreader.data.remote

/**
 * A sealed class that wraps every network call result into one of three states.
 *
 * ─── Why not just throw exceptions? ──────────────────────────────────────────
 * Exceptions work fine for truly exceptional cases, but network failures are
 * EXPECTED outcomes — they happen frequently on mobile. Using a sealed class
 * makes every call site explicitly handle both success and failure without
 * relying on try/catch scattered across the codebase.
 *
 * ─── The three cases ──────────────────────────────────────────────────────────
 *
 * Success<T>  — HTTP 2xx response, body successfully parsed.
 *               The data is the decoded Kotlin object.
 *
 * HttpError   — HTTP response received (4xx, 5xx), but indicates a logical
 *               error (bad API key, not found, server error). The raw code and
 *               message help callers decide whether to retry.
 *
 * NetworkError — No response at all. Could be no internet, DNS failure, timeout,
 *               SSL error, or any IOException. The throwable carries the details.
 *
 * ─── Usage pattern ────────────────────────────────────────────────────────────
 *   when (val result = safeApiCall { apiService.getTopHeadlines() }) {
 *       is NetworkResult.Success    -> handleData(result.data)
 *       is NetworkResult.HttpError  -> showError("Server error ${result.code}")
 *       is NetworkResult.NetworkError -> showError("Check your connection")
 *   }
 */
sealed class NetworkResult<out T> {

    /** The network call succeeded and the body was parsed into [data]. */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * The server responded but returned a non-2xx HTTP status code.
     * @param code  The HTTP status code (e.g. 401, 429, 500).
     * @param message The HTTP status message or API error body.
     */
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>()

    /**
     * No HTTP response was received — network unreachable, timeout, SSL failure.
     * @param throwable The underlying exception with technical details.
     */
    data class NetworkError(val throwable: Throwable) : NetworkResult<Nothing>()
}

/**
 * A top-level helper that executes a suspend [apiCall] and wraps the result
 * in [NetworkResult], catching both HTTP errors and IO exceptions.
 *
 * This centralises error handling so individual data sources don't need
 * try/catch blocks — they just call [safeApiCall] and pattern-match the result.
 *
 * Example:
 *   val result = safeApiCall { newsApiService.getTopHeadlines(page = 1) }
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: retrofit2.HttpException) {
        // HttpException is thrown by Retrofit when the server returns a non-2xx code.
        NetworkResult.HttpError(
            code = e.code(),
            message = e.response()?.errorBody()?.string() ?: e.message()
        )
    } catch (e: java.io.IOException) {
        // IOException covers: no internet, DNS failure, timeout, SSL errors.
        NetworkResult.NetworkError(e)
    }
}