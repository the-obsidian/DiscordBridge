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
import java.util.*

class ConfigurationNode() : MutableMap<String, Any> {
    private var entries2: MutableMap<String, Any>? = null
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

    constructor(f: File): this() {
        this.f = f
    }

    constructor(map: MutableMap<String, Any>): this() {
        entries2 = map
    }

    @SuppressWarnings("unchecked")
    fun load(): Boolean {
        initparse()

        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(f)
            val o: Any? = yaml!!.load(UnicodeReader(fis))
            if((o != null) && (o is MutableMap<*, *>))
                entries2 = o as MutableMap<String, Any>
            fis.close()
        }
        catch (e: YAMLException) {
            //Log.severe("Error parsing " + f.path + ". Use http://yamllint.com to debug the YAML syntax." )
            throw e
        } catch(iox: IOException) {
            //Log.severe("Error reading " + f.path)
            return false
        } finally {
            if(fis != null) {
                try { fis.close(); } catch (x: IOException) {}
            }
        }
        return (entries2 != null)
    }

    fun save(): Boolean {
        return save(f)
    }

    fun save(file: File?): Boolean {
        initparse()

        var stream: FileOutputStream? = null

        file?.parentFile?.mkdirs()

        try {
            stream = FileOutputStream(file)
            val writer = OutputStreamWriter(stream, "UTF-8")
            yaml!!.dump(entries2, writer)
            writer.close()
            return true
        }
        catch (e: IOException) { }
        finally {
            try { if (stream != null) stream.close() }
            catch (e: IOException) { }
        }
        return false
    }

    @SuppressWarnings("unchecked")
    fun getObject(path: String): Any? {
        if (path.isEmpty())
            return entries2
        val separator = path.indexOf('.')
        if (separator < 0)
            return get(path)
        val localKey = path.substring(0, separator)
        val subvalue = (get(localKey) ?: return null) as? MutableMap<*, *> ?: return null
        val submap: MutableMap<String, Any>
        try {
            submap = subvalue as MutableMap<String, Any>
        } catch (e: ClassCastException) {
            return null
        }

        val subpath = path.substring(separator + 1)
        return ConfigurationNode(submap).getObject(subpath)
    }

    fun getObject(path: String, default: Any): Any {
        return getObject(path) ?: return default
    }

    fun getInteger(path: String, default: Int): Int = Integer.parseInt(getObject(path, default).toString())
    fun getLong(path: String, default: Long): Double = getObject(path, default).toString().toLong().toDouble()
    fun getFloat(path: String, default: Float): Float = getObject(path, default).toString().toFloat()
    fun getDouble(path: String, default: Double): Double = getObject(path, default).toString().toDouble()
    fun getBoolean(path: String, default: Boolean): Boolean = getObject(path, default).toString().toBoolean()
    fun getString(path: String): String? = getObject(path).toString()

    fun getStrings(path: String, default: List<String>): List<String> {
        val o = getObject(path) as? List<*> ?: return default
        return o.mapTo(ArrayList()) { it.toString() }
    }

    fun getString(path: String, default: String): String = getObject(path, default).toString()

    @SuppressWarnings("unchecked")
    fun <T> getList(path: String): List<T> {
        try {
            return getObject(path) as List<T>
        } catch (e: ClassCastException) {
            try {
                val o = getObject(path) as T ?: return ArrayList()
                val al = ArrayList<T>()
                al.add(o)
                return al
            } catch (e2: ClassCastException) {
                return ArrayList()
            }
        }
    }

    companion object {
        private fun copyValue(v: Any): Any {
            when (v) {
                is MutableMap<*, *> -> {
                    //@SuppressWarnings("unchecked")
                    val mv = v as MutableMap<String, Any>
                    val newv = LinkedHashMap<String,Any>()
                    for(me in mv.entries) {
                        newv.put(me.key, copyValue(me.value))
                    }
                    return newv
                }
                is List<*> -> {
                    @SuppressWarnings("unchecked")
                    val lv = v as List<Any>
                    return lv.indices.mapTo(ArrayList()) { copyValue(lv[it]) }
                }
                else -> return v
            }
        }

        private fun extendMap(left: MutableMap<String, Any>, right: MutableMap<String, Any>) {
            val original = ConfigurationNode(left)
            for(entry in right.entries) {
                val key = entry.key
                val value = entry.value
                original.put(key, copyValue(value))
            }
        }
    }

    override val size: Int get() = entries2!!.size

    override fun isEmpty(): Boolean = entries2!!.isEmpty()
    override fun containsKey(key: String): Boolean = entries2!!.containsKey(key)
    override fun containsValue(value: Any): Boolean = entries2!!.containsValue(value)
    override fun get(key: String): Any? = entries2!![key]
    override fun put(key: String, value: Any): Any? = entries2!!.put(key, value)
    override fun remove(key: String): Any? = entries2!!.remove(key)

    override fun putAll(from: Map<out String, Any>) {
        entries2!!.putAll(from)
    }

    override fun clear() {
        entries2!!.clear()
    }

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
