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
