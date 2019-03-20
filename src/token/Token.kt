package token

import com.google.gson.JsonObject
import com.jaju.utils.Encoder
import org.hibernate.mapping.Map
import java.lang.StringBuilder


class Token {
    private val tokenGen = TokenGen()
    var tokenStr = ""

    /**
     * Method for getting TokenGenerator
     * @return TokenGenerator
     */
    fun getTokenGen(): TokenGen = tokenGen

    /**
     * Method for compare tokens
     * @return boolean value
     */
    fun isEqual(token: String) = this.tokenStr == token

    inner class TokenGen {
        private val payloadPar = HashMap<String, String>()

        /**
         * Method for add parameters too map
         * @param parameters map of parameters
         * @return TokenGen
         */
        fun addParameters(parameters:HashMap<String,String>) {
            payloadPar.putAll(parameters)
        }

        /**
         * Method for create token
         * @param secret secret key
         */
        fun createToken(secret: String) {
            val header = JsonObject()
            header.addProperty("alg", "SHA512")
            header.addProperty("type", "JWT")
            val payload = JsonObject()
            payloadPar.forEach { name, value -> payload.addProperty(name, value) }
            val headerStr = Encoder().bas64(header.toString())
            val payloadStr = Encoder().bas64(payload.toString())
            val signature = Encoder()
                .bas64(
                    Encoder()
                        .sha512(Encoder().bas64(headerStr.plus('.').plus(payloadStr)), secret)
                )
            tokenStr = headerStr.plus(".").plus(payloadStr).plus(".").plus(signature)
        }
    }
}