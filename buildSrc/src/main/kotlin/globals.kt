import com.google.common.collect.Ordering
import java.util.*

//region Semver classes
/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class VersionNumber(
    val major: Int,
    val minor: Int,
    val micro: Int,
    val patch: Int = 0,
    val qualifier: String? = null
) : Comparable<VersionNumber> {
    companion object {
        val UNKNOWN = version(0)
        private val DEFAULT_SCHEME = DefaultScheme()
        private val PATCH_SCHEME = SchemeWithPatchVersion()

        fun version(major: Int) = version(major, 0)
        fun version(major: Int, minor: Int) = VersionNumber(major, minor, 0)

        fun scheme(): Scheme = DEFAULT_SCHEME
        fun withPatchNumber(): Scheme = PATCH_SCHEME
        fun parse(versionString: String): VersionNumber = DEFAULT_SCHEME.parse(versionString)
    }

    private val scheme = if (patch == 0) DEFAULT_SCHEME else PATCH_SCHEME

    override fun compareTo(other: VersionNumber): Int {
        if (major != other.major) {
            return major - other.major
        }
        if (minor != other.minor) {
            return minor - other.minor
        }
        if (micro != other.micro) {
            return micro - other.micro
        }
        if (patch != other.patch) {
            return patch - other.patch
        }

        return Ordering.natural<String?>().nullsLast<String?>().compare(toLowerCase(qualifier), toLowerCase(other.qualifier))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionNumber

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (micro != other.micro) return false
        if (patch != other.patch) return false
        if (qualifier != other.qualifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + micro
        result = 31 * result + patch
        result = 31 * result + (qualifier?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return scheme.format(this)
    }

    fun getBaseVersion(): VersionNumber {
        return VersionNumber(major, minor, micro, patch, null)
    }

    private fun toLowerCase(string: String?): String? {
        return string?.lowercase(Locale.getDefault())
    }

    interface Scheme {
        fun parse(value: String): VersionNumber
        fun format(versionNumber: VersionNumber): String
    }

    private abstract class AbstractScheme : Scheme {
        abstract val depth: Int

        override fun parse(value: String): VersionNumber {
            if (value.isEmpty()) {
                return UNKNOWN
            }
            val scanner = Scanner(value)
            var minor = 0
            var micro = 0
            var patch = 0

            if (!scanner.hasDigit()) {
                return UNKNOWN
            }
            val major: Int = scanner.scanDigit()
            if (scanner.isSeparatorAndDigit('.')) {
                scanner.skipSeparator()
                minor = scanner.scanDigit()
                if (scanner.isSeparatorAndDigit('.')) {
                    scanner.skipSeparator()
                    micro = scanner.scanDigit()
                    if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
                        scanner.skipSeparator()
                        patch = scanner.scanDigit()
                    }
                }
            }

            if (scanner.isEnd()) {
                return VersionNumber(major, minor, micro, patch, null)
            }

            if (scanner.isQualifier()) {
                scanner.skipSeparator()
                return VersionNumber(major, minor, micro, patch, scanner.remainder())
            }

            return UNKNOWN
        }

        private class Scanner(versionString: String) {
            var pos = 0
            val str = versionString

            fun hasDigit(): Boolean {
                return pos < str.length && Character.isDigit(str[pos])
            }

            fun isSeparatorAndDigit(vararg separators: Char): Boolean {
                return pos < str.length - 1 && oneOf(*separators) && Character.isDigit(str[pos + 1])
            }

            fun isSeparatorAndDigit(separators: String): Boolean {
                return isSeparatorAndDigit(*separators.toCharArray())
            }

            private fun oneOf(vararg separators: Char): Boolean {
                val current = str[pos]
                separators.forEach {
                    if (current == it) {
                        return true
                    }
                }
                return false
            }

            fun isQualifier(): Boolean {
                return pos < str.length - 1 && oneOf('.', '-')
            }

            fun scanDigit(): Int {
                val start = pos
                while (hasDigit()) {
                    pos++
                }
                return str.substring(start, pos).toInt()
            }

            fun isEnd(): Boolean {
                return pos == str.length
            }

            fun skipSeparator() {
                pos++
            }

            fun remainder(): String? {
                return if (pos == str.length) null else str.substring(pos)
            }
        }
    }

    private class DefaultScheme : AbstractScheme() {
        override val depth = 3
        override fun format(versionNumber: VersionNumber): String {
            return String.format(
                "%d.%d.%d%s",
                versionNumber.major,
                versionNumber.minor,
                versionNumber.micro,
                if (versionNumber.qualifier != null) "-${versionNumber.qualifier}" else ""
            )
        }
    }

    private class SchemeWithPatchVersion : AbstractScheme() {
        override val depth = 4
        override fun format(versionNumber: VersionNumber): String {
            return String.format(
                "%d.%d.%d.%d%s",
                versionNumber.major,
                versionNumber.minor,
                versionNumber.micro,
                versionNumber.patch,
                if (versionNumber.qualifier != null) "-${versionNumber.qualifier}" else ""
            )
        }
    }
}
//endregion

