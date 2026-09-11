package org.levimc.launcher.core.mods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ModNativeLoaderTest {
    @Test
    public void exactPatternMatchesOnlySameVersion() {
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.20", "1.26.20"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.20", "1.26.21"));
    }

    @Test
    public void wildcardPatternMatchesPrefix() {
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.20"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.21"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.22"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.30"));
    }

    @Test
    public void numericWildcardMatchesHotfixFamily() {
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.5X.X", "1.26.50.1"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.5X.X", "1.26.59.12"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.5x.x", "1.26.54.2"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.5X.X", "1.26.60.1"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.5X.X", "1.26.500.1"));
    }

    @Test
    public void anyPatternCanMatch() {
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Arrays.asList("1.25.*", "1.26.2*", "1.27.0"),
                "1.26.21"));
    }

    @Test
    public void missingOrEmptyPatternsAllowAllVersions() {
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion((java.util.List<String>) null, "1.26.21"));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(Collections.emptyList(), "1.26.21"));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(Arrays.asList("", "   "), "1.26.21"));
    }

    @Test
    public void incompatiblePatternRejectsVersion() {
        assertFalse(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Arrays.asList("1.25.*", "1.26.20"),
                "1.26.21"));
    }
}
