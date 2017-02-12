package thecodewarrior.evolved

import com.badlogic.gdx.math.Vector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */

fun vec(x: Number, y: Number) = Vector2(x.toFloat(), y.toFloat())

operator fun <T : Vector<T>> T.plus(vec: T) = this.cpy().add(vec)
operator fun <T : Vector<T>> T.minus(vec: T) = this.cpy().sub(vec)

operator fun <T : Vector<T>> T.times(number: Number) = this.cpy().scl(number.toFloat())
operator fun <T : Vector<T>> T.times(vec: T) = this.cpy().scl(vec)

operator fun <T : Vector<T>> T.div(number: Number) = this.cpy().scl(1f/number.toFloat())

operator fun <T : Vector2> T.div(vec: T) = this.cpy().scl(1/vec.x, 1/vec.y)
operator fun <T : Vector3> T.div(vec: T) = this.cpy().scl(1/vec.x, 1/vec.y, 1/vec.z)

fun <T : Vector<T>> T.projectTo(vec: T): T {

    this.set(vec * ( this.dot(vec) / vec.cpy().dot(vec) ))

    return this
}

val Number.pxtom: Float
    get() = this.toFloat() / EvolvedGame.ppm

val Number.mtopx: Float
    get() = this.toFloat() * EvolvedGame.ppm

val Number.mtocm: Float
    get() = this.toFloat() * 100

val Number.cmtom: Float
    get() = this.toFloat() / 100

val Vector2.pxtom: Vector2
    get() = this / EvolvedGame.ppm

val Vector2.mtopx: Vector2
    get() = this * EvolvedGame.ppm

val Vector2.mtocm: Vector2
    get() = this * 100

val Vector2.cmtom: Vector2
    get() = this / 100


fun <T> List<T>.pick(rand: Random = ThreadLocalRandom.current()): T? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun <T> Array<T>.pick(rand: Random = ThreadLocalRandom.current()): T? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Int>.pick(rand: Random = ThreadLocalRandom.current()): Int? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Long>.pick(rand: Random = ThreadLocalRandom.current()): Long? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Short>.pick(rand: Random = ThreadLocalRandom.current()): Short? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Byte>.pick(rand: Random = ThreadLocalRandom.current()): Byte? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Float>.pick(rand: Random = ThreadLocalRandom.current()): Float? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}
fun Array<Double>.pick(rand: Random = ThreadLocalRandom.current()): Double? {
    if(this.isEmpty())
        return null
    return this[rand.nextInt(this.size)]
}

fun IntRange.pick(rand: Random = ThreadLocalRandom.current()): Int {
    return rand.nextInt(this.endInclusive - this.start) + start
}

fun IntRange.pick(count: Int, rand: Random = ThreadLocalRandom.current()): List<Int> {
    return this.toList().pick(count, rand)
}

fun <T> List<T>.pickIndices(count: Int, rand: Random = ThreadLocalRandom.current()): List<Int> {
    if(this.isEmpty())
        return listOf()
    return this.indices.toList().shuffle().subList(0, count)
}

inline fun <reified T> Array<T>.pickIndices(count: Int, rand: Random = ThreadLocalRandom.current()): Array<Int> {
    if(this.isEmpty())
        return arrayOf()
    return this.indices.toList().shuffle().subList(0, count).toTypedArray()
}

fun <T> List<T>.pick(count: Int, rand: Random = ThreadLocalRandom.current()): List<T> {
    if(this.isEmpty())
        return listOf()
    return this.pickIndices(count, rand).map { this[it] }
}

inline fun <reified T> Array<T>.pick(count: Int, rand: Random = ThreadLocalRandom.current()): Array<T> {
    if(this.isEmpty())
        return arrayOf()
    return this.pickIndices(count, rand).map { this[it] }.toTypedArray()
}


fun <T> Collection<T>.shuffle(): List<T> {
    val list = ArrayList(this)
    Collections.shuffle(list)
    return list
}

inline fun <reified T> Array<T>.repeat(n: Int): Array<T> {
    if(this.isEmpty())
        return this
    if(n < 0)
        throw IllegalArgumentException("n must be greater than or equal to zero")
    return Array(this.size*n) { i -> this[i%this.size] }
}

fun Byte.clamp(min: Byte, max: Byte): Byte {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}
fun Short.clamp(min: Short, max: Short): Short {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}
fun Int.clamp(min: Int, max: Int): Int {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}
fun Long.clamp(min: Long, max: Long): Long {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}
fun Float.clamp(min: Float, max: Float): Float {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}
fun Double.clamp(min: Double, max: Double): Double {
    if(this < min)
        return min
    if(this > max)
        return max
    return this
}

fun Float.floor() = this.toInt()
fun Double.floor() = this.toInt()

fun Float.ceil() = Math.ceil(this.toDouble()).toInt()
fun Double.ceil() = Math.ceil(this).toInt()

fun gcm(a: Int, b: Int): Int {
    return if (b == 0) a else gcm(b, a % b) // Not bad for one line of code :)
}

inline fun Int.times(f: (Int) -> Unit) {
    for(i in 0..this-1) {
        f(i)
    }
}


