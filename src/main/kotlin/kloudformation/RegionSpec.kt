package kloudformation

data class RegionSpec(
        val PropertyTypes: Map<String, PropertyInfo>,
        val ResourceTypes: Map<String, PropertyInfo>,
        val ResourceSpecificationVersion: String
){
    fun merge(regionSpec: RegionSpec): RegionSpec{
        val newResources = regionSpec.ResourceTypes.withoutAnythingIn(this.ResourceTypes)
        val newProperties = regionSpec.PropertyTypes.withoutAnythingIn(this.PropertyTypes)
        return this.copy(PropertyTypes = this.PropertyTypes + newProperties, ResourceTypes = this.ResourceTypes + newResources)
    }
}

data class PropertyInfo(
        val Documentation: String,
        val Properties: Map<String, Property>,
        val Attributes: Map<String, Attribute>? = null,
        val AdditionalProperties: Boolean? = null
)
data class Property(
        val Documentation: String,
        val Required: Boolean,
        val UpdateType: String,
        val Type: String? = null,
        val DuplicatesAllowed: Boolean? = null,
        val ItemType: String? = null,
        val PrimitiveType: String? = null,
        val PrimitiveItemType: String? = null
)

data class Attribute(
        val Type: String? = null,
        val DataSourceArn: String? = null,
        val PrimitiveType: String? = null,
        val PrimitiveItemType: String? = null
)

fun <T> Collection<T>.withoutAnythingIn(otherList: Collection<T>) = this.filter { otherList.contains(it) }
fun <K, T> Map<K, T>.withoutAnythingIn(otherMap: Map<K,T>) = otherMap.keys.withoutAnythingIn(this.keys).map { it to otherMap[it]!! }.toMap()