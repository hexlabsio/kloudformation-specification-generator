package kloudformation

data class RegionSpec(
        val PropertyTypes: Map<String, PropertyInfo>,
        val ResourceTypes: Map<String, PropertyInfo>,
        val ResourceSpecificationVersion: String
){
    infix fun and(regionSpec: RegionSpec) = this.copy(
               PropertyTypes = this.PropertyTypes and regionSpec.PropertyTypes,
               ResourceTypes = this.ResourceTypes and regionSpec.ResourceTypes
       )

    companion object {
        private infix fun Map<String, PropertyInfo>.and(otherProperties: Map<String, PropertyInfo>) =
            this.map {
                it.key to (
                        if(otherProperties.containsKey(it.key)) it.value and otherProperties[it.key]!!
                        else it.value
                )
            }.toMap() + otherProperties.withoutAnythingIn(this)
        }
}

data class PropertyInfo(
        val Documentation: String,
        val Properties: Map<String, Property>,
        val Attributes: Map<String, Attribute>? = null,
        val AdditionalProperties: Boolean? = null
){
    infix fun and(property: PropertyInfo) = this
}
data class Property(
        val Documentation: String,
        val Required: Boolean,
        val UpdateType: String,
        val Type: String? = null,
        val DuplicatesAllowed: Boolean? = null,
        val ItemType: String? = null,
        val PrimitiveType: String? = null,
        val PrimitiveItemType: String? = null
){
    infix fun and(property: Property) = this.copy(
            Type = this.Type ?: property.Type,
            PrimitiveType = this.PrimitiveType ?: property.PrimitiveType,
            PrimitiveItemType = this.PrimitiveItemType ?: property.PrimitiveItemType,
            DuplicatesAllowed = this.DuplicatesAllowed ?: property.DuplicatesAllowed,
            ItemType = this.ItemType ?: property.ItemType
    )
}

data class Attribute(
        val Type: String? = null,
        val DataSourceArn: String? = null,
        val PrimitiveType: String? = null,
        val PrimitiveItemType: String? = null
){
    infix fun and(attribute: Attribute) = this.copy(
            Type = this.Type ?: attribute.Type,
            DataSourceArn = this.DataSourceArn ?: attribute.DataSourceArn,
            PrimitiveType = this.PrimitiveType ?: attribute.PrimitiveType,
            PrimitiveItemType = this.PrimitiveItemType ?: attribute.PrimitiveItemType
    )
}

fun <T> Collection<T>.withoutAnythingIn(otherList: Collection<T>) = this.filter { otherList.contains(it) }
fun <K, T> Map<K, T>.withoutAnythingIn(otherMap: Map<K,T>) = otherMap.keys.withoutAnythingIn(this.keys).map { it to otherMap[it]!! }.toMap()