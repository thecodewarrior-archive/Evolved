package thecodewarrior.evolved.learning.network

import thecodewarrior.evolved.learning.gene.Gene
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */

class BrainData(var nets: Map<Action, NetData>) : Cloneable, Gene<BrainData> {

    public override fun clone(): BrainData {
        return BrainData(nets.mapValues { it.value.clone() })
    }

    companion object {
        fun random() : BrainData {
            val rand = ThreadLocalRandom.current()

            val nets = mutableMapOf<Action, NetData>()

            for(action in Action.values()) {
                nets.put(action, NetData.random(action))
            }

            return BrainData(nets)
        }
    }

    override fun cross(other: BrainData): BrainData {
        val nets = mutableMapOf<Action, NetData>()

        for(action in Action.values()) {
            val sel = ThreadLocalRandom.current().nextFloat()

            if(sel < 0.4) {
                nets[action] = this.nets[action]!!
            } else if(sel < 0.8) {
                nets[action] = other.nets[action]!!
            } else {
                nets[action] = this.nets[action]!!.cross(other.nets[action]!!)
            }
        }

        return BrainData(nets)
    }

    override fun mutate(): BrainData {
        val nets = mutableMapOf<Action, NetData>()

        this.nets.forEach {
            nets[it.key] = it.value.mutate()
        }

        return BrainData(nets)
    }

    override fun random(): BrainData {
        val nets = mutableMapOf<Action, NetData>()

        for(action in Action.values()) {
            nets[action] = NetData.random(action)
        }

        return BrainData(nets)
    }

}

class Brain(_data: BrainData, food: (Int) -> Unit) {
    val data = _data.clone()

    val nets: Map<Action, Net>
    val inputs: Map<Sense, Map<Int, InputNeuron>>
    val outputs: Map<Action, OutputNeuron>

    init {
        nets = mapOf(*data.nets.map { it.key to Net(it.value, food) }.toTypedArray())

        val i = mutableMapOf<Sense, MutableMap<Int, InputNeuron>>()
        val o = mutableMapOf<Action, OutputNeuron>()

        nets.forEach {
            it.value.neurons.forEach {
                if(it is InputNeuron)
                    Sense.values().getOrNull(it.sense)?.let { s -> i.getOrPut(s, { mutableMapOf() })[it.senseIndex] = it }
                if(it is OutputNeuron)
                    Action.values().getOrNull(it.action)?.let { a -> o[a] = it }
            }
        }

        inputs = i
        outputs = o
    }

    fun tick() {
        nets.values.forEach(Net::tick)
    }

    fun fire(sense: Sense, strength: Int = 100) {
        inputs[sense]?.forEach {
            it.value.accept(strength)
        }
    }

    fun fire(sense: Sense, strength: IntArray) {
        inputs[sense]?.forEach {
            strength.getOrNull(it.value.senseIndex - sense.indexRange.first)?.let { s -> it.value.accept(s) }
        }
    }

    fun get(action: Action): Boolean {
        return outputs[action]?.get() ?: false
    }
}

enum class Sense(val indexRange: IntRange = 0..0) {
    ALWAYS(),
    SOUND(-1..1)
}

enum class Action {
    STOP,
    TURN_LEFT,
    TURN_RIGHT
}
