package thecodewarrior.evolved.learning.network

import thecodewarrior.evolved.pick
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
enum class NeuronType {
    NORMAL {
        override fun create(data: IntArray, food: (Int) -> Unit): Neuron {
            val neuron = NormalNeuron(food)
            neuron.threshold = data[0]
            return neuron
        }

        override fun getData(neuron: Neuron): IntArray {
            return intArrayOf(neuron.threshold)
        }

        override fun random(): IntArray {
            return intArrayOf(ThreadLocalRandom.current().nextInt(1, 150))
        }

        override fun mutate(array: IntArray): IntArray {
            return intArrayOf(Math.max(0, array[0] + ThreadLocalRandom.current().nextInt(50) - 25))
        }
    },
    INPUT {
        override fun create(data: IntArray, food: (Int) -> Unit): Neuron {
            val neuron = InputNeuron(food)
            neuron.sense = data[0]
            neuron.senseIndex = data[1]
            return neuron
        }

        override fun getData(neuron: Neuron): IntArray {
            neuron as InputNeuron
            return intArrayOf(neuron.sense, neuron.senseIndex)
        }

        override fun random(): IntArray {
            val sense = Sense.values().pick()!!
            return intArrayOf(sense.ordinal, ThreadLocalRandom.current().nextInt(sense.indexRange.first, sense.indexRange.last+1))
        }

        override fun mutate(array: IntArray): IntArray {
            return intArrayOf(array[0], array[1])
        }
    },
    OUTPUT {
        override fun create(data: IntArray, food: (Int) -> Unit): Neuron {
            val neuron = OutputNeuron(food)
            neuron.threshold = data[0]
            neuron.action = data[1]
            return neuron
        }

        override fun getData(neuron: Neuron): IntArray {
            neuron as OutputNeuron
            return intArrayOf(neuron.threshold, neuron.action)
        }

        override fun random(): IntArray {
            return intArrayOf(ThreadLocalRandom.current().nextInt(1, 100), Action.values().pick()!!.ordinal)
        }

        override fun mutate(array: IntArray): IntArray {
            return intArrayOf(Math.max(0, array[0] + ThreadLocalRandom.current().nextInt(50) - 25), array[1])
        }
    };

    abstract fun create(data: IntArray, food: (Int) -> Unit): Neuron

    abstract fun getData(neuron: Neuron): IntArray

    abstract fun random(): IntArray

    abstract fun mutate(array: IntArray): IntArray
}

data class NeuronConnection(val neuron: Neuron, var strength: Int) {
    fun accept() {
        if(neuron is InputNeuron)
            return // don't allow connections back to inputs
        if(Thread.currentThread().getStackTrace().size > 100) // I give up
            return
        val fired = neuron.accept(strength)

//        if(fired) {
//            if(strength > 0) // reinforce
//                strength++
//            else
//                strength--
//        } else {
//            if(strength > 0) // weaken
//                strength--
//            else
//                strength++
//        }
    }
}

abstract class Neuron(val type: NeuronType) {
    var threshold = 100
        set(value) {
            if(value < 0)
                field = 0
            else
                field = value
        }
    var currentCharge = 0

    abstract fun fire()

    fun accept(spike: Int): Boolean {
        currentCharge += spike

        if(currentCharge < 0)
            currentCharge = 0
        if(currentCharge > threshold) {
            fire()
            currentCharge = 0
            return true
        }
        return false
    }

    open fun tick() {
        val leak = currentCharge / 30
        currentCharge -= leak
    }
}

class NormalNeuron(val food: (Int) -> Unit) : Neuron(NeuronType.NORMAL) {
    val outputs = mutableListOf<NeuronConnection>()

    override fun fire() {
        food(1)
        outputs.forEach { it.accept() }
    }
}

class InputNeuron(val food: (Int) -> Unit) : Neuron(NeuronType.INPUT) {
    var sense = 0
    var senseIndex = 0

    val outputs = mutableListOf<NeuronConnection>()

    override fun fire() {
        food(1)
        outputs.forEach { it.accept() }
    }
}

class OutputNeuron(val food: (Int) -> Unit) : Neuron(NeuronType.OUTPUT) {
    var action: Int = -1

    private var on = false

    override fun fire() {
        food(1)
        on = true
    }

    fun get(): Boolean {
        return on
    }

    override fun tick() {
        super.tick()
        on = false
    }
}
