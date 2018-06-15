package io.kloudformation

object SpecificationMerger{
    fun merge(specifications: List<Specification>): Specification {
        return specifications.reduce { mergedSpec, currentSpec -> mergedSpec.copy(
                    propertyTypes = mergedSpec.propertyTypes + mergedSpec.propertyTypes.keys
                            .filter { currentSpec.propertyTypes.keys.contains(it) }
                            .map { it to mergedSpec.propertyTypes[it]!! }
                            .toMap(),
                    resourceTypes = mergedSpec.resourceTypes + mergedSpec.resourceTypes.keys
                            .filter { currentSpec.resourceTypes.keys.contains(it) }
                            .map { it to mergedSpec.resourceTypes[it]!! }
                            .toMap())
        }
    }
}