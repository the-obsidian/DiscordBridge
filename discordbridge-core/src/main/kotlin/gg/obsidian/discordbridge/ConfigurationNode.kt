// Class borrowed from dynmap-core (https://github.com/webbukkit/DynmapCore/blob/master/src/main/java/org/dynmap/ConfigurationNode.java)

package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.config.UserAlias
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.*
import org.yaml.snakeyaml.reader.UnicodeReader
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
import java.io.*
import java.lang.NullPointerException
import java.util.*

class ConfigurationNode() : MutableMap<String, Any> {
    private var entries2: MutableMap<String, Any>? = null
    private var name: String = ""
    private var f: File? = null
    private var yaml: Yaml? = null

    init {
        entries2 = LinkedHashMap()
    }

    private fun initparse() {
        if(yaml == null) {
            val options = DumperOptions()

            options.indent = 4
            options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            options.isPrettyFlow = true

            val representer = EmptyNullRepresenter()
            representer.addClassTag(String::class.java, Tag.STR)
            representer.addClassTag(UserAlias::class.java, Tag.MAP)

            yaml = Yaml(SafeConstructor(), representer, options)
        }
    }

    constructor(name: String, f: File): this() {
        this.name = name
        this.f = f
    }

    constructor(name: String, map: MutableMap<String, Any>): this() {
        this.name = name
        entries2 = map
    }

    fun load(): Boolean {
        initparse()

        val file = f ?: return (entries2 != null)
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            entries2 = yaml!!.load(UnicodeReader(fis))
            fis.close()
        }
        catch (e: YAMLException) {
            DiscordBridge.logger.severe("Error parsing " + file.path + ". Use http://yamllint.com to debug the YAML syntax." )
            throw e
        } catch(iox: IOException) {
            DiscordBridge.logger.severe("Error reading " + file.path)
            return false
        } finally {
            if(fis != null) {
                try { fis.close(); } catch (x: IOException) {}
            }
        }
        return (entries2 != null)
    }

    fun save(): Boolean {
        val file = f ?: return false
        return save(file)
    }

    private fun save(file: File): Boolean {
        initparse()

        var stream: FileOutputStream? = null

        file.parentFile?.mkdirs()

        try {
            stream = FileOutputStream(file)
            val writer = OutputStreamWriter(stream, "UTF-8")
            yaml!!.dump(entries2, writer)
            writer.close()
            return true
        }
        catch (e: IOException) { }
        finally {
            try {
                stream?.close()
            }
            catch (e: IOException) { }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getObject(path: String, default: T): T {
        if (path.isEmpty()) {
            return try {
                entries2 as T ?: default
            } catch (e: ClassCastException) {
                default
            }
        }

        val separator = path.indexOf('.')
        if (separator < 0) {
            return try {
                get(path) as T ?: default
            } catch (e: ClassCastException) {
                default
            }
        }

        return try {
            val localKey = path.substring(0, separator)
            val submap = get(localKey) as MutableMap<String, Any>
            val subpath = path.substring(separator + 1)
            ConfigurationNode(path, submap).getObject(subpath, default) ?: default
        } catch (e: ClassCastException) {
            default
        } catch (e: NullPointerException) {
            default
        }
    }

    fun getInteger(path: String, default: Int): Int = getObject(path, default)
    fun getLong(path: String, default: Long): Long = getObject(path, default)
    fun getFloat(path: String, default: Float): Float = getObject(path, default)
    fun getDouble(path: String, default: Double): Double = getObject(path, default)
    fun getBoolean(path: String, default: Boolean): Boolean = getObject(path, default)
    fun getString(path: String, default: String): String = getObject(path, default)
    fun getStrings(path: String, default: List<String>): List<String> = getObject(path, default)
    fun <T> getList(path: String, default: List<T>): List<T> = getObject(path, default)

    override val size: Int get() = entries2!!.size
    override fun isEmpty(): Boolean = entries2!!.isEmpty()
    override fun containsKey(key: String): Boolean = entries2!!.containsKey(key)
    override fun containsValue(value: Any): Boolean = entries2!!.containsValue(value)
    override fun get(key: String): Any? = entries2!![key]
    override fun put(key: String, value: Any): Any? = entries2!!.put(key, value)
    override fun remove(key: String): Any? = entries2!!.remove(key)
    override fun putAll(from: Map<out String, Any>) = entries2!!.putAll(from)
    override fun clear() = entries2!!.clear()
    override val keys: MutableSet<String> get() = entries2!!.keys
    override val values: MutableCollection<Any> get() = entries2!!.values
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>> get() = entries2!!.entries

    private class EmptyNullRepresenter : Representer() {
        init {
            this.nullRepresenter = EmptyRepresentNull()
        }

        private inner class EmptyRepresentNull : Represent {
            override fun representData(data: Any): Node {
                return representScalar(Tag.NULL, "") // Changed "null" to "" so as to avoid writing nulls
            }
        }

        // Code borrowed from snakeyaml (http://code.google.com/p/snakeyaml/source/browse/src/test/java/org/yaml/snakeyaml/issues/issue60/SkipBeanTest.java)
        override fun representJavaBeanProperty(javaBean: Any, property: Property, propertyValue: Any, customTag: Tag): NodeTuple? {
            val tuple = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
            val valueNode = tuple.valueNode
            if (valueNode is CollectionNode<*>) {
                // Removed null check
                if (Tag.SEQ == valueNode.getTag()) {
                    val seq = valueNode as SequenceNode
                    if (seq.value.isEmpty()) {
                        return null // skip empty lists
                    }
                }
                if (Tag.MAP == valueNode.getTag()) {
                    val seq = valueNode as MappingNode
                    if (seq.value.isEmpty()) {
                        return null // skip empty maps
                    }
                }
            }
            return tuple
        }
        // End of borrowed code
    }
}
