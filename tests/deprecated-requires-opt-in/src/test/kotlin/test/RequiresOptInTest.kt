package test

import com.apollographql.apollo.annotations.ApolloRequiresOptIn
import com.apollographql.apollo.api.Optional
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import custom.GetNewFieldQuery as GetNewFieldQueryCustom
import custom.type.Direction as DirectionCustom
import custom.type.SomeInput as SomeInputCustom
import default.GetNewFieldQuery as GetNewFieldQueryDefault
import default.type.Direction as DirectionDefault
import default.type.SomeInput as SomeInputDefault
import none.GetNewFieldQuery as GetNewFieldQueryNone
import none.type.Direction as DirectionNone
import none.type.SomeInput as SomeInputNone
import com.example.MyRequiresOptIn

@Suppress("DEPRECATION")
class RequiresOptInTest {
  /**
   * Visual test: this allows to see how the annotation is used but doesn't actually test anything
   */
  @Test
  fun visualTest() {
    SomeInputNone(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    @OptIn(ApolloRequiresOptIn::class)
    SomeInputDefault(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    @OptIn(MyRequiresOptIn::class)
    SomeInputCustom(
        newInputField = Optional.Present(9),
        oldInputField = Optional.Present(0),
    ).apply {
      oldInputField
      newInputField
    }

    GetNewFieldQueryNone.Data(
        newField = DirectionNone.NORTH,
        oldField = DirectionNone.SOUTH
    ).apply {
      oldField
      newField
    }

    @OptIn(ApolloRequiresOptIn::class)
    GetNewFieldQueryDefault.Data(
        newField = DirectionDefault.NORTH,
        oldField = DirectionDefault.SOUTH
    ).apply {
      oldField
      newField
    }

    @OptIn(MyRequiresOptIn::class)
    GetNewFieldQueryCustom.Data(
        newField = DirectionCustom.NORTH,
        oldField = DirectionCustom.SOUTH
    ).apply {
      oldField
      newField
    }
  }

  /**
   * This test retrieves the reflection at runtime
   *
   * Because [ApolloRequiresOptIn] is on PROPERTY, Kotlin puts the annotation on a synthetic method
   */
  @Test
  fun reflectionTest() {
    val noneInputClass = SomeInputNone::class.java
    val noneInputAnnotationMethod = noneInputClass.declaredMethods.firstOrNull { it.name == "getNewInputField${'$'}annotations" }
    assertTrue(noneInputAnnotationMethod == null)

    val defaultInputClass = SomeInputDefault::class.java
    val defaultInputAnnotationsMethod = defaultInputClass.declaredMethods.firstOrNull { it.name == "getNewInputField${'$'}annotations" }
    assertFalse(defaultInputAnnotationsMethod?.declaredAnnotations?.any { it.annotationClass.simpleName == "ApolloRequiresOptIn"} == true)

    val customInputClass = SomeInputCustom::class.java
    val customInputAnnotationsMethod = customInputClass.declaredMethods.firstOrNull { it.name == "getNewInputField${'$'}annotations" }
    assertTrue(customInputAnnotationsMethod?.declaredAnnotations?.any { it.annotationClass.simpleName == "MyRequiresOptIn"} == true)

    assertFalse(DirectionDefault.NORTH.javaClass.getField("NORTH").declaredAnnotations.any { it.annotationClass.simpleName == "ApolloRequiresOptIn"})
    @OptIn(MyRequiresOptIn::class)
    assertTrue(DirectionCustom.NORTH.javaClass.getField("NORTH").declaredAnnotations.any { it.annotationClass.simpleName == "MyRequiresOptIn"})

    val noneClass = GetNewFieldQueryNone.Data::class.java
    val noneAnnotationMethod = noneClass.declaredMethods.firstOrNull { it.name == "getNewField${'$'}annotations" }
    assertTrue(noneAnnotationMethod == null)

    val defaultClass = GetNewFieldQueryDefault.Data::class.java
    val defaultAnnotationsMethod = defaultClass.declaredMethods.firstOrNull { it.name == "getNewField${'$'}annotations" }
    assertFalse(defaultAnnotationsMethod?.declaredAnnotations?.any { it.annotationClass.simpleName == "ApolloRequiresOptIn"} == true)

    val customClass = GetNewFieldQueryCustom.Data::class.java
    val customAnnotationsMethod = customClass.declaredMethods.firstOrNull { it.name == "getNewField${'$'}annotations" }
    assertTrue(customAnnotationsMethod?.declaredAnnotations?.any { it.annotationClass.simpleName == "MyRequiresOptIn"} == true)
  }
}
