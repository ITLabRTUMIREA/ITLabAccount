package utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import database.user.User
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*



object JwtConfig {
    private const val secret = "secret"
    private const val issuer = "ru.rtuitlab.account"
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()


    fun makeAccess(user: User): String = JWT.create()
        .withIssuer(issuer)
        .withClaim("user_id", user.id)
        .withExpiresAt(getExpiration(hours = 5))
        .sign(algorithm)

    fun makeRefresh(user: User): String = JWT.create()
        .withIssuer(issuer)
        .withClaim("user_id", user.id)
        .withExpiresAt(getExpiration(days = 10))
        .sign(algorithm)

    // -3h because LocalDateTime in a hurry
    private fun getExpiration(minutes: Long = 0, hours: Long = 0, days: Long = 0, moths: Long = 0) =
        Date.from(
            LocalDateTime.now().plusMinutes(minutes).plusHours(hours - 3).plusDays(days).plusMonths(moths).toInstant(
                ZoneOffset.UTC
            )
        )

    val ApplicationCall.user
        get() = authentication.principal<User>()
}
