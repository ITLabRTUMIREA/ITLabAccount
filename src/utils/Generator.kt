package com.jaju.utils

class Generator {
    /**
     * Method for generating string of latin symbols and numbers
     * @param len length of string
     * @return string of symbols
     */
    fun strGen(len: Int) = (('a'..'z') + ('A'..'Z') + ('0'..'9')).map { it }.shuffled().subList(0, len).joinToString("")
}
