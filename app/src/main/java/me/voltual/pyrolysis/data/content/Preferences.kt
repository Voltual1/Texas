package me.voltual.pyrolysis.data.content

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
//import androidx.work.NetworkType
import me.voltual.pyrolysis.FILTER_CATEGORY_ALL
//import me.voltual.pyrolysis.NeoApp
import me.voltual.pyrolysis.PREFS_LANGUAGE
import me.voltual.pyrolysis.PREFS_LANGUAGE_DEFAULT
//import me.voltual.pyrolysis.R
//import me.voltual.pyrolysis.data.entity.AndroidVersion
//import me.voltual.pyrolysis.data.entity.InstallerType
//import me.voltual.pyrolysis.data.entity.Order
//import me.voltual.pyrolysis.utils.amInstalled
import me.voltual.pyrolysis.utils.extension.android.Android
//import me.voltual.pyrolysis.utils.getHasSystemInstallPermission
//import me.voltual.pyrolysis.utils.hasShizukuOrSui
//import me.voltual.pyrolysis.utils.isBiometricLockAvailable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.net.Proxy

data object Preferences : OnSharedPreferenceChangeListener {
    private lateinit var preferences: SharedPreferences
    private val subject = MutableSharedFlow<Key<*>>()

    private val keys = sequenceOf(
        Key.DisableSignatureCheck
    ).map { Pair(it.name, it) }.toMap()

    fun init(context: Context) {
        preferences =
            context.getSharedPreferences(
                "${context.packageName}_preferences",
                Context.MODE_PRIVATE
            )
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        CoroutineScope(Dispatchers.Default).launch {
            keys[key]?.let {
                subject.emit(it)
            }
        }
    }

    suspend fun addPreferencesChangeListener(listener: suspend (Key<*>) -> Unit) {
        subject.collect {
            listener(it)
        }
    }

    sealed class Value<T> {
        abstract val value: T

        internal abstract fun get(
            preferences: SharedPreferences,
            key: String,
            defaultValue: Value<T>,
        ): T

        internal abstract fun set(preferences: SharedPreferences, key: String, value: T)

        class BooleanValue(override val value: Boolean) : Value<Boolean>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<Boolean>,
            ): Boolean {
                return preferences.getBoolean(key, defaultValue.value)
            }

            override fun set(preferences: SharedPreferences, key: String, value: Boolean) {
                preferences.edit().putBoolean(key, value).apply()
            }
        }

        class IntValue(override val value: Int) : Value<Int>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<Int>,
            ): Int {
                return preferences.getInt(key, defaultValue.value)
            }

            override fun set(preferences: SharedPreferences, key: String, value: Int) {
                preferences.edit().putInt(key, value).apply()
            }
        }

        class LongValue(override val value: Long) : Value<Long>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<Long>,
            ): Long {
                return preferences.getLong(key, defaultValue.value)
            }

            override fun set(preferences: SharedPreferences, key: String, value: Long) {
                preferences.edit().putLong(key, value).apply()
            }
        }

        class StringSetValue(override val value: Set<String>) : Value<Set<String>>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<Set<String>>,
            ): Set<String> {
                return preferences.getStringSet(key, defaultValue.value) ?: emptySet()
            }

            override fun set(preferences: SharedPreferences, key: String, value: Set<String>) {
                preferences.edit().putStringSet(key, value).apply()
            }
        }

        class StringValue(override val value: String) : Value<String>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<String>,
            ): String {
                return preferences.getString(key, defaultValue.value) ?: defaultValue.value
            }

            override fun set(preferences: SharedPreferences, key: String, value: String) {
                preferences.edit().putString(key, value).apply()
            }
        }

        class EnumerationValue<T : Enumeration<T>>(override val value: T) : Value<T>() {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<T>,
            ): T {
                val value = preferences.getString(key, defaultValue.value.valueString)
                return defaultValue.value.values.find { it.valueString == value }
                    ?: defaultValue.value
            }

            override fun set(preferences: SharedPreferences, key: String, value: T) {
                preferences.edit().putString(key, value.valueString).apply()
            }
        }

        class EnumValue<T>(override val value: T, private val enumClass: Class<T>) : Value<T>()
                where T : Enum<T>, T : EnumEnumeration {
            override fun get(
                preferences: SharedPreferences,
                key: String,
                defaultValue: Value<T>,
            ): T {
                val value = preferences.getInt(key, defaultValue.value.ordinal)
                return enumFromOrdinal(enumClass, value)
            }

            override fun set(preferences: SharedPreferences, key: String, value: T) {
                preferences.edit().putInt(key, value.ordinal).apply()
            }

            private fun enumFromOrdinal(enumClass: Class<T>, ordinal: Int): T {
                return enumClass.enumConstants?.getOrNull(ordinal)
                    ?: enumClass.enumConstants?.first()
                    ?: throw NoSuchElementException("Enum ${enumClass.simpleName} is empty.")
            }
        }
    }

    interface Enumeration<T> {
        val values: List<T>
        val valueString: String
    }

    interface EnumEnumeration {
        val valueString: String
    }

    sealed class Key<T>(val name: String, val default: Value<T>) {

        data object DisableSignatureCheck :
            Key<Boolean>("disable_signature_check", Value.BooleanValue(false))

    }      

    operator fun <T> get(key: Key<T>): T {
        return key.default.get(preferences, key.name, key.default)
    }

    operator fun <T> set(key: Key<T>, value: T) {
        key.default.set(preferences, key.name, value)
    }
}
