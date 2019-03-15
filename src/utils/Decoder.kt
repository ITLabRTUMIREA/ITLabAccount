package com.jaju.utils

import java.util.*

class Decoder {
    /**
     * Method for decode string
     * @param item encoded string
     * @return decoded string
     */
    fun bas64(item: String) = String(Base64.getMimeDecoder().decode(item))

    /**
     * Method for decode token
     * @param token token
     * @return decoded token as string
     */
    fun decodeToken(token: String): String {
        val header = bas64(token.substring(0, token.indexOf('.')))
        val payload = bas64(token.substring(token.indexOf('.'), token.lastIndexOf('.')))
        val signature = bas64(token.substring(token.lastIndexOf('.'), token.length))
        return header.plus('.').plus(payload).plus('.').plus(signature)
    }
}