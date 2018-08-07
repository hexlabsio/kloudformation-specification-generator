package io.kloudformation.function

import io.kloudformation.Value

open class Reference<T>(val ref: String): Value<T>, Cidr.Value<T>, Att.Value<T>, Select.ObjectValue<T>