package utils

import kotlin.random.Random

object Session {
    fun generate()=
        (0..Random.nextInt(3,5)).
            map { (('A'..'Z')+('a'..'z')+('0'..'9')).random() }.
            joinToString(separator = "")
}