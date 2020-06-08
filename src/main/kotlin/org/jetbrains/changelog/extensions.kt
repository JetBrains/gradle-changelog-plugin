package org.jetbrains.changelog

import groovy.lang.Closure

fun <T : Any> closure(function: () -> T) = object : Closure<T>(null) {
    @Suppress("unused")
    fun doCall() = function()
}
