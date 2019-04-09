import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import database.user.User
import java.util.*

object JwtConfig {
    private const val secret = "secret"
    private const val issuer = "ru.rtuitlab.account"
    private const val validityInMs = 36_000_00 // 1 hour
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()


    fun makeAccess(user: User): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", user.id)
        .withExpiresAt(getExpiration(hours = 5))
        .sign(algorithm)

    fun makeRefresh(user: User): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", user.id)
        .withExpiresAt(getExpiration(days = 10))
        .sign(algorithm)

    private fun getExpiration(hours: Int = 0, days: Int = 0) =
        Date(System.currentTimeMillis() + validityInMs * hours + validityInMs * 24 * days)
}