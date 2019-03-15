package com.jaju.utils

import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Encoder {
    /**
     * Method for hashing data
     * @param item string that hashing
     * @param key key value
     * @return encoded data as string
     */
    fun sha512(item: String, key: String): String {
        val bytes = item.toByteArray()
        val mac = Mac.getInstance("HmacSHA512")
        val keySpec = SecretKeySpec(key.toByteArray(), "HmacSHA512")
        mac.init(keySpec)
        return mac.doFinal(bytes).fold("") { str, it ->
            str + "%02x".format(it)
        }
    }

    /**
     * Method for encode data
     * @param item string that encoding
     * @return encoded data as string
     */
    fun bas64(item: String): String = Base64.getUrlEncoder().encodeToString(item.toByteArray())
}