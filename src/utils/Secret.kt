package utils

import kotlin.random.Random

object Secret {
    fun generate()=
        (0..Random.nextInt(15,30)).
            map { (('A'..'Z')+('a'..'z')+('0'..'9')).random() }.
            joinToString(separator = "")
}