package kioskware.vision.processor

/**
 * A class that holds the results of image processing operations.
 * It maps each processor to its corresponding result.
 * @property results A map where keys are processors and values are their results.
 */
@Suppress("UNCHECKED_CAST")
class ProcessingResults internal constructor(
    private val results: Map<ImageProcessor<*>, Any?>
) {

    companion object{
        /**
         * An empty instance of ProcessingResults with no results.
         */
        @JvmStatic
        val Empty = ProcessingResults(emptyMap())
    }

    /**
     * Retrieves the result for a specific processor.
     * @param processor The processor whose result is to be retrieved.
     * @return The result of the specified processor, or null if not found.
     */
    fun <T> getResults(processor: ImageProcessor<T>) : T? {
        return results[processor] as? T
    }

    /**
     * Retrieves all results as a list.
     * @return A list of all results from the processing operations.
     */
    fun getAll(): List<Pair<ImageProcessor<*>, Any?>> {
        return results.entries.map { entry ->
            Pair(entry.key, entry.value)
        }
    }

    /**
     * Retrieves all results as a list of values.
     * @return A list of all values from the processing operations.
     */
    fun getAllValues(): List<Any?> {
        return results.values.toList()
    }

}