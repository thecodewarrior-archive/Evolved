package thecodewarrior.evolved.learning

import com.badlogic.gdx.graphics.Color
import thecodewarrior.evolved.clamp

/**
 * Created by TheCodeWarrior
 */
interface Visible {
    val color: DeepColor
}

class DeepColor {
    val channels = DoubleArray(DeepChannel.values().size)

    operator fun set(channel: DeepChannel, value: Double) {
        channels[channel.ordinal] = value.clamp(0.0, 1.0)
    }
    operator fun get(channel: DeepChannel) : Double {
        return channels[channel.ordinal]
    }

    fun with(channel: DeepChannel, value: Double) : DeepColor {
        this[channel] = value
        return this
    }
}

enum class DeepChannel(val realColor: Color) {
    BORDER(Color.DARK_GRAY),
    FOOD(Color.BLUE),
    CREATURE(Color.WHITE)
}

enum class EnumSense(val rangeMin: Double, val rangeMax: Double) {
    ENERGY(0..1),
    BORDER(0..1),
    FOOD(0..1),
    CREATURE(0..1)
    ;

    constructor(r: IntRange) : this(r.start.toDouble(), r.endInclusive.toDouble())
    constructor() : this(0.0, 0.0)

    companion object {
        val byChannel = mutableMapOf(
                DeepChannel.BORDER to BORDER,
                DeepChannel.FOOD to FOOD,
                DeepChannel.CREATURE to CREATURE
        )
    }
}

enum class EnumAction(val min: Double, val max: Double) {
    FORWARD(0..1),
    LEFT(0..1),
    RIGHT(0..1),
    BREED(-1..1);

    constructor(r: IntRange) : this(r.start.toDouble(), r.endInclusive.toDouble())
}

