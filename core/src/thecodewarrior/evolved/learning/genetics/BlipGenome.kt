package thecodewarrior.evolved.learning.genetics

import org.neuroph.core.Connection
import org.neuroph.core.NeuralNetwork
import org.neuroph.core.Neuron
import org.neuroph.core.data.DataSet
import org.neuroph.core.learning.UnsupervisedLearning
import org.neuroph.nnet.comp.neuron.InputOutputNeuron
import org.neuroph.nnet.comp.neuron.ThresholdNeuron
import org.neuroph.nnet.learning.UnsupervisedHebbianLearning
import org.neuroph.util.*
import thecodewarrior.evolved.*
import thecodewarrior.evolved.learning.EnumAction
import thecodewarrior.evolved.learning.EnumSense
import thecodewarrior.evolved.learning.network.NetConst
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by TheCodeWarrior
 */
class BlipGenome(val brain: Brain = Brain(mutableListOf(*Array(ThreadLocalRandom.current().nextInt(2, 6)) { NetCodec.def.random(ThreadLocalRandom.current()) })), val mainData: MainGenomeData = MainGenomeDataCodec.create(MainGenomeDataCodec.def.random(ThreadLocalRandom.current()))) {

    fun breed(other: BlipGenome): BlipGenome {
        return BlipGenome(this.brain.breed(other.brain), MainGenomeDataCodec.create(MainGenomeDataCodec.def.crossover(ThreadLocalRandom.current(), this.mainData.genome, other.mainData.genome).first))
    }

}

class MainGenomeData(val genome: Chromosome, val fov: Double, val visionResolution: Int, val maxAge: Int) {

}

object MainGenomeDataCodec : ChromosomeCodec<MainGenomeData> {
    override val def: ChromosomeDef = ChromosomeDef(
            DoubleGeneDef(1.0, 360.0), IntGeneDef(1, 9), IntGeneDef(1000, 10000)
    )

    override fun create(chromosome: Chromosome): MainGenomeData {
        val seq = chromosome.seq()
        return MainGenomeData(
                chromosome,
                seq.next().get<Double>(),
                seq.next().get<Int>(),
                seq.next().get<Int>()
        )
    }

}

class Brain(val genes: List<Chromosome>) {
    val nets: MutableList<Net> = genes.map { NetCodec.create(it) }.toMutableList()
    val inputMap = mutableMapOf<EnumSense, MutableList<Pair<Neuron, Double>>>()
    val outputMap = mutableMapOf<EnumAction, MutableList<Neuron>>()

    val inputValues = mutableMapOf<EnumSense, DoubleArray>()

    init {
        nets.removeAll {
            if(it.neurons.inputsCount == 0 || it.neurons.outputsCount == 0)
                return@removeAll true
            it.neurons.layers.forEachIndexed { iLayer, layer ->
                if(iLayer == 0)
                    return@forEachIndexed
                if(layer.neurons.none { it.hasInputConnections() }) {
                    return@removeAll true
                }
            }
            false
        }
        nets.forEach { net ->
            net.neurons.inputNeurons.forEachIndexed { iNeuron, neuron ->
                inputMap.getOrPut(net.inputs[iNeuron].first, { mutableListOf() }).add(neuron to net.inputs[iNeuron].second)
            }
            net.neurons.outputNeurons.forEachIndexed { iNeuron, neuron ->
                outputMap.getOrPut(net.outputs[iNeuron], { mutableListOf() }).add(neuron)
            }
        }
    }

    operator fun set(key: EnumSense, values: DoubleArray) {
        inputValues[key] = values
    }

    operator fun get(key: EnumAction): Double {
        return outputMap[key]?.fold(0.0) { s, i ->
            s + i.output
        }?.let { it / Math.min(1.0, outputMap[key]!!.size.toDouble()) }?.clamp(key.min, key.max) ?: 0.0
    }

    fun breed(other: Brain): Brain {
        return Brain(breedNets(this.genes, other.genes))
    }

    fun update() {

        inputMap.forEach {
            val sense = it.key
            it.value.forEach {
                val (neuron, f) = it
                neuron.setInput(interp(sense.rangeMin, sense.rangeMax, f, inputValues[sense] ?: doubleArrayOf(0.0)))
            }
        }

        nets.forEach {
            val set = DataSet(it.neurons.inputsCount)
            set.addRow(it.neurons.inputNeurons.map { it.netInput }.toDoubleArray())
            (it.neurons.learningRule as? UnsupervisedHebbianLearning)?.doLearningEpoch(set)
        }
    }

    fun interp(min: Double, max: Double, point: Double, array: DoubleArray): Double {
        if(array.isEmpty())
            return 0.0

        val frac = (point-min)/(max-min) // get the fraction through the array from 0-1

        val partialIndex = frac * array.size // get the fractional index in the array

        if(partialIndex < 0) {
            return array[0]
        }
        if(partialIndex > array.size-1) {
            return array[array.size-1]
        }
        if(partialIndex.toInt().toDouble() == partialIndex) {
            return array[partialIndex.toInt()]
        }


        val lower = array[partialIndex.floor()] // value below
        val higher = array[partialIndex.ceil()] // value above
        val foo = partialIndex - partialIndex.floor() // from 0 ( = below ) to 1 ( = above)

        return lower + (higher - lower) * foo
    }

    fun breedNets(a: List<Chromosome>, b: List<Chromosome>): MutableList<Chromosome> {
        val rand = ThreadLocalRandom.current()
        // For the overlap, one of either of the genomes will be chosen to provide that chromosme
        // For overflow, NetConst.EXTRA_CHANCE % will be inherited
        // Either:
        //     One pair (don't have to be adjacent) will be crossed over (~ in diagram)
        //     Or one chromosome will be mutated
        //  this: c c c c c c c c c c
        //          |     |   |     |
        //   new: c c C c c c~c c c c   c c   c
        //        |   |     |   | |     | |   |
        // other: c c c c c c c c c c c c c c c


        // create a new genome
        val genome = mutableListOf<Chromosome>()

        // add overlap chromosomes
        val overlap = Math.min(a.size, b.size)

        for(i in 0..overlap-1) {
            genome.add(if(rand.nextBoolean()) a[i] else b[i])
        }

        // add extra chromosomes

        if(a.size != b.size) {
            val bigger = if (a.size > b.size) a else b // pick out the biggest one
            for(i in overlap..bigger.size-1) {
                if(rand.nextFloat() < GeneConst.EXTRA_CHANCE) {
                    genome.add(bigger[i])
                }
            }
        }

        // so that the next generation won't always inherit the same first genes, if I don't do this the "overflow"
        // genes will tend not to be inherited
        // randomized already
        for(i in 0..ThreadLocalRandom.current().nextInt(0, Math.min(1, genome.size/2))) {
            val picked = genome.pickIndices(2)
            val tmp = genome[picked[0]]
            genome[picked[0]] = genome[picked[1]]
            genome[picked[1]] = tmp
        }
//        Collections.shuffle(genome)

        // mutate one and/or cross over two random chromosomes
        var rnd = rand.nextDouble()
        val r = { chance: Double -> rnd -= chance; chance < 0 }
        if(r(GeneConst.CHROMOSOME_SHUFFLE_CHANCE) && genome.size > 1) { // cross over
            val picked = genome.pickIndices(2)
            val (a, b) = NetCodec.def.crossover(ThreadLocalRandom.current(), genome[picked[0]], genome[picked[1]])
            genome[picked[0]] = a
            genome[picked[1]] = b
        }
        else if(r(GeneConst.CHROMOSOME_MUTATE_CHANCE)) { // mutate
            val i = genome.indices.pick()
            genome[i] = NetCodec.def.mutate(ThreadLocalRandom.current(), genome[i])
        }
        else if(r(GeneConst.CHROMOSOME_DELETE_CHANCE)) { // delete one
            val i = genome.indices.pick()
            genome.removeAt(i)
        }
        else if(r(GeneConst.CHROMOSOME_NEW_CHANCE)) { // create a new one
            genome.add(NetCodec.def.random(ThreadLocalRandom.current()))
        }

        return genome
    }
}

class Net(val neurons: NeuralNetwork<UnsupervisedLearning>, val inputs: List<Pair<EnumSense, Double>>, val outputs: List<EnumAction>) {
}


/**
 * A pseudo-chromosome that stores an entire net
 */
object NetCodec : ChromosomeCodec<Net>{

    override val def: ChromosomeDef

    init {
        val input = arrayOf(EnumGeneDef(EnumSense::class.java).chr('I'), DoubleGeneDef(0.0, 1.0).chr('F'))
        val output = arrayOf(EnumGeneDef(EnumAction::class.java).chr('A'))
        val connection = arrayOf(BoolGeneDef().chr('C'), GaussianDoubleGeneDef(1.0).chr('W'))
        val neuron = arrayOf(BoolGeneDef().chr('N'), GaussianDoubleGeneDef(2.0).chr('T'))

        val inputNeuron = arrayOf(*neuron)
        val outputNeuron = arrayOf(*neuron, *connection.repeat(NetConst.HIDDEN_WIDTH))
        val hiddenNeuron = arrayOf(*neuron, *connection.repeat(NetConst.HIDDEN_WIDTH))
        val hiddenNeuronFirstLayer = arrayOf(*neuron, *connection.repeat(NetConst.INPUT_COUNT))

        val main = arrayOf(
                *input.repeat(NetConst.INPUT_COUNT),
                *output.repeat(NetConst.OUTPUT_COUNT),
                *inputNeuron.repeat(NetConst.INPUT_COUNT),
                *hiddenNeuronFirstLayer.repeat(NetConst.HIDDEN_WIDTH),
                *hiddenNeuron.repeat(NetConst.HIDDEN_WIDTH*(NetConst.HIDDEN_DEPTH-1)),
                *outputNeuron.repeat(NetConst.OUTPUT_COUNT)
        )

        def = ChromosomeDef(*main)
        println(def.desc())
    }

    override fun create(chromosome: Chromosome) : Net {
        val seq = chromosome.seq()

        val net = NeuralNetwork<UnsupervisedLearning>()

        val inputProps = NeuronProperties(InputOutputNeuron::class.java)
        inputProps.setProperty("transferFunction", TransferFunctionType.LINEAR)
        inputProps.setProperty("transferFunction.slope", 1.0)

        val outputProps = NeuronProperties(InputOutputNeuron::class.java)
        outputProps.setProperty("transferFunction", TransferFunctionType.LINEAR)
        outputProps.setProperty("transferFunction.slope", 1.0)

        val hiddenProps = NeuronProperties(ThresholdNeuron::class.java)
        hiddenProps.setProperty("transferFunction", TransferFunctionType.LINEAR)
        hiddenProps.setProperty("transferFunction.slope", 1.0)

        net.networkType = NeuralNetworkType.UNSUPERVISED_HEBBIAN_NET


        // create all the neuron layers
        net.addLayer(LayerFactory.createLayer(NetConst.INPUT_COUNT, inputProps))

        for(h in 0..NetConst.HIDDEN_DEPTH-1) {
            net.addLayer(LayerFactory.createLayer(NetConst.HIDDEN_WIDTH, hiddenProps))
        }

        net.addLayer(LayerFactory.createLayer(NetConst.OUTPUT_COUNT, outputProps))


        // create all possible connections, and assign them their weight
        // for layer in layers
        //    next = layer.nextLayer
        //    for from in layer.neurons
        //        for to in next.neurons
        //            addConnection(from, to, weight[i++])
        net.layers.forEachIndexed { iLayer, layer ->
            if(iLayer+1 < net.layers.size) {
                val next = net.getLayerAt(iLayer+1)
                layer.neurons.forEach { from ->
                    next.neurons.forEach { to ->
                        net.createConnection(from, to, 0.0)
                    }
                }
            }
        }

        // remove neurons and connections based on the genes
        // format is:
        // NTCCCC NTCCCC NTCCCC NTCCCC NTCCCC ... (N = neuron existence, T = neuron threshold, C = input connection existence)
        // They are interlaced so related genes stay near each other. This increases the likelihood that a slice will
        // catch both the neuron and its connections.
        val inputsArr = Array(NetConst.INPUT_COUNT) { val e = seq.next().get<EnumSense>(); e to (seq.next().get<Double>()*(e.rangeMax-e.rangeMin)+e.rangeMin) }
        val outputsArr = Array(NetConst.OUTPUT_COUNT) { seq.next().get<EnumAction>(); EnumAction.values()[it] }

        val inputs = mutableListOf<Pair<EnumSense, Double>>()
        val outputs = mutableListOf<EnumAction>()

        var ioIndex = 0

        net.layers.forEachIndexed { iLayer, layer ->
            val toRemove = mutableListOf<Int>()
            val connectionsToRemove = mutableSetOf<Connection>()
            ioIndex = 0
            layer.neurons.forEachIndexed { iNeuron, neuron ->
                if(!seq.next().get<Boolean>()) {
                    toRemove.add(iNeuron)
                    seq.next() // consume the threshold
                } else {
                    val threshold = seq.next().get<Double>()
                    (neuron as? ThresholdNeuron)?.thresh = threshold

                    if(iLayer == 0) {
                        inputs.add(inputsArr[ioIndex++])
                    } else if(iLayer == NetConst.HIDDEN_DEPTH+1){
                        outputs.add(outputsArr[ioIndex++])
                    }
                }
                neuron.inputConnections.forEach { con ->
                    if(seq.next().get<Boolean>()) {
                        connectionsToRemove.add(con)
                        seq.next()
                    } else {
                        con.weight.setValue(seq.next().get<Double>())
                    }
                }
            }

            connectionsToRemove.forEach { con ->
                con.fromNeuron.removeOutputConnectionTo(con.toNeuron)
            }
            toRemove.reversed().forEach { index -> // reverse because otherwise the indexes always change as I remove elements
                layer.removeNeuronAt(index)
            }
        }

        // remove all isolated neurons

        var removed = false
        do {
            removed = false
            val lastLayer = net.layersCount-1
            net.layers.forEachIndexed { iLayer, layer ->
                val toRemove = mutableListOf<Neuron>()
                layer.neurons.forEach { neuron ->
                    if ((neuron.inputConnections.isEmpty() && iLayer != 0) || (neuron.outConnections.isEmpty() && iLayer != lastLayer)) {
                        toRemove.add(neuron)
                    }
                }
                if(toRemove.size > 0)
                    removed = true
                toRemove.forEach { neuron ->
                    layer.removeNeuron(neuron)
                }
            }
        } while(removed)

        net.randomizeWeights(-1.0, 2.0)

//        net.inputNeurons = net.getLayerAt(0).neurons.toMutableList()
//        net.outputNeurons = net.getLayerAt(NetConst.HIDDEN_DEPTH+1).neurons.toMutableList()

        NeuralNetworkFactory.setDefaultIO(net)
        net.learningRule = UnsupervisedHebbianLearning()

        return Net(net, inputs, outputs)
    }

}
