package thecodewarrior.evolved.learning.genetics

import thecodewarrior.evolved.clamp
import java.util.*

class BoolGeneDef : GeneDef<Boolean>() {
    @Suppress("UNCHECKED_CAST")
    override fun mutate(rand: Random, value: Gene<*>): Gene<Boolean> {
        value as Gene<Boolean>
        return Gene(rand.nextBoolean())
    }

    override fun random(rand: Random): Gene<Boolean> {
        return Gene(rand.nextBoolean())
    }
}

class EnumGeneDef<T : Enum<T>>(val clazz: Class<T>) : GeneDef<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun mutate(rand: Random, value: Gene<*>): Gene<T> {
        value as Gene<T>
        return Gene(clazz.enumConstants[(value.value.ordinal + rand.nextInt(5) - 2) % clazz.enumConstants.size])
    }

    override fun random(rand: Random): Gene<T> {
        return Gene(clazz.enumConstants[rand.nextInt(clazz.enumConstants.size)])
    }
}

class IntGeneDef(val min: Int, val max: Int) : GeneDef<Int>() {
    override fun mutate(rand: Random, value: Gene<*>): Gene<Int> {
        value as Gene<Int>

        val range = Math.max(1.0, (max-min) * GeneConst.MUTATION_DISTANCE).toInt() // ± 1 or ±5%, whichever is greater
        val n = value.value + rand.nextInt(range*2)-range

        return Gene(n.clamp(min, max))
    }

    override fun random(rand: Random): Gene<Int> {
        return Gene(rand.nextInt(max-min)+min)
    }

}

class DoubleGeneDef(val min: Double, val max: Double) : GeneDef<Double>() {
    override fun mutate(rand: Random, value: Gene<*>): Gene<Double> {
        value as Gene<Double>

        val range = (max-min) * GeneConst.MUTATION_DISTANCE
        val n = value.value + (rand.nextDouble() - 0.5)*range
        return Gene(n.clamp(min, max))
    }

    override fun random(rand: Random): Gene<Double> {
        return Gene(rand.nextDouble()*(max-min)-min)
    }
}

class GaussianDoubleGeneDef(val mul: Double) : GeneDef<Double>() {
    override fun mutate(rand: Random, value: Gene<*>): Gene<Double> {
        value as Gene<Double>

        val n = value.value + rand.nextGaussian()*mul
        return Gene(n)
    }

    override fun random(rand: Random): Gene<Double> {
        return Gene(rand.nextGaussian()*mul)
    }
}

class Gene<T>(val value: T) {

    @Suppress("UNCHECKED_CAST")
    fun <V> get(): V {
        return value as V
    }
}

abstract class GeneDef<T> {
    var char = '!'
        protected set

    abstract fun mutate(rand: Random, value: Gene<*>): Gene<T>
    abstract fun random(rand: Random): Gene<T>

    fun chr(char: Char): GeneDef<*> {
        this.char = char
        return this
    }
}

class Chromosome(val list: List<Gene<*>>) : List<Gene<*>> by list {
    fun seq(): ChromosomeSeq {
        return ChromosomeSeq(this)
    }
}

class ChromosomeSeq(val chromosome: Chromosome) {
    private var index = 0

    fun next(): Gene<*> {
        return chromosome[index++]
    }

    fun last(): Gene<*> {
        return chromosome[index - 1]
    }
}

class ChromosomeDef(vararg val types: GeneDef<*>) {

    fun desc(): String {
        return types.map { it.char }.joinToString("")
    }

    fun crossover(rand: Random, a: Chromosome, b: Chromosome): Pair<Chromosome, Chromosome> {
        val newA = mutableListOf<Gene<*>>()
        val newB = mutableListOf<Gene<*>>()

        newA.addAll(a.list)
        newB.addAll(b.list)

        for(i in 0..a.size-1) {
            if(rand.nextDouble() < GeneConst.SWAP_GENE_CHANCE) {
                val tmp = newA[i]
                newA[i] = newB[i]
                newB[i] = newA[i]
            }
        }

        return Chromosome(newA) to Chromosome(newB)
    }

    fun mutate(rand: Random, c: Chromosome): Chromosome {
        val newList = c.toMutableList()
        newList.indices.forEach { i ->
            if(rand.nextDouble() < GeneConst.MUTATION_CHANCE)
                newList[i] = types[i].mutate(rand, newList[i])
        }
        return Chromosome(newList)
    }

    fun random(rand: Random): Chromosome {
        return Chromosome(types.map { it.random(rand) })
    }
}

interface ChromosomeCodec<T> {
    val def: ChromosomeDef
    fun create(chromosome: Chromosome): T
}
