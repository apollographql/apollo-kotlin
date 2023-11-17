package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.codegen.kotlin.experimental.ExplicitlyRemovedNode
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.google.common.truth.Truth
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import kotlin.test.Test

class FlattenKtTest {


  @Test
  fun testMaybeFlatten() {
    val testIrModelGroup = createTestIrModelGroup()
    val flatten = testIrModelGroup.maybeFlatten(true, createTestNode())
    Truth.assertThat(flatten).hasSize(16)
  }

  @Test
  fun testWalk() {
    // Mock test data
    val testNode = createTestNode()
    val testIrModelGroup = createTestIrModelGroup()

    // Starting values
    val matchedImgs = mutableListOf(ArrayDeque<IrModelGroup>())
    val matchedIms = mutableListOf(ArrayDeque<IrModel>())

    walk(true, testNode, listOf(testIrModelGroup), matchedImgs, matchedIms)
    Truth.assertThat(matchedImgs).hasSize(6)
    Truth.assertThat(matchedIms).hasSize(6)

    val irModelGroupIds = arrayOf(
      arrayOf("operation.IMG_a.Data", "IMG_3"),
      arrayOf("IMG_ra", "IMG_rr"),
      arrayOf("IMG_n", "IMG_bb"),
      arrayOf("IMG_ra", "IMG_nn"),
      arrayOf("IMG_a3", "IMG_a4"),
    )
    val irModelValues = arrayOf(
      arrayOf("IM_b", "IM_c"),
      arrayOf("IM_d", "IM_e"),
      arrayOf("IM_f", "IM_j"),
      arrayOf("IM_h", "IM_i"),
      arrayOf("IM_k", "IM_l"),
    )

    irModelGroupIds.forEachIndexed { index, ids ->
      matchedImgs[index].forEachIndexed { i, irModelGroup ->
        Truth.assertThat(irModelGroup.baseModelId).isEqualTo(ids[i])
      }
    }
    irModelValues.forEachIndexed { index, ids ->
      matchedIms[index].forEachIndexed { i, irModel ->
        Truth.assertThat(irModel.modelName).isEqualTo(ids[i])
      }
    }
  }

  @Test
  fun testRebuildIrModelGroups() {
    // Mock test data
    val testNode = createTestNode()
    val testIrModelGroup = createTestIrModelGroup()
    val matchedImgs = mutableListOf(ArrayDeque<IrModelGroup>())
    val matchedIms = mutableListOf(ArrayDeque<IrModel>())

    // Prepare test data
    walk(true, testNode, listOf(testIrModelGroup), matchedImgs, matchedIms)
    matchedImgs.popLast()
    matchedIms.popLast()

    // Test
    val testResults = rebuildIrModelGroups(matchedImgs, matchedIms)
    Truth.assertThat(testResults).hasSize(6)
    Truth.assertThat(testResults[0].baseModelId).isEqualTo("operation.IMG_a.Data")
    Truth.assertThat(testResults[1].baseModelId).isEqualTo("IMG_3")
    Truth.assertThat(testResults[2].baseModelId).isEqualTo("IMG_rr")
    Truth.assertThat(testResults[3].baseModelId).isEqualTo("IMG_bb")
    Truth.assertThat(testResults[4].baseModelId).isEqualTo("IMG_nn")
    Truth.assertThat(testResults[5].baseModelId).isEqualTo("IMG_a4")
  }

  @Test
  fun testVerifyRebuiltIrModelGroups() {
    val testIrModelGroupList = getTestIeModelGroups()
    val verifiedIrModelGroups = verifyRebuiltIrModelGroups(testIrModelGroupList)
    Truth.assertThat(verifiedIrModelGroups).hasSize(testIrModelGroupList.size)

    Truth.assertThat(verifiedIrModelGroups[0].models[0].modelGroups[1].models[0].modelGroups[0].models[0].modelName).isEqualTo("1_A1_1_A_11")
    Truth.assertThat(verifiedIrModelGroups[0].models[0].modelGroups[1].models[0].modelGroups[0].models[0].id).isEqualTo("1_A2_1_A_1")
    Truth.assertThat(testIrModelGroupList[0].models[0].modelGroups[1].models[0].modelGroups[0].models[0].modelName).isEqualTo("1_A1_1_A_1")
    Truth.assertThat(testIrModelGroupList[0].models[0].modelGroups[1].models[0].modelGroups[0].models[0].id).isEqualTo("1_A2_1_A_1")

    Truth.assertThat(verifiedIrModelGroups[1].models[0].modelGroups[0].models[0].modelName).isEqualTo("1_A1_11")
    Truth.assertThat(verifiedIrModelGroups[1].models[0].modelGroups[0].models[0].id).isEqualTo("2_B1_1")
    Truth.assertThat(testIrModelGroupList[1].models[0].modelGroups[0].models[0].modelName).isEqualTo("1_A1_1")
    Truth.assertThat(testIrModelGroupList[1].models[0].modelGroups[0].models[0].id).isEqualTo("2_B1_1")

    Truth.assertThat(verifiedIrModelGroups[3].models[0].modelName).isEqualTo("1_A1_1")
    Truth.assertThat(verifiedIrModelGroups[3].models[0].id).isEqualTo("C")
    Truth.assertThat(testIrModelGroupList[3].models[0].modelName).isEqualTo("1_A1_1")
    Truth.assertThat(testIrModelGroupList[3].models[0].id).isEqualTo("C")
  }

  private fun getTestIeModelGroups() = listOf(
    IrModelGroup(
      baseModelId = "1",
      models = listOf(
        createTestIrModel(
          modelName = "A",
          id = "A"
        ) {
          listOf(
            IrModelGroup(
              baseModelId = "1_A1",
              models = listOf(
                createTestIrModel(
                  modelName = "1_A1_1",
                  id = "1_A1_1"
                ) {
                  listOf(
                    IrModelGroup(
                      baseModelId = "1_A1_1_A",
                      models = listOf(
                        createTestIrModel(
                          modelName = "1_A1_1_A_1",
                          id = "1_A1_1_A_1"
                        ) { listOf() }
                      )
                    )
                  )
                }
              )
            ),
            IrModelGroup(
              baseModelId = "1_A2",
              models = listOf(
                createTestIrModel(
                  modelName = "1_A2_1",
                  id = "1_A2_1"
                ) {
                  listOf(
                    IrModelGroup(
                      baseModelId = "1_A2_1_A",
                      models = listOf(
                        createTestIrModel(
                          modelName = "1_A1_1_A_1", // Test match
                          id = "1_A2_1_A_1"
                        ) { listOf() }
                      )
                    )
                  )
                }
              )
            ),
          )
        }
      )
    ),
    IrModelGroup(
      baseModelId = "2",
      models = listOf(
        createTestIrModel(
          modelName = "B",
          id = "B"
        ) {
          listOf(
            IrModelGroup(
              baseModelId = "2_B1",
              models = listOf(
                createTestIrModel(
                  modelName = "1_A1_1", // Test match
                  id = "2_B1_1",
                ) {
                  listOf(
                    IrModelGroup(
                      baseModelId = "2_B1_1_A",
                      models = listOf(
                        createTestIrModel(
                          modelName = "2_B1_1_A_1",
                          id = "2_B1_1_A_1"
                        ) { listOf() }
                      )
                    )
                  )
                }
              )
            )
          )
        }
      )
    ),
    IrModelGroup(
      baseModelId = "3",
      models = listOf()
    ),
    IrModelGroup(
      baseModelId = "4",
      models = listOf(
        createTestIrModel(
          modelName = "1_A1_1", // Different depth, same name
          id = "C"
        ){
          listOf(
            IrModelGroup(
              baseModelId = "4_C1",
              models = listOf(
                createTestIrModel(
                  modelName = "4_C1_1",
                  id = "4_C1_1",
                ) {
                  listOf()
                }
              )
            )
          )
        },
        createTestIrModel(
          modelName = "D",
          id = "D"
        ){
          listOf(
            IrModelGroup(
              baseModelId = "2_D1",
              models = listOf(
                createTestIrModel(
                  modelName = "4_D1_1",
                  id = "4_D1_1",
                ) {
                  listOf()
                }
              )
            )
          )
        }
      )
    ),
  )


  private fun createTestIrModelGroup(): IrModelGroup {
    return IrModelGroup(
      baseModelId = "operation.IMG_a.Data",
      models = listOf(
        createTestIrModel("IM_b", "IM_b") {
          listOf(
            IrModelGroup(
              baseModelId = "IMG_3",
              models = listOf(
                createTestIrModel("IM_c", "IM_c") {
                  listOf(
                    IrModelGroup(
                      baseModelId = "IMG_ra",
                      models = listOf(
                        createTestIrModel("IM_d", "IM_d") {
                          listOf(
                            IrModelGroup(
                              baseModelId = "IMG_rr",
                              models = listOf(
                                createTestIrModel("IM_e", "IM_e") {
                                  listOf(
                                    IrModelGroup(
                                      baseModelId = "IMG_n",
                                      models = listOf(
                                        createTestIrModel("IM_f", "IM_f") {
                                          listOf(
                                            IrModelGroup(
                                              baseModelId = "IMG_bb",
                                              models = listOf(
                                                createTestIrModel("IM_j", "IM_j") {
                                                  listOf()
                                                },
                                              )
                                            ),
                                          )
                                        },
                                      )
                                    ),
                                  )
                                },
                              )
                            ),
                          )
                        },
                        createTestIrModel("IM_h", "IM_h") {
                          listOf(
                            IrModelGroup(
                              baseModelId = "IMG_nn",
                              models = listOf(
                                createTestIrModel("IM_i", "IM_i") {
                                  listOf(
                                    IrModelGroup(
                                      baseModelId = "IMG_a3",
                                      models = listOf(
                                        createTestIrModel("IM_k", "IM_k") {
                                          listOf(
                                            IrModelGroup(
                                              baseModelId = "IMG_a4",
                                              models = listOf(
                                                createTestIrModel("IM_l", "IM_l") {
                                                  listOf()
                                                },
                                              )
                                            )
                                          )
                                        },
                                      )
                                    ),
                                  )
                                },
                              )
                            ),
                          )
                        },
                      )
                    )
                  )
                },
                createTestIrModel("IM_23", "IM_23") {
                  emptyList()
                },
              )
            ),
            IrModelGroup(
              baseModelId = "IMG_cc",
              models = listOf()
            )
          )
        },
        createTestIrModel("IM_a7", "IM_a7") {
          emptyList()
        },
        createTestIrModel("IM_a10", "IM_a10") {
          emptyList()
        },
      )
    )
  }

  private fun createTestIrModel(modelName: String, id: String, modelGroups: () -> List<IrModelGroup>): IrModel {
    return IrModel(
      modelName = modelName,
      id = id,
      modelGroups = modelGroups.invoke(),
      accessors = emptyList(),
      typeSet = emptySet(),
      possibleTypes = emptyList(),
      isInterface = false,
      properties = emptyList(),
      implements = emptyList(),
      isFallback = false,
    )
  }

  /**
   * Important to call out in our test data: the tree mapping the data to be extracted will always terminate in a node where
   * `hasExtracted` is true. This is because we are only ever inputting the case where the data is extracted.
   */
  private fun createTestNode(): ExplicitlyRemovedNode {
    return ExplicitlyRemovedNode(
      name = "IMG_a",
      hasExtracted = false,
      renameTo = "",
      children = mutableMapOf(
        "IM_b" to ExplicitlyRemovedNode(
          name = "IM_b",
          hasExtracted = false,
          renameTo = "",
          children = mutableMapOf(
            "IM_c" to ExplicitlyRemovedNode(
              name = "IM_c",
              hasExtracted = true,
              renameTo = "",
              children = mutableMapOf(
                "IM_d" to ExplicitlyRemovedNode(
                  name = "IM_d",
                  hasExtracted = false,
                  renameTo = "",
                  children = mutableMapOf(
                    "IM_e" to ExplicitlyRemovedNode(
                      name = "IM_e",
                      hasExtracted = true,
                      renameTo = "",
                      children = mutableMapOf(
                        "IM_f" to ExplicitlyRemovedNode(
                          name = "IM_f",
                          hasExtracted = false,
                          renameTo = "",
                          children = mutableMapOf(
                            "IM_j" to ExplicitlyRemovedNode(
                              name = "IM_j",
                              hasExtracted = true,
                              renameTo = "",
                              children = mutableMapOf()
                            )
                          )
                        )
                      )
                    )
                  )
                ),
                "IM_h" to ExplicitlyRemovedNode(
                  name = "IM_h",
                  hasExtracted = false,
                  renameTo = "",
                  children = mutableMapOf(
                    "IM_i" to ExplicitlyRemovedNode(
                      name = "IM_i",
                      hasExtracted = true,
                      renameTo = "",
                      children = mutableMapOf(
                        "IM_k" to ExplicitlyRemovedNode(
                          name = "IM_k",
                          hasExtracted = false,
                          renameTo = "",
                          children = mutableMapOf(
                            "IM_l" to ExplicitlyRemovedNode(
                              name = "IM_l",
                              hasExtracted = true,
                              renameTo = "",
                              children = mutableMapOf()
                            )
                          )
                        )
                      )
                    )
                  ),
                )
              ),
            )
          )
        )
      )
    )
  }
}