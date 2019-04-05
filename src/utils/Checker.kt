package utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson

class Checker {

    /**
     * Method for checking token valid
     * @param token token
     * @param secret secret
     * @return boolean depending of token valid
     */
    fun isTokenValid(token: String, secret: String): Boolean {
        return try {
            val verifier = JWT.require(Algorithm.HMAC512(secret))
            verifier.build().verify(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

    /**
     * Method for checking token decodabling
     * @param token token
     * @return boolean depending of token decodabling
     */
    fun isTokenDecodable(token: String): Boolean {
        return try {
            JWT.decode(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

}