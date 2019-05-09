package utils

import com.lambdaworks.crypto.SCryptUtil
import org.slf4j.LoggerFactory

object PasswordConfig {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    fun generatePassword(password: String) = SCryptUtil.scrypt(password, 16, 16, 16)!!

    fun matchPassword(passwordFromRequest: String, passwordFromTable: String):Boolean {
        return try {
            SCryptUtil.check(passwordFromRequest, passwordFromTable)
        } catch (ex: java.lang.IllegalArgumentException) {
            logger.error(ex.message)
            false
        }
    }
}