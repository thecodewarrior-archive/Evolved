package thecodewarrior.evolved.learning.gene

import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
open class Genome : Gene<Genome> {
    val genes: MutableMap<Any, Gene<*>> = mutableMapOf()

    override fun cross(other: Genome): Genome {
        val new = Genome()

        val keys = this.genes.keys + other.genes.keys

        val rand = ThreadLocalRandom.current()

        keys.forEach { k ->
            val a = this.genes[k]
            val b = other.genes[k]

            val chooseA = rand.nextBoolean()

            if(a != null) {
                if(chooseA || b == null)
                    new.genes[k] = a
            }
            if(b != null) {
                if(!chooseA || a == null)
                    new.genes[k] = b
            }
        }

        return new
    }

    override fun mutate(): Genome {
        val new = Genome()

        this.genes.keys.forEach { k ->
            this.genes[k]?.let { new.genes[k] = it.mutate() }
        }

        return new
    }

    override fun random(): Genome {
        val new = Genome()

        this.genes.keys.forEach { k ->
            this.genes[k]?.let { new.genes[k] = it.random() }
        }

        return new
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Any): T? {
        return genes[key] as? T
    }
}
