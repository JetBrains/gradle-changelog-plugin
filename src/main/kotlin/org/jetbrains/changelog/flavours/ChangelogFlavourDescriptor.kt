package org.jetbrains.changelog.flavours

import java.net.URI
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.TrimmingInlineHolderProvider
import org.intellij.markdown.parser.LinkMap

class ChangelogFlavourDescriptor : GFMFlavourDescriptor() {

    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?) =
            super.createHtmlGeneratingProviders(linkMap, baseURI) + hashMapOf(
                    MarkdownElementTypes.MARKDOWN_FILE to TrimmingInlineHolderProvider()
            )
}
