package utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import database.tables.User
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class JwtConfig {
    private val secret = "secret"

    private val issuer = when (val a = Config().loadPath("ktor.jwt.issuer")) {
        null -> "ru.rtuitlab.account"
        else -> a
    }

    private val algorithm = Algorithm.HMAC512(secret)

    val accessVerifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withClaim("token_type", "access")
        .build()

    val refreshVerifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withClaim("token_type", "refresh")
        .build()

    fun makeAccess(user: User): String {

        val expire = when (val a = Config().loadPath("ktor.tokens_expires.access_expire_in")) {
            null -> 2L
            else -> a.toLong()
        }

        return JWT.create()
            .withIssuer(issuer)
            .withClaim("user_id", user.id)
            .withClaim("token_type", "access")
            .withExpiresAt(getExpiration(hours = expire))
            .sign(algorithm)
    }

    fun makeRefresh(user: User): String {

        val expire = when (val a = Config().loadPath("ktor.tokens_expires.refresh_expire_in")) {
            null -> 2L
            else -> a.toLong()
        }

        return JWT.create()
            .withIssuer(issuer)
            .withClaim("user_id", user.id)
            .withClaim("token_type", "refresh")
            .withExpiresAt(getExpiration(days = expire))
            .sign(algorithm)
    }

    private fun getExpiration(minutes: Long = 0, hours: Long = 0, days: Long = 0, moths: Long = 0) =
        Date.from(
            // -3h because LocalDateTime in a hurry
            LocalDateTime.now().plusMinutes(minutes).plusHours(hours - 3).plusDays(days).plusMonths(moths).toInstant(
                ZoneOffset.UTC
            )
        )

}

val ApplicationCall.user
    get() = authentication.principal<User>()

