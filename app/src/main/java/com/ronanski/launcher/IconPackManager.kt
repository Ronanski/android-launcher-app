package com.ronanski.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Reads standard ADW/Nova-format icon packs (appfilter.xml). */
class IconPackManager(private val context: Context) {

    data class IconPack(val packageName: String, val label: String)

    private var loadedPack: String? = null
    private var packResources: Resources? = null
    private var mapping: Map<String, String> = emptyMap()

    fun installedPacks(): List<IconPack> {
        val pm = context.packageManager
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme",
        )
        return actions
            .flatMap { pm.queryIntentActivities(Intent(it), PackageManager.GET_META_DATA) }
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { pkg ->
                try {
                    IconPack(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    @Synchronized
    fun getIcon(pack: String, component: ComponentName): Drawable? {
        if (pack.isEmpty()) return null
        if (pack != loadedPack) load(pack)
        val res = packResources ?: return null
        val name = mapping[component.flattenToString()] ?: return null
        val id = res.getIdentifier(name, "drawable", pack)
        if (id == 0) return null
        return try {
            res.getDrawable(id, null)
        } catch (e: Exception) {
            null
        }
    }

    private fun load(pack: String) {
        loadedPack = pack
        packResources = null
        mapping = emptyMap()
        try {
            val res = context.packageManager.getResourcesForApplication(pack)
            val xmlId = res.getIdentifier("appfilter", "xml", pack)
            val parser: XmlPullParser? = if (xmlId != 0) {
                res.getXml(xmlId)
            } else {
                try {
                    XmlPullParserFactory.newInstance().newPullParser().apply {
                        setInput(res.assets.open("appfilter.xml"), "UTF-8")
                    }
                } catch (e: Exception) {
                    null
                }
            }
            val map = HashMap<String, String>()
            if (parser != null) {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "item") {
                        val comp = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (comp != null && drawable != null &&
                            comp.startsWith("ComponentInfo{") && comp.endsWith("}")
                        ) {
                            val inner = comp.substring("ComponentInfo{".length, comp.length - 1)
                            ComponentName.unflattenFromString(inner)?.let {
                                map[it.flattenToString()] = drawable
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            mapping = map
            packResources = res
        } catch (e: Exception) {
            // Pack missing or broken: fall back to normal icons.
        }
    }
}
