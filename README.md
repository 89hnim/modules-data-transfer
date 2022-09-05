# modules-data-transfer
Across modules data transfer with sticky data, scope, lifecycle aware...

# Features
- Collect and emit data across modules in json format
- Collect and emit data to specific scope
- Collect emitted data with lifecycle-aware
- Support sticky data

# Why json format
I originally used ```Any``` instead of Json.<br>
Then I have a problem: what if feature A wants to transfer an internal object to feature B but feature B doesn't know about model of feature A?<br>
To avoid duplicate code or need to add a shared model between two modules, I decided to convert the emitted data to json.<br>
So feature B will receive model of feature A in json format, it can parse into a new model or do whatever it wants with the String.<br>

# How to use
## Emitting data
```kotlin
fun emit(
    data: Any,
    scope: OneDataTransferScope = OneDataTransferScope.Application,
    isSticky: Boolean = false
)
```
- ```data```: data to transfer
- ```scope```: scope of emitting data
- ```isSticky```: new collector should receive this data or not

Sample:
```kotlin
// emit a string within global scope
OneDataTransfer.get().emit("Value from feature B activity")
  
// emit an sticky object within FeatureB scope
OneDataTransfer.get()
    .emit(TestModel("Sticky event from main act"), OneDataTransferScope.Custom("FeatureB"), true)
```

## Collecting data
```kotlin
fun collect(
    collector: Collector,
    scope: OneDataTransferScope = OneDataTransferScope.Application,
    strategy: OneDataTransferStrategy = OneDataTransferStrategy.Always
)
```

- ```collector```: the collector (listener)
- ```scope```: collect emitted values in this scope only
- ```strategy```: how we should collect the emitted values
  - ```Always```: always collect the values
  - ```LifecycleAware```: respect lifecycle to collect values: start collecting when onStart(), stop collecting when onPause(), remove collector when onDestroy()

Sample:
```kotlin
OneDataTransfer.get().collect(object : OneDataTransfer.Collector() {
    override fun onDataChanged(data: String) {
        // got da data in json format!
    }
}, OneDataTransferScope.Custom("FeatureB"), OneDataTransferStrategy.LifecycleAware(this))
```

If you don't use Lifecycle-Aware Strategy, you should remove collector by manual:
```kotlin
OneDataTransfer.get().removeCollector(collector)
```
