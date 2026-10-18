package com.virex.music.core.util

import java.io.File
import java.text.Normalizer

object SafeFiles {
 fun safeName(value: String): String {
  val normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
  val cleaned = normalized.replace(Regex("[^A-Za-z0-9._ -]"), "").replace(Regex("\\s+"), "_").trim('.', '_', ' ')
  return cleaned.take(80).ifBlank { "track" }
 }
 fun inside(parent: File, child: File): Boolean {
  val p = parent.canonicalFile.toPath()
  val c = child.canonicalFile.toPath()
  return c.startsWith(p) && c != p
 }
}
