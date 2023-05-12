import jsexport.GetAnimalQuery

actual fun data(response: String): GetAnimalQuery.Data = animalImpl(response)