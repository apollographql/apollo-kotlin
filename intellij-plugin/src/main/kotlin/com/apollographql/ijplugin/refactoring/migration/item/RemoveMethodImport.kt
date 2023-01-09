package com.apollographql.ijplugin.refactoring.migration.item

class RemoveMethodImport(
    containingDeclarationName: String,
    methodName: String,
) : RemoveMethodCall(
    containingDeclarationName,
    methodName,
    removeImportsOnly = true,
)
