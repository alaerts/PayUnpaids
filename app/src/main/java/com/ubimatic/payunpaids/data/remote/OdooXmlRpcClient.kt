package com.ubimatic.payunpaids.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "OdooXmlRpc"

@Singleton
class OdooXmlRpcClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val xmlMediaType = "text/xml; charset=utf-8".toMediaType()

    suspend fun version(url: String): String? {
        val body = buildXmlRpcCall("version", emptyList())
        val response = call("$url/xmlrpc/2/common", body)
        return Regex("<name>server_version</name>\\s*<value>\\s*<string>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .find(response)?.groupValues?.get(1)
    }

    suspend fun listDatabases(url: String): List<String> {
        val body = buildXmlRpcCall("list", emptyList())
        return try {
            val response = call("$url/xmlrpc/2/db", body)
            Log.d(TAG, "DB list response: $response")
            val dbs = mutableListOf<String>()
            Regex("<value><string>(.*?)</string></value>").findAll(response).forEach {
                dbs.add(it.groupValues[1])
            }
            dbs
        } catch (e: Exception) {
            Log.w(TAG, "Could not list databases: ${e.message}")
            emptyList()
        }
    }

    suspend fun authenticate(url: String, db: String, username: String, apiKey: String): Int? {
        // Try JSON-RPC first (works on Odoo 19 SaaS), fall back to XML-RPC
        return authenticateJsonRpc(url, db, username, apiKey)
            ?: authenticateXmlRpc(url, db, username, apiKey)
    }

    private suspend fun authenticateJsonRpc(url: String, db: String, username: String, apiKey: String): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                    {"jsonrpc":"2.0","method":"call","id":1,"params":{
                        "db":"$db",
                        "login":"$username",
                        "password":"${apiKey.replace("\"", "\\\"")}"
                    }}
                """.trimIndent()
                Log.d(TAG, ">>> POST $url/web/session/authenticate (JSON-RPC)")
                Log.d(TAG, ">>> Body: {db=$db, login=$username, password=***REDACTED***}")
                val request = Request.Builder()
                    .url("$url/web/session/authenticate")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .header("Content-Type", "application/json")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext null
                Log.d(TAG, "<<< JSON-RPC response: $responseBody")

                // Parse uid from JSON response
                val uidMatch = Regex("\"uid\"\\s*:\\s*(\\d+)").find(responseBody)
                val uid = uidMatch?.groupValues?.get(1)?.toIntOrNull()

                // Check for error
                if (responseBody.contains("\"uid\":false") || responseBody.contains("\"uid\": false")) {
                    Log.w(TAG, "JSON-RPC auth returned uid=false")
                    return@withContext null
                }

                uid
            } catch (e: Exception) {
                Log.w(TAG, "JSON-RPC auth failed, will try XML-RPC: ${e.message}")
                null
            }
        }
    }

    private suspend fun authenticateXmlRpc(url: String, db: String, username: String, apiKey: String): Int? {
        val body = buildXmlRpcCall("authenticate", listOf(
            xmlString(db),
            xmlString(username),
            xmlString(apiKey),
            xmlStruct(emptyMap()),
        ))
        val response = call("$url/xmlrpc/2/common", body)
        return parseIntValue(response)
    }

    suspend fun searchRead(
        url: String,
        db: String,
        uid: Int,
        apiKey: String,
        model: String,
        domain: List<Any>,
        fields: List<String>,
        order: String? = null,
    ): List<Map<String, Any?>> {
        val kwargs = mutableMapOf<String, Any>(
            "fields" to fields,
        )
        if (order != null) {
            kwargs["order"] = order
        }

        val body = buildXmlRpcCall("execute_kw", listOf(
            xmlString(db),
            xmlInt(uid),
            xmlString(apiKey),
            xmlString(model),
            xmlString("search_read"),
            xmlArray(listOf(xmlDomain(domain))),
            xmlKwargs(kwargs),
        ))
        val response = call("$url/xmlrpc/2/object", body)
        return parseArrayOfStructs(response)
    }

    suspend fun write(
        url: String,
        db: String,
        uid: Int,
        apiKey: String,
        model: String,
        ids: List<Int>,
        values: Map<String, Any>,
    ): Boolean {
        val body = buildXmlRpcCall("execute_kw", listOf(
            xmlString(db),
            xmlInt(uid),
            xmlString(apiKey),
            xmlString(model),
            xmlString("write"),
            xmlArray(listOf(
                xmlIntArray(ids),
                xmlStruct(values.mapValues { (_, v) ->
                    when (v) {
                        is String -> xmlString(v)
                        is Int -> xmlInt(v)
                        is Boolean -> xmlBoolean(v)
                        is Double -> xmlDouble(v)
                        else -> xmlString(v.toString())
                    }
                }),
            )),
        ))
        val response = call("$url/xmlrpc/2/object", body)
        return parseBooleanValue(response)
    }

    suspend fun read(
        url: String,
        db: String,
        uid: Int,
        apiKey: String,
        model: String,
        ids: List<Int>,
        fields: List<String>,
    ): List<Map<String, Any?>> {
        val body = buildXmlRpcCall("execute_kw", listOf(
            xmlString(db),
            xmlInt(uid),
            xmlString(apiKey),
            xmlString(model),
            xmlString("read"),
            xmlArray(listOf(xmlIntArray(ids))),
            xmlKwargs(mapOf("fields" to fields)),
        ))
        val response = call("$url/xmlrpc/2/object", body)
        return parseArrayOfStructs(response)
    }

    suspend fun create(
        url: String,
        db: String,
        uid: Int,
        apiKey: String,
        model: String,
        values: Map<String, Any>,
        context: Map<String, Any> = emptyMap(),
    ): Int? {
        val kwargs = if (context.isNotEmpty()) {
            mapOf("context" to context)
        } else emptyMap()

        val params = mutableListOf(
            xmlString(db),
            xmlInt(uid),
            xmlString(apiKey),
            xmlString(model),
            xmlString("create"),
            xmlArray(listOf(
                xmlStruct(values.mapValues { (_, v) -> xmlTypedValue(v) }),
            )),
        )
        if (kwargs.isNotEmpty()) {
            params.add(xmlKwargs(kwargs))
        }

        val body = buildXmlRpcCall("execute_kw", params)
        val response = call("$url/xmlrpc/2/object", body)
        return parseIntValue(response)
    }

    suspend fun callMethod(
        url: String,
        db: String,
        uid: Int,
        apiKey: String,
        model: String,
        method: String,
        ids: List<Int>,
        context: Map<String, Any> = emptyMap(),
    ): String {
        val kwargs = if (context.isNotEmpty()) {
            mapOf("context" to context)
        } else emptyMap()

        val params = mutableListOf(
            xmlString(db),
            xmlInt(uid),
            xmlString(apiKey),
            xmlString(model),
            xmlString(method),
            xmlArray(listOf(xmlIntArray(ids))),
        )
        if (kwargs.isNotEmpty()) {
            params.add(xmlKwargs(kwargs))
        }

        val body = buildXmlRpcCall("execute_kw", params)
        return call("$url/xmlrpc/2/object", body)
    }

    private fun xmlTypedValue(v: Any): String = when (v) {
        is String -> xmlString(v)
        is Int -> xmlInt(v)
        is Boolean -> xmlBoolean(v)
        is Double -> xmlDouble(v)
        is List<*> -> xmlArray(v.map { xmlTypedValue(it ?: "") })
        else -> xmlString(v.toString())
    }

    private suspend fun call(endpoint: String, xmlBody: String): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, ">>> POST $endpoint")
            // Redact 3rd param (password/API key) from authenticate calls
            val redacted = if (xmlBody.contains("authenticate")) {
                var paramCount = 0
                xmlBody.replace(Regex("<param><value><string>(.*?)</string></value></param>")) { match ->
                    paramCount++
                    if (paramCount == 3) "<param><value><string>***REDACTED***</string></value></param>"
                    else match.value
                }
            } else xmlBody
            Log.d(TAG, ">>> Body:\n$redacted")
            val request = Request.Builder()
                .url(endpoint)
                .post(xmlBody.toRequestBody(xmlMediaType))
                .build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw OdooException("Empty response from $endpoint")
            Log.d(TAG, "<<< ${response.code} Response:\n$responseBody")
            if (!response.isSuccessful) {
                throw OdooException("HTTP ${response.code}: $responseBody")
            }
            checkForFault(responseBody)
            responseBody
        }
    }

    private fun checkForFault(xml: String) {
        if (xml.contains("<fault>")) {
            val faultString = Regex(
                "<name>faultString</name>\\s*<value>\\s*<string>(.*?)</string>",
                RegexOption.DOT_MATCHES_ALL,
            ).find(xml)?.groupValues?.get(1) ?: "Unknown XML-RPC fault"
            Log.e(TAG, "XML-RPC fault: $faultString")
            throw OdooException(faultString)
        }
    }

    // XML-RPC building helpers

    private fun buildXmlRpcCall(method: String, params: List<String>): String {
        val paramsXml = params.joinToString("\n") { "<param><value>$it</value></param>" }
        return """<?xml version="1.0"?>
<methodCall>
<methodName>$method</methodName>
<params>
$paramsXml
</params>
</methodCall>"""
    }

    private fun xmlString(value: String): String = "<string>${escapeXml(value)}</string>"
    private fun xmlInt(value: Int): String = "<int>$value</int>"
    private fun xmlDouble(value: Double): String = "<double>$value</double>"
    private fun xmlBoolean(value: Boolean): String = "<boolean>${if (value) 1 else 0}</boolean>"

    private fun xmlStruct(map: Map<String, String>): String {
        if (map.isEmpty()) return "<struct/>"
        val members = map.entries.joinToString("\n") { (k, v) ->
            "<member><name>${escapeXml(k)}</name><value>$v</value></member>"
        }
        return "<struct>$members</struct>"
    }

    private fun xmlArray(items: List<String>): String {
        val data = items.joinToString("") { "<value>$it</value>" }
        return "<array><data>$data</data></array>"
    }

    private fun xmlIntArray(ids: List<Int>): String {
        val data = ids.joinToString("") { "<value><int>$it</int></value>" }
        return "<array><data>$data</data></array>"
    }

    private fun xmlDomain(domain: List<Any>): String {
        val items = domain.map { item ->
            when (item) {
                is List<*> -> {
                    val triple = item as List<Any>
                    val field = xmlString(triple[0] as String)
                    val op = xmlString(triple[1] as String)
                    val value = when (val v = triple[2]) {
                        is String -> xmlString(v)
                        is Int -> xmlInt(v)
                        is Boolean -> xmlBoolean(v)
                        is List<*> -> {
                            val arrItems = v.map { sv ->
                                when (sv) {
                                    is String -> xmlString(sv)
                                    is Int -> xmlInt(sv)
                                    else -> xmlString(sv.toString())
                                }
                            }
                            xmlArray(arrItems)
                        }
                        else -> xmlString(v.toString())
                    }
                    "<value><array><data><value>$field</value><value>$op</value><value>$value</value></data></array></value>"
                }
                is String -> "<value>${xmlString(item)}</value>"
                else -> "<value>${xmlString(item.toString())}</value>"
            }
        }
        return "<array><data>${items.joinToString("")}</data></array>"
    }

    private fun xmlKwargs(kwargs: Map<String, Any>): String {
        val members = kwargs.entries.joinToString("\n") { (k, v) ->
            val valueXml = when (v) {
                is String -> xmlString(v)
                is Int -> xmlInt(v)
                is List<*> -> {
                    val items = v.map { item ->
                        when (item) {
                            is String -> xmlString(item)
                            is Int -> xmlInt(item)
                            else -> xmlString(item.toString())
                        }
                    }
                    xmlArray(items)
                }
                else -> xmlString(v.toString())
            }
            "<member><name>${escapeXml(k)}</name><value>$valueXml</value></member>"
        }
        return "<struct>$members</struct>"
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // XML-RPC DOM-based parsing

    private val docBuilderFactory = DocumentBuilderFactory.newInstance()

    private fun parseXml(xml: String): Element {
        val builder = docBuilderFactory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        return doc.documentElement
    }

    private fun parseIntValue(xml: String): Int? {
        val root = parseXml(xml)
        val value = root.getElementsByTagName("value").item(0) ?: return null
        return parseDomValue(value as Element) as? Int
    }

    private fun parseBooleanValue(xml: String): Boolean {
        val root = parseXml(xml)
        val value = root.getElementsByTagName("value").item(0) ?: return false
        return parseDomValue(value as Element) == true
    }

    fun parseArrayOfStructs(xml: String): List<Map<String, Any?>> {
        val root = parseXml(xml)
        // Find the first <value> inside <param>
        val params = root.getElementsByTagName("param")
        if (params.length == 0) return emptyList()
        val paramEl = params.item(0) as Element
        val valueEl = firstChildElement(paramEl, "value") ?: return emptyList()
        val parsed = parseDomValue(valueEl)

        // Result should be a list of maps
        return when (parsed) {
            is List<*> -> parsed.filterIsInstance<Map<String, Any?>>()
            else -> emptyList()
        }
    }

    private fun parseDomValue(valueElement: Element): Any? {
        // A <value> element contains a single type child: <int>, <string>, <boolean>, <double>, <array>, <struct>, <base64>
        // Or plain text (treated as string)
        val child = firstChildElement(valueElement) ?: run {
            // Plain text content = string value
            val text = valueElement.textContent?.trim()
            return if (text.isNullOrEmpty()) null else text
        }

        return when (child.tagName) {
            "int", "i4" -> child.textContent?.trim()?.toIntOrNull()
            "double" -> child.textContent?.trim()?.toDoubleOrNull()
            "boolean" -> child.textContent?.trim() == "1"
            "string" -> child.textContent ?: ""
            "base64" -> child.textContent?.trim()
            "array" -> {
                val data = firstChildElement(child, "data") ?: return emptyList<Any?>()
                val items = mutableListOf<Any?>()
                var node = data.firstChild
                while (node != null) {
                    if (node is Element && node.tagName == "value") {
                        items.add(parseDomValue(node))
                    }
                    node = node.nextSibling
                }
                items
            }
            "struct" -> {
                val map = mutableMapOf<String, Any?>()
                var node = child.firstChild
                while (node != null) {
                    if (node is Element && node.tagName == "member") {
                        val nameEl = firstChildElement(node, "name")
                        val valEl = firstChildElement(node, "value")
                        if (nameEl != null && valEl != null) {
                            map[nameEl.textContent.trim()] = parseDomValue(valEl)
                        }
                    }
                    node = node.nextSibling
                }
                map
            }
            else -> child.textContent?.trim()
        }
    }

    private fun firstChildElement(parent: Element, tagName: String? = null): Element? {
        var node = parent.firstChild
        while (node != null) {
            if (node is Element && (tagName == null || node.tagName == tagName)) {
                return node
            }
            node = node.nextSibling
        }
        return null
    }
}

class OdooException(message: String) : Exception(message)
