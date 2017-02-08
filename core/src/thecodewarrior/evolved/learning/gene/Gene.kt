package thecodewarrior.evolved.learning.gene

/**
 * Created by TheCodeWarrior
 */
interface Gene<T : Gene<T>> {
    fun cross(other: T): T
    fun mutate(): T
    fun random(): T
}
