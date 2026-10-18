package in.virex.music.core.util

import org.junit.Assert.*
import org.junit.Test

class SafeFilesTest {
 @Test fun sanitizesUnsafeFilenameCharacters() {
  val value = SafeFiles.safeName("../../hello: world?.mp3")
  assertFalse(value.contains("/"))
  assertFalse(value.contains(":"))
  assertFalse(value.contains("?"))
  assertTrue(value.isNotBlank())
 }
}
