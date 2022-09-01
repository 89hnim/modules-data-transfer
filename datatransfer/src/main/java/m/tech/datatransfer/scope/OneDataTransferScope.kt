package m.tech.datatransfer.scope

sealed class OneDataTransferScope {
    object Application : OneDataTransferScope()

    data class Custom(private val scope: String) : OneDataTransferScope()
}