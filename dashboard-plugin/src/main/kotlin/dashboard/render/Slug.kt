package dashboard.render

import java.text.Normalizer

/** Deterministic, file-safe slug for static borough pages. */
object Slug {
    fun of(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        val ascii = decomposed.replace(Regex("\\p{M}+"), "")
        return ascii
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "borough" }
    }

    fun fileName(value: String): String = "borough-${of(value)}.html"
}
