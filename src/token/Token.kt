package token

import com.jaju.utils.Encoder
import java.lang.StringBuilder


class Token {
    private val tokenGen = TokenGen()
    var tokenStr: String = ""

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
        private val payloadPar = LinkedHashMap<String, String>()

        /**
         * Method for add parameters too map
         * @param name name of parameter
         * @param value value of parameter
         * @return TokenGen
         */
        fun addPar(name: String, value: Any): TokenGen {
            payloadPar[name] = "$value"
            return this
        }

        /**
         * Method for delete parameters from map
         * @param name name of parameter
         * @return TokenGen
         */
        fun delPar(name: String): TokenGen {
            payloadPar.remove(name)
            return this
        }

        /**
         * Method for create token
         * @param secret secret key
         */
        fun createToken(secret: String) {
            val header = Encoder().bas64("""{"alg":"SHA512","type":"JWT"}""")
            var payload = StringBuilder("""{""")
            payloadPar.forEach { name, value -> payload.append(""","$name":"$value"""") }
            payload.deleteCharAt(1)
            payload.append("""}""")
            payload = StringBuilder(Encoder().bas64(payload.toString()))
            val signature = Encoder()
                .bas64(
                    Encoder()
                        .sha512(Encoder().bas64(header.plus('.').plus(payload)), secret)
                )
            tokenStr = header.plus(".").plus(payload).plus(".").plus(signature)
        }
    }
}