package com.example.mutation_create_review.type

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.InputType
import java.io.IOException
import java.util.Date
import javax.annotation.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.Throws

/**
 * The input object sent when someone is creating a new review
 * @param stars 0-5 stars
 * @param nullableIntFieldWithDefaultValue for test purpose only
 * @param commentary Comment about the movie, optional
 * @param favoriteColor Favorite color, optional
 * @param enumWithDefaultValue for test purpose only
 * @param nullableEnum for test purpose only
 * @param listOfCustomScalar for test purpose only
 * @param customScalar for test purpose only
 * @param listOfEnums for test purpose only
 * @param listOfInt for test purpose only
 * @param listOfString for test purpose only
 * @param booleanWithDefaultValue for test purpose only
 * @param listOfListOfString for test purpose only
 * @param listOfListOfEnum for test purpose only
 * @param listOfListOfCustom for test purpose only
 * @param listOfListOfObject for test purpose only
 * @param capitalizedField for test purpose only
 */
@Generated("Apollo GraphQL")
class ReviewInput(
    val stars: Int,
    val nullableIntFieldWithDefaultValue: Input<Int> = Input.optional(10),
    val commentary: Input<String> = Input.optional(null),
    val favoriteColor: ColorInput,
    val enumWithDefaultValue: Input<Episode> = Input.optional(Episode.safeValueOf("JEDI")),
    val nullableEnum: Input<Episode> = Input.optional(null),
    val listOfCustomScalar: Input<List<Date>> = Input.optional(null),
    val customScalar: Input<Date> = Input.optional(null),
    val listOfEnums: Input<List<Episode>> = Input.optional(null),
    val listOfInt: Input<List<Int>> = Input.optional(listOf(1, 2, 3)),
    val listOfString: Input<List<String>> = Input.optional(listOf("test1", "test2", "test3")),
    val booleanWithDefaultValue: Input<Boolean> = Input.optional(true),
    val listOfListOfString: Input<List<List<String>>> = Input.optional(null),
    val listOfListOfEnum: Input<List<List<Episode>>> = Input.optional(null),
    val listOfListOfCustom: Input<List<List<Date>>> = Input.optional(null),
    val listOfListOfObject: Input<List<List<ColorInput>>> = Input.optional(null),
    val capitalizedField: Input<String> = Input.optional(null)
) : InputType {
    override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
        @Throws(IOException::class)
        override fun marshal(writer: InputFieldWriter) {
            writer.writeInt("stars", stars)
            writer.writeInt("nullableIntFieldWithDefaultValue", nullableIntFieldWithDefaultValue)
            writer.writeString("commentary", commentary)
            writer.writeObject("favoriteColor", favoriteColor.marshaller())
            if (enumWithDefaultValue.defined) writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value?.rawValue)
            if (nullableEnum.defined) writer.writeString("nullableEnum", nullableEnum.value?.rawValue)
            if (listOfCustomScalar.defined) {
                writer.writeList("listOfCustomScalar", listOfCustomScalar.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeCustom(CustomType.DATE, it)
                            }
                        }
                    }
                } ?: null)
            }
            writer.writeCustom("customScalar", CustomType.DATE, customScalar.value)
            if (listOfEnums.defined) {
                writer.writeList("listOfEnums", listOfEnums.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeString(it?.rawValue)
                            }
                        }
                    }
                } ?: null)
            }
            if (listOfInt.defined) {
                writer.writeList("listOfInt", listOfInt.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeInt(it)
                            }
                        }
                    }
                } ?: null)
            }
            if (listOfString.defined) {
                writer.writeList("listOfString", listOfString.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeString(it)
                            }
                        }
                    }
                } ?: null)
            }
            writer.writeBoolean("booleanWithDefaultValue", booleanWithDefaultValue)
            if (listOfListOfString.defined) {
                writer.writeList("listOfListOfString", listOfListOfString.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeList(object : InputFieldWriter.ListWriter {
                                    override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                                        it.foreach {
                                            listItemWriter.writeString(it)
                                        }
                                    }
                                })
                            }
                        }
                    }
                } ?: null)
            }
            if (listOfListOfEnum.defined) {
                writer.writeList("listOfListOfEnum", listOfListOfEnum.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeList(object : InputFieldWriter.ListWriter {
                                    override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                                        it.foreach {
                                            listItemWriter.writeString(it?.rawValue)
                                        }
                                    }
                                })
                            }
                        }
                    }
                } ?: null)
            }
            if (listOfListOfCustom.defined) {
                writer.writeList("listOfListOfCustom", listOfListOfCustom.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeList(object : InputFieldWriter.ListWriter {
                                    override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                                        it.foreach {
                                            listItemWriter.writeCustom(CustomType.DATE, it)
                                        }
                                    }
                                })
                            }
                        }
                    }
                } ?: null)
            }
            if (listOfListOfObject.defined) {
                writer.writeList("listOfListOfObject", listOfListOfObject.value?.let {
                    object : InputFieldWriter.ListWriter {
                        override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                            it.foreach {
                                listItemWriter.writeList(object : InputFieldWriter.ListWriter {
                                    override fun write(listItemWriter: InputFieldWriter.ListItemWriter) {
                                        it.foreach {
                                            listItemWriter.writeObject(it.marshaller())
                                        }
                                    }
                                })
                            }
                        }
                    }
                } ?: null)
            }
            writer.writeString("CapitalizedField", capitalizedField)
        }
    }
}
