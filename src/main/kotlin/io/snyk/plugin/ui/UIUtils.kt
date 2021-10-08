package io.snyk.plugin.ui

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBHtmlEditorKit
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.snyk.plugin.pluginSettings
import io.snyk.plugin.isSnykCodeAvailable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.text.html.HTMLDocument

fun boldLabel(title: String): JLabel {
    val label = JLabel(title)
    val labelFont = label.font
    label.font = labelFont.deriveFont(labelFont.style or Font.BOLD)

    return label
}

fun iconLabel(imageIcon: ImageIcon): JLabel {
    val label = JLabel()
    label.horizontalAlignment = 0
    label.icon = imageIcon
    label.text = ""

    return label
}

fun getFont(style: Int, size: Int, currentFont: Font?): Font? {
    if (currentFont == null) {
        return null
    }

    return Font(currentFont.name, if (style >= 0) style else currentFont.style, if (size >= 0) size else currentFont.size)
}

fun buildBoldTitleLabel(title: String): JLabel {
    val bold16pxLabel = JLabel(title)
    val detailedPathsAndRemediationLabelFont: Font? = getFont(Font.BOLD, 16, bold16pxLabel.font)

    if (detailedPathsAndRemediationLabelFont != null) {
        bold16pxLabel.font = detailedPathsAndRemediationLabelFont
    }

    return bold16pxLabel
}

fun buildTwoLabelsPanel(title: String, text: String): JPanel {
    val titleLabel = JLabel()
    val vulnerableModuleLabelFont: Font? = getFont(Font.BOLD, -1, titleLabel.font)

    if (vulnerableModuleLabelFont != null) {
        titleLabel.font = vulnerableModuleLabelFont
    }

    titleLabel.text = title

    val wrapPanel = JPanel()

    wrapPanel.add(titleLabel)
    wrapPanel.add(JLabel(text))

    return wrapPanel
}

fun buildTextAreaWithLabelPanel(title: String, text: String): JPanel {
    val titleLabel = JLabel()
    val vulnerableModuleLabelFont: Font? = getFont(Font.BOLD, -1, titleLabel.font)

    if (vulnerableModuleLabelFont != null) {
        titleLabel.font = vulnerableModuleLabelFont
    }

    titleLabel.text = title

    val wrapPanel = JPanel(BorderLayout())

    wrapPanel.add(titleLabel, BorderLayout.WEST)

    val textArea = JTextArea(text)
    textArea.lineWrap = true
    textArea.wrapStyleWord = true
    textArea.isOpaque = false
    textArea.isEditable = false
    textArea.background = UIUtil.getPanelBackground()

    wrapPanel.add(ScrollPaneFactory.createScrollPane(textArea, true), BorderLayout.CENTER)

    return wrapPanel
}

fun snykCodeAvailabilityPostfix(): String = when {
    !isSnykCodeAvailable(pluginSettings().customEndpointUrl) -> " (disabled for endpoint)"
    !(pluginSettings().sastOnServerEnabled ?: false) -> " (disabled for organization)"
    else -> ""
}

fun getReadOnlyClickableHtmlJEditorPane(htmlText: String): JEditorPane {
    // don't remove that!
    // Some magic (side-effect? customStyleSheet?) happens when JBHtmlEditorKit() initializing
    // that make html tags like <em>, <p>, <ul> etc. be treated properly inside JEditorPane
    JBHtmlEditorKit()

    return JEditorPane(
        "text/html",
        "<html>$htmlText</html>"
    ).apply {
        isEditable = false
        background = UIUtil.getPanelBackground()
        preferredSize = Dimension() // this is the key part for shrink/grow.

        // add a CSS rule to force body tags to use the default label font
        // instead of the value in javax.swing.text.html.default.csss
        val font = UIUtil.getLabelFont()
        val fontColor = UIUtil.getTextFieldForeground()
        val bodyRule = UIUtil.displayPropertiesToCSS(font, fontColor) +
            "a { color: #${ColorUtil.toHex(JBUI.CurrentTheme.Link.linkColor())}; }"
        (document as HTMLDocument).styleSheet.addRule(bodyRule)

        // open clicked link in browser
        addHyperlinkListener {
            BrowserHyperlinkListener.INSTANCE.hyperlinkUpdate(it)
        }
    }
}
