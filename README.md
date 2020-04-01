YAKL is a Kotlin native YAML 1.2 processor.

It is a straight port of [SnakeYAML Engine](https://bitbucket.org/asomov/snakeyaml/src/master/)  

Currently this is very experimental. Limitations include:
* Only UTF-8 is supported
* Depends on a unreleased branch of kotlinx.io (https://github.com/Kotlin/kotlinx-io/tree/e5l/bytes) 
* Since Kotlin/native doesn't have any file handling yet, VERY basic code for reading files is included
* Recursive structures cause an infinite loop
* Only support macOS and Linux, although other platforms should be easy to add   

## Examples

### Deserialisation

```kotlin
val loadSettings = LoadSettings() // This can be used to configure how the yaml is deserialised.  
val load = Load(loadSettings)
val yaml = """
- item1
- item2
- item3
"""
val result = load.loadFromString(yaml) as List<String>
println(result)
```
Prints `[item1, item2, item3]`

### Serialisation

```kotlin
// Override the default flow style so the list is output with one element per line
val settings = DumpSettings(defaultFlowStyle = FlowStyle.BLOCK) 
val dump = Dump(settings)
val result = dump.dumpToString(listOf("item1", "item2", "item3"))
println(result)
```
Prints 

```yaml
- item1
- item2
- item3
```
