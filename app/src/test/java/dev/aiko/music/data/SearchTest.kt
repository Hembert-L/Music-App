package dev.aiko.music.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    @Test fun searchIgnoresAccentsCaseAndOuterWhitespace() {
        assertEquals("cancion de corazon", "  CANCIÓN de Corazón  ".searchKey())
        assertEquals("montagem direcao", "MONTAGEM DIREÇÃO".searchKey())
    }
}
