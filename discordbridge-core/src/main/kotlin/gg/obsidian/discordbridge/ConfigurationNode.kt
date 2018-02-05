// Class borrowed from dynmap-core (https://github.com/webbukkit/DynmapCore/blob/master/src/main/java/org/dynmap/ConfigurationNode.java)

package gg.obsidian.discordbridge

import gg.obsidian.discordbridge.util.Rating
import gg.obsidian.discordbridge.util.Respect
import gg.obsidian.discordbridge.util.Script
import gg.obsidian.discordbridge.util.UserAlias
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.util.ArrayList
import java.util.LinkedHashMap

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.CollectionNode
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.reader.UnicodeReader
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer

class ConfigurationNode() : MutableMap<String, Any> {
    var entries2: MutableMap<String, Any>? = null
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
            //yaml = Yaml(CustomClassLoaderConstructor(DiscordBridge::class.java.classLoader), representer, options)
        }
    }

    constructor(f: File): this() {
        this.f = f
    }

    constructor(map: MutableMap<String, Any>): this() {
        if (map == null) {
            throw IllegalArgumentException()
        }
        entries2 = map
    }

    constructor(input: InputStream): this() {
        load(input)
    }

    @SuppressWarnings("unchecked")
    fun load(input: InputStream): Boolean {
        initparse()

        val o: Any = yaml!!.load(UnicodeReader(input))
        if(o is MutableMap<*, *>)
            entries2 = o as MutableMap<String, Any>
        return (entries2 != null)
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
            return true
        } catch (e: IOException) {
        } finally {
            try {
                if (stream != null) {
                    stream.close()
                }
            } catch (e: IOException) {
            }
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
        val ret = ConfigurationNode(submap).getObject(subpath)
        return ret
    }

    fun getObject(path: String, default: Any): Any {
        return getObject(path) ?: return default
    }

    @SuppressWarnings("unchecked")
    fun <T> getGeneric(path: String, default: T): T {
        val o = getObject(path, default as Any)
        return try {
            o as T
        } catch(e: ClassCastException) {
            default
        }
    }

    fun getInteger(path: String, default: Int): Int {
        return Integer.parseInt(getObject(path, default).toString())
    }

    fun getLong(path: String, default: Long): Double {
        return getObject(path, default).toString().toLong().toDouble()
    }

    fun getFloat(path: String, default: Float): Float {
        return getObject(path, default).toString().toFloat()
    }

    fun getDouble(path: String, default: Double): Double {
        return getObject(path, default).toString().toDouble()
    }

    fun getBoolean(path: String, default: Boolean): Boolean {
        return getObject(path, default).toString().toBoolean()
    }

    fun getString(path: String): String? {
        val o = getObject(path)
        return o.toString()
    }

    fun getStrings(path: String, default: List<String>): List<String> {
        val o = getObject(path) as? List<*> ?: return default
        return o.mapTo(ArrayList()) { it.toString() }
    }

    fun getString(path: String, default: String): String {
        val o = getObject(path, default)
        return o.toString()
    }

    @SuppressWarnings("unchecked")
    fun <T> getList(path: String): List<T> {
        try {
            val list = getObject(path) as List<T>
            return list
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

    fun getMapList(path: String): List<MutableMap<String, Any>> {
        return getList(path)
    }

    fun getNode(path: String): ConfigurationNode? {
        var v: MutableMap<String, Any>? = null
        v = getGeneric(path, v)
        if (v == null)
            return null
        return ConfigurationNode(v)
    }

    @SuppressWarnings("unchecked")
    fun getNodes(path: String): List<ConfigurationNode> {
        val o: List<Any> = getList(path)

        val nodes = ArrayList<ConfigurationNode>()
        for(i in o) {
            if (i is MutableMap<*, *>) {
                var map: MutableMap<String, Any>
                try {
                    map = i as MutableMap<String, Any>
                } catch(e: ClassCastException) {
                    continue
                }
                nodes.add(ConfigurationNode(map))
            }
        }
        return nodes
    }

    fun extend(other: MutableMap<String, Any>) {
        if (other != null)
            extendMap(this, other)
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

    fun <T> createInstance(constructorParameters: Array<Class<*>>, constructorArguments: Array<Any>): T? {
        val typeName = getString("class")
        try {
            val mapTypeClass = Class.forName(typeName)

            val constructorParameterWithConfiguration = arrayOfNulls<Class<*>>(constructorParameters.size+1)
            for(i in constructorParameters.indices) { constructorParameterWithConfiguration[i] = constructorParameters[i]; }
            constructorParameterWithConfiguration[constructorParameterWithConfiguration.size-1] = javaClass

            val constructorArgumentsWithConfiguration = arrayOfNulls<Any>(constructorArguments.size+1)
            for(i in constructorArguments.indices) { constructorArgumentsWithConfiguration[i] = constructorArguments[i]; }
            constructorArgumentsWithConfiguration[constructorArgumentsWithConfiguration.size-1] = this
            val constructor = mapTypeClass.getConstructor(*constructorParameterWithConfiguration)
            @SuppressWarnings("unchecked")
            val t = constructor.newInstance(constructorArgumentsWithConfiguration) as T
            return t
        } catch (e: Exception) {
            // TODO: Remove reference to MapManager.
            //Log.severe("Error loading maptype", e)
            e.printStackTrace()
        }
        return null
    }

    fun <T> createInstances(path: String, constructorParameters: Array<Class<*>>, constructorArguments: Array<Any>): List<T> {
        val nodes = getNodes(path)
        val instances = ArrayList<T>()
        for(node in nodes) {
            val instance = node.createInstance<T>(constructorParameters, constructorArguments)
            if (instance != null) instances.add(instance)
        }
        return instances
    }

    override val size: Int get() {
        return entries2!!.size
    }

    override fun isEmpty(): Boolean {
        return entries2!!.isEmpty()
    }

    override fun containsKey(key: String): Boolean {
        return entries2!!.containsKey(key)
    }

    override fun containsValue(value: Any): Boolean {
        return entries2!!.containsValue(value)
    }

    override fun get(key: String): Any? {
        return entries2!![key]
    }

    override fun put(key: String, value: Any): Any? {
        return entries2!!.put(key, value)
    }

    override fun remove(key: String): Any? {
        return entries2!!.remove(key)
    }

    override fun putAll(from: Map<out String, Any>) {
        entries2!!.putAll(from)
    }

    override fun clear() {
        entries2!!.clear()
    }

    override val keys: MutableSet<String> get() {
        return entries2!!.keys
    }

    override val values: MutableCollection<Any> get() {
        return entries2!!.values
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>> get() {
        return entries2!!.entries
    }

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