package org.javacs.kt

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.javacs.kt.commands.ALL_COMMANDS
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class KotlinLanguageServer: LanguageServer, LanguageClientAware {
    val classPath = CompilerClassPath()
    val sourcePath = SourcePath(classPath)
    val sourceFiles = SourceFiles(sourcePath)
    private val workspaces = KotlinWorkspaceService(sourceFiles, sourcePath, classPath)
    private val textDocuments = KotlinTextDocumentService(sourceFiles, sourcePath)

    override fun connect(client: LanguageClient) {
        workspaces.connect(client)
        textDocuments.connect(client)

        LOG.info("Connected to client")
    }

    override fun shutdown(): CompletableFuture<Any> {
        return completedFuture(null)
    }

    override fun getTextDocumentService(): KotlinTextDocumentService {
        return textDocuments
    }

    override fun exit() {
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities()
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental)
        capabilities.workspace = WorkspaceServerCapabilities()
        capabilities.workspace.workspaceFolders = WorkspaceFoldersOptions()
        capabilities.workspace.workspaceFolders.supported = true
        capabilities.workspace.workspaceFolders.changeNotifications = Either.forRight(true)
        capabilities.hoverProvider = true
        capabilities.completionProvider = CompletionOptions(false, listOf("."))
        capabilities.signatureHelpProvider = SignatureHelpOptions(listOf("(", ","))
        capabilities.definitionProvider = true
        capabilities.documentSymbolProvider = true
        capabilities.workspaceSymbolProvider = true
        capabilities.referencesProvider = true
        capabilities.codeActionProvider = true
        capabilities.executeCommandProvider = ExecuteCommandOptions(ALL_COMMANDS)
        capabilities.documentRangeFormattingProvider = true

        if (params.rootUri != null) {
            LOG.info("Adding workspace ${params.rootUri} to source path")

            val root = Paths.get(URI.create(params.rootUri))

            sourceFiles.addWorkspaceRoot(root)
            classPath.addWorkspaceRoot(root)
        }

        return completedFuture(InitializeResult(capabilities))
    }

    override fun getWorkspaceService(): KotlinWorkspaceService {
        return workspaces
    }
}
