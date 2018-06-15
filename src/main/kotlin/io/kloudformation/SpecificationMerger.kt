package io.kloudformation

object SpecificationMerger{
    fun merge(specifications: List<Specification>): Specification {
        return specifications.reduce { mergedSpec, currentSpec -> mergedSpec.copy(
                    PropertyTypes = mergedSpec.PropertyTypes + mergedSpec.PropertyTypes.keys
                            .filter { currentSpec.PropertyTypes.keys.contains(it) }
                            .map { it to mergedSpec.PropertyTypes[it]!! }
                            .toMap(),
                    ResourceTypes = mergedSpec.ResourceTypes + mergedSpec.ResourceTypes.keys
                            .filter { currentSpec.ResourceTypes.keys.contains(it) }
                            .map { it to mergedSpec.ResourceTypes[it]!! }
                            .toMap())
        }
    }
}