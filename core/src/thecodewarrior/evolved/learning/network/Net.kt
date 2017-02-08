package thecodewarrior.evolved.learning.network

import thecodewarrior.evolved.learning.gene.Gene
import thecodewarrior.evolved.pick
import java.util.*
import java.util.concurrent.ThreadLocalRandom



/**
 * Created by TheCodeWarrior
 */
class ConnectionData(var from: Int, var to: Int, var strength: Int) : Cloneable {
    public override fun clone(): ConnectionData {
        return ConnectionData(from, to, strength)
    }
}

class NeuronData(var type: NeuronType, var depth: Int, var data: IntArray) : Cloneable {
    public override fun clone(): NeuronData {
        return NeuronData(type, depth, data.clone())
    }

    companion object {
        fun random(type: NeuronType, depth: Int = ThreadLocalRandom.current().nextInt(1, 5)): NeuronData {
            return NeuronData(type, depth, type.random())
        }
    }
}

class NetData(var neurons: MutableList<NeuronData>, var connections: MutableList<ConnectionData>) : Cloneable, Gene<NetData> {

    init {
        validate()
    }

    fun validate() {
        // with my little to no understanding of algorithms, I'm going to try:
        // for each input
        // depth first search, if I come to an already visited node,
        // remove offending connection
        // 5. ???
        // 6. profit!

        neurons.forEachIndexed { i, neuron ->
            if(neuron.type == NeuronType.INPUT) {
                val stack = ArrayDeque<Int>()
                val removals = mutableSetOf<ConnectionData>()
                stack.push(i)
                connections.filter { it.from == i }.forEach { validate(it, stack, removals) }
            }
        }
    }

    fun validate(con: ConnectionData, stack: Deque<Int>, removals: MutableSet<ConnectionData>) {
        if(con.to in stack) {
            removals.add(con)
            return
        }
        stack.push(con.to)
        connections.filter { it.from == con.to }.forEach {
            if(it !in removals)
                validate(it, stack, removals)
        }
        stack.pop()
    }

    public override fun clone(): NetData {
        return NetData(ArrayList(neurons.map { it.clone() }), ArrayList(connections.map { it.clone() }))
    }

    companion object {
        fun random(action: Action): NetData {
            val rand = ThreadLocalRandom.current()

            val neurons = mutableListOf<NeuronData>()
            val connections = mutableListOf<ConnectionData>()

            for(i in 0..rand.nextInt(1,3)) {
                neurons.add(NeuronData.random(NeuronType.NORMAL))
            }

            for(i in 0..rand.nextInt(3)) {
                neurons.add(NeuronData.random(NeuronType.INPUT, 0))
            }
            neurons.add(NeuronData.random(NeuronType.OUTPUT, neurons.maxBy { it.depth }?.depth ?: 0 + 1).let { it.data[1] = action.ordinal; it})

            neurons.forEach { n ->
                val v = neurons.filter {it.depth > n.depth}.size
                if(v <= 0)
                    return@forEach
                for(i in 1..rand.nextInt(1, v)) {
                    connections.add(ConnectionData(
                            neurons.indexOf(n),
                            neurons.indexOf(neurons.filter { it.depth > n.depth }.pick()),
                            rand.nextInt(100)
                    ))
                }
            }

            return NetData(neurons, connections)
        }
    }

    override fun cross(other: NetData): NetData {
        var A = this
        var B = other

        val rand = ThreadLocalRandom.current()
        if(rand.nextBoolean()) {
            B = this
            A = other
        }

        val neurons = mutableListOf<NeuronData>()
        neurons.addAll(A.neurons.map { it.clone() })

        val connections = mutableListOf<ConnectionData>()
        connections.addAll(A.connections.map { it.clone() })


        // transfer some random neurons from the other net
        var transfers = mutableListOf(*Array<Int>(B.neurons.size) { it })

        Collections.shuffle(transfers)

        transfers = transfers.subList(0, rand.nextInt(0, transfers.size))

        var transferMap = mutableMapOf<Int, Int>()

        transfers.forEach { v ->
            transferMap[v] = neurons.size
            neurons.add(B.neurons[v].clone())
        }

        // transfer any connections that had both ends transferred
        B.connections.forEach {
            if(it.from in transferMap && it.to in transferMap) {
                connections.add(it.clone())
            }
        }

        connections.forEach {
            it.strength = Math.max(0, it.strength + rand.nextInt(50) - 25)
        }

        // add some random connections between the new neurons and the existing ones
        // the transfers list is already randomized, so making a smaller sublist will also be random
        transfers.subList(0, rand.nextInt(0, transfers.size)).forEach {
            val index = transferMap[it]!! // get the new index
            val n = neurons[index] // get the neuron so we can exclude it from the possible connection points
            val newIndex = neurons.mapIndexed { i, v -> i to v }.filter { it.second.depth > n.depth && it.second != n }.pick()?.first

            if(newIndex != null)
                connections.add(ConnectionData(index, newIndex, rand.nextInt(100)))
        }

        return NetData(neurons, connections)
    }

    override fun mutate(): NetData {
        val rand = ThreadLocalRandom.current()


        var neurons = mutableListOf(*neurons.map { it.clone() }.toTypedArray())
        neurons.forEach { it.data = it.type.mutate(it.data) }

        var nIds = mutableListOf<Int>()
        var toDelete = mutableListOf<Int>()

        neurons.indices.toCollection(nIds)
        nIds.indices.toCollection(toDelete)

        Collections.shuffle(toDelete)

        toDelete = toDelete.subList(0, Math.min(toDelete.size, rand.nextInt(2)))
        nIds.removeAll(toDelete)

        val idMap = nIds.mapIndexed { i, v -> v to i}.associate { it }

        var connections = mutableListOf(*connections.filter { it.from in idMap.keys && it.to in idMap.keys }.toTypedArray())
        connections.forEach { it.from = idMap[it.from]!!; it.to = idMap[it.to]!! }

        Collections.shuffle(connections)
        if(connections.size > 0) {
            connections = mutableListOf(*connections.subList(0, Math.min(connections.size, rand.nextInt(2))).toTypedArray())

            if(connections.size > 0) {
                var indices = mutableListOf<Int>()
                connections.indices.toCollection(indices)
                Collections.shuffle(indices)
                connections.removeAll(indices.subList(0, Math.min(indices.size, rand.nextInt(2))).map { connections[it] })


                connections.removeAll { neurons[it.from].depth >= neurons[it.to].depth }
            }
        }
        return NetData(neurons, connections)
    }

    override fun random(): NetData {
        return NetData.random(Action.values()[(this.neurons.find { it.type == NeuronType.OUTPUT }?.data ?: intArrayOf(0, 0) )[1]])
    }
}

class Net(_data: NetData, val food: (Int) -> Unit) {

    val data = _data.let { it.validate(); it}.clone()
    val neurons: List<Neuron>

    init {
        neurons = data.neurons.map {
            it.type.create(it.data, food)
        }

        val rand = ThreadLocalRandom.current()

        data.connections.forEach {
            val from = neurons[it.from]
            if(from is InputNeuron)
                from.outputs.add(NeuronConnection(neurons[it.to], it.strength))
            if(from is NormalNeuron)
                from.outputs.add(NeuronConnection(neurons[it.to], it.strength))
        }
    }

    fun tick() {
        neurons.forEach { it.tick() }
    }
}
