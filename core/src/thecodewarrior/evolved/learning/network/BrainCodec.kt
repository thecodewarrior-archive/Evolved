package thecodewarrior.evolved.learning.network

import org.neuroph.core.Connection
import org.neuroph.core.NeuralNetwork
import org.neuroph.core.Neuron
import org.neuroph.core.learning.UnsupervisedLearning
import org.neuroph.nnet.learning.UnsupervisedHebbianLearning
import org.neuroph.util.*

/**
 *
 *
 */
object BrainCodec {



    fun decode(neurons: BooleanArray, connections: DoubleArray): NeuralNetwork<UnsupervisedLearning> {


        val net = NeuralNetwork<UnsupervisedLearning>()

        val neuronProperties = NeuronProperties()
        neuronProperties.setProperty("transferFunction", TransferFunctionType.LINEAR)
        neuronProperties.setProperty("transferFunction.slope", 1)

        net.networkType = NeuralNetworkType.UNSUPERVISED_HEBBIAN_NET


        // create all the neuron layers
        net.addLayer(LayerFactory.createLayer(NetConst.INPUT_COUNT, neuronProperties))

        for(h in 0..NetConst.HIDDEN_DEPTH-1) {
            net.addLayer(LayerFactory.createLayer(NetConst.HIDDEN_WIDTH, neuronProperties))
        }

        net.addLayer(LayerFactory.createLayer(NetConst.OUTPUT_COUNT, neuronProperties))

        // create all possible connections, and assign them their weight
        // for layer in layers
        //    next = layer.nextLayer
        //    for from in layer.neurons
        //        for to in next.neurons
        //            addConnection(from, to, weight[i++])
        var i = 0
        net.layers.forEachIndexed { iLayer, layer ->
            if(iLayer+1 < net.layers.size) {
                val next = net.getLayerAt(iLayer+1)
                layer.neurons.forEach { from ->
                    next.neurons.forEach { to ->
                        net.createConnection(from, to, connections[i++])
                    }
                }
            }
        }

        // remove all hidden neurons with a false flag in `neurons`
        // also remove all their related connections
        i = 0
        net.layers.forEachIndexed { iLayer, layer ->
            val toRemove = mutableSetOf<Int>()
            if(iLayer == 0 || iLayer > NetConst.HIDDEN_DEPTH)
                return@forEachIndexed
            layer.neurons.forEachIndexed { iNeuron, neuron ->
                if(!neurons[i++])
                    toRemove.add(iNeuron)
            }
            toRemove.forEach { index ->
                layer.getNeuronAt(index).removeAllConnections()
                layer.removeNeuronAt(index)
            }
        }

        // remove all connections with a weight sufficiently close to 0

        net.layers.forEach { layer ->
            layer.neurons.forEach { neuron ->
                val toRemove = mutableSetOf<Connection>()

                neuron.inputConnections.forEach {
                    if(Math.abs(it.weight.value) < NetConst.MIN_WEIGHT) {
                        toRemove.add(it)
                    }
                }
                toRemove.forEach {
                    it.toNeuron.removeInputConnectionFrom(it.fromNeuron) // the reverse is automatically performed
                }
            }
        }

        // remove all isolated neurons

        var removed = false
        do {
            removed = false
            net.layers.forEach { layer ->
                val toRemove = mutableSetOf<Neuron>()
                layer.neurons.forEach { neuron ->
                    if (neuron.inputConnections.isEmpty() || neuron.outConnections.isEmpty()) {
                        toRemove.add(neuron)
                    }
                }
                if(toRemove.size > 0)
                    removed = true
                // remove all related connections first
                toRemove.forEach { neuron ->
                    neuron.removeAllConnections()
                    layer.removeNeuron(neuron)
                }
            }
        } while(removed)

        NeuralNetworkFactory.setDefaultIO(net)
        net.learningRule = UnsupervisedHebbianLearning()

        return net
    }
}
