import jsexport.GetAnimalQuery

actual fun data(response: String): GetAnimalQuery.Data {
  return JSON.parse(response)
}