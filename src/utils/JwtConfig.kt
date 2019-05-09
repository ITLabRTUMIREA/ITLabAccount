package utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import database.RedisDBClient
import database.tables.User
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class JwtConfig(private val secret: String) {

    companion object {
        private var redisClient: RedisDBClient? = null
        //var secret: String = "qwe"
    }


    val x = JwtConfig.Companion

    fun connectToRedis() {
        if (redisClient == null) redisClient = RedisDBClient()
    }

    private val issuer = when (val a = Config().loadPath("ktor.jwt.issuer")) {
        null -> "ru.rtuitlab.account"
        else -> a
    }

//    fun loadSecretFromRedis(userId: Int): Boolean {
//        secret = when (val a = redisClient!!.getSecret(userId.toString())) {
//            null -> ""
//            else -> a
//        }
//        println(secret)
//        algorithm = Algorithm.HMAC512(secret)
//        return !secret.isNullOrBlank()
//    }

//    fun writeSecretToRedis(userId: Int) {
//        secret = Secret.generate()
//        algorithm = Algorithm.HMAC512(secret)
//        redisClient!!.addSecret(userId.toString(), secret)
//    }



    var algorithm = Algorithm.HMAC512(secret)

    fun reloadAlgorithm(secret: String) {
        algorithm = Algorithm.HMAC512(secret)
    }

    val accessVerifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withClaim("token_type", "access")
        .build()

    fun accessVerifier(): JWTVerifier {
        println()
        return JWT
            .require(algorithm)
            .withIssuer(issuer)
            .withClaim("token_type", "access")
            .build()
    }

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

inline val ApplicationCall.user
    get() = authentication.principal<User>()

