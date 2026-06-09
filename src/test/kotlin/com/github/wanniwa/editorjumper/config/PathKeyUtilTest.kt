package com.github.wanniwa.editorjumper.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PathKeyUtilTest {
    @Test
    fun normalizeAnchorPath_unifiesSlashes() {
        val normalized = PathKeyUtil.normalizeAnchorPath("/Users/test/myproject")
        assertEquals("/Users/test/myproject", normalized)
    }

    @Test
    fun computeConfigKey_isStableForSamePath() {
        val path = "/Users/test/myproject"
        assertEquals(
            PathKeyUtil.computeConfigKey(path),
            PathKeyUtil.computeConfigKey(path),
        )
    }

    @Test
    fun computeConfigKey_differsForDifferentPaths() {
        assertNotEquals(
            PathKeyUtil.computeConfigKey("/Users/a/project"),
            PathKeyUtil.computeConfigKey("/Users/b/project"),
        )
    }

    @Test
    fun buildProjectCacheFileName_containsSuffix() {
        val key = PathKeyUtil.computeConfigKey("/tmp/demo")
        val name = PathKeyUtil.buildProjectCacheFileName(key, "/tmp/demo", "jumper")
        assertEquals(true, name.endsWith("_jumper.json"))
        assertEquals(true, name.startsWith(key))
    }
}
