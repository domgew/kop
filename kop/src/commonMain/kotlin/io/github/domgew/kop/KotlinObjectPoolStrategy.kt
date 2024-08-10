package io.github.domgew.kop

public enum class KotlinObjectPoolStrategy {

    /**
     * **Last-In-First-Out**
     *
     * Works well if you want the lowest number of object in the pool.
     */
    LIFO,

    /**
     * **First-In-First-Out**
     *
     * Works well if you want the lowest number of object creations.
     */
    FIFO,
    ;
}
