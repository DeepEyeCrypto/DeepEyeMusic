// Copyright (C) 2026 DeepEye
// SPDX-License-Identifier: GPL-3.0-or-later

package com.deepeye.musicpro.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the count caption on the Library quick-access tiles.
 *
 * Device testing at 2.0x font scale surfaced "1 items" on the Downloads tile,
 * which reads as a defect to users. The tiles are pluralised, not templated.
 */
class ItemCountLabelTest {

    @Test
    fun `zero uses the plural form`() {
        assertEquals("0 items", itemCountLabel(0))
    }

    @Test
    fun `one uses the singular form`() {
        assertEquals("1 item", itemCountLabel(1))
    }

    @Test
    fun `two uses the plural form`() {
        assertEquals("2 items", itemCountLabel(2))
    }

    @Test
    fun `large counts use the plural form`() {
        assertEquals("1024 items", itemCountLabel(1024))
    }
}
