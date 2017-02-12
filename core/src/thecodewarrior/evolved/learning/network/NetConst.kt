package thecodewarrior.evolved.learning.network

import thecodewarrior.evolved.learning.EnumAction

/**
 * Created by TheCodeWarrior
 */
object NetConst {
    /** the max width of hidden layers */
    val HIDDEN_WIDTH = 3
    /** the depth of hidden layers */
    val HIDDEN_DEPTH = 1
    /** the number of inputs in a net */
    val INPUT_COUNT = 5
    /** the number of outputs in a net */
    val OUTPUT_COUNT = EnumAction.values().size
    /** the absolute value below which a connection is removed (if weight.abs < this; delete connection) */
    val MIN_WEIGHT = 0.01
    /** the max weight for a connection (min/max are +- this value) */
    val MAX_WEIGHT = 20.0

    /** the number of connections possible for a max-size net */
    val POSSIBLE_CONNECTIONS =
            INPUT_COUNT * HIDDEN_WIDTH + // connections between input and first hidden layer
                    HIDDEN_WIDTH * OUTPUT_COUNT + // connections between last hidden layer and output
                    (HIDDEN_WIDTH * HIDDEN_WIDTH) * // connections between each hidden layer
                            (HIDDEN_DEPTH-1) // if there are 3 hidden layers, there are two sets of hidden -> hidden connections
    val TOTAL_NEURONS = INPUT_COUNT + OUTPUT_COUNT + HIDDEN_WIDTH * HIDDEN_DEPTH

}
