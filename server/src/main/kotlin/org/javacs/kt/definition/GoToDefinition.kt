package org.javacs.kt.definition

import org.eclipse.lsp4j.Location
import org.javacs.kt.CompiledFile
import org.javacs.kt.LOG
import org.javacs.kt.externalsources.Decompiler
import org.javacs.kt.externalsources.FernflowerDecompiler
import org.javacs.kt.externalsources.parseUriAsClassInJar
import org.javacs.kt.position.location
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtNamedDeclaration

private val decompiler: Decompiler = FernflowerDecompiler()
private val decompiledClassesCache = mutableMapOf<String, String>()

fun goToDefinition(file: CompiledFile, cursor: Int): Location? {
    val (_, target) = file.referenceAtPoint(cursor) ?: return null

    LOG.info("Found declaration descriptor {}", target)
    var destination = location(target)
    val psi = target.findPsi()

    if (psi is KtNamedDeclaration) {
        destination = psi.nameIdentifier?.let(::location) ?: destination
    }

    if (destination != null) {
        val rawClassURI = destination.uri
        if (isInsideJar(rawClassURI)) {
            destination.uri = cachedDecompile(rawClassURI)
        }
    }

    return destination
}

private fun isInsideJar(uri: String) = uri.contains(".jar!")

private fun cachedDecompile(uri: String) = decompiledClassesCache[uri] ?: decompile(uri)

private fun decompile(uri: String): String {
    val decompiledUri = parseUriAsClassInJar(uri)
            .decompile(decompiler)
            .toUri()
            .toString()
    decompiledClassesCache[uri] = decompiledUri
    return decompiledUri
}
