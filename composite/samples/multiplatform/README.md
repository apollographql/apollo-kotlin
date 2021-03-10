# Multiplatform Development Notes

Here are some things the community has run into which you should be aware of if you are planning to use KMP.

## Things To Be Aware Of With Objective-C interop

Kotlin/Native compiles down to Objective-C headers and uses the Objective-C runtime on iOS. This means there are a couple extra things to watch out for: 

- **All method calls are dynamically dispatched**. Objective-C looks up implementations at runtime rather than at compile time, but this can lead to code that looks perfectly fine working *really* weirdly, especially if you use a method or property name that's the same as something Apple has used in Objective-C. This also means that something that works perfectly fine on Android **is not guaranteed** to do so on iOS, so you need to make sure you're testing on both platforms.
- **All Objective-C types are classes inheriting from `NSObject`** are based on `NSObject`. This means a few things: 
    - Related to dynamic dispatch, `NSObject` has a default `description` property which returns a debug description of the object. If your GraphQL object has a `description` field (which is then turned into a Kotlin property), it generally won't get properly set on iOS, even though it'll work totally fine on Android. The recommended workaround is to typealias at the GraphQL level, as seen in the KMP sample app's use of `repoDescription` rather than `description`. 
    - SwiftUI relies really heavily on the behavior of `structs` in Swift, which won't work with KMP since everything is a `class` inheriting from `NSObject`. If you're planning to use SwiftUI, look into [`ObservableObject` wrappers](https://www.hackingwithswift.com/books/ios-swiftui/sharing-swiftui-state-with-observedobject) to handle telling your UI when changes have occurred. 