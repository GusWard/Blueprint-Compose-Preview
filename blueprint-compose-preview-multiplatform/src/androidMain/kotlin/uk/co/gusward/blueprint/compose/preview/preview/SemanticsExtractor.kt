package uk.co.gusward.blueprint.compose.preview.preview

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalInspectionMode
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import uk.co.gusward.blueprint.compose.preview.items.WherePossible
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.ViewRootForTest

private val staticBlueprintCache = mutableMapOf<String, Map<String, BlueprintItemData>>()

@Composable
actual fun rememberBlueprintItems(
    showInternalItems: Boolean,
    stabilityId: String
): Map<String, BlueprintItemData> {
    var blueprintItemDataState by remember {
        mutableStateOf(staticBlueprintCache[stabilityId] ?: emptyMap())
    }
    val view = LocalView.current
    val isInspectionMode = LocalInspectionMode.current

    DisposableEffect(view, showInternalItems) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (view.width > 0 && view.height > 0) {
                try {
                    val newMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
                    if (newMap.isNotEmpty() && newMap != blueprintItemDataState) {
                        blueprintItemDataState = newMap
                        staticBlueprintCache[stabilityId] = newMap
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PassiveBlueprint", "Recovery extraction failed", e)
                }
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    if (isInspectionMode) {
        try {
            val immediateMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
            if (immediateMap.isNotEmpty() && immediateMap != blueprintItemDataState) {
                blueprintItemDataState = immediateMap
                staticBlueprintCache[stabilityId] = immediateMap
            }
        } catch (e: Exception) {}
    }

    return blueprintItemDataState
}

private fun findAndroidComposeView(view: View): View? {
    if (view.javaClass.name.contains("AndroidComposeView")) {
        return view
    }
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            val found = findAndroidComposeView(view.getChildAt(i))
            if (found != null) return found
        }
    }
    return null
}

private val suppressedNodes = mutableSetOf<Int>()

internal fun extractBlueprintItemsFromSemantics(view: View, showInternalItems: Boolean = true): Map<String, BlueprintItemData> {
    val items = mutableMapOf<String, BlueprintItemData>()
    suppressedNodes.clear() 

    val screenSize = Size(view.width.toFloat(), view.height.toFloat())

    var composeView: ViewRootForTest? = null
    var currentView: View? = view
    while (currentView != null) {
        if (currentView is ViewRootForTest) {
            composeView = currentView
            break
        }
        currentView = currentView.parent as? View
    }

    if (composeView != null) {
        try {
            val semanticsOwner = composeView.semanticsOwner
            val rootNode = try {
                val unmergedProperty = semanticsOwner.javaClass.getDeclaredMethod("getUnmergedRootSemanticsNode")
                unmergedProperty.isAccessible = true
                unmergedProperty.invoke(semanticsOwner) as androidx.compose.ui.semantics.SemanticsNode
            } catch (e: Exception) {
                semanticsOwner.rootSemanticsNode
            }
            traverseSemanticsNode(rootNode, items, screenSize)
        } catch (e: Exception) {
            android.util.Log.e("PassiveBlueprint", "Direct extraction failed", e)
        }
    } else {
        try {
            val androidComposeView = findAndroidComposeView(view)
            if (androidComposeView != null) {
                val semanticsOwnerField = androidComposeView.javaClass.getDeclaredMethod("getSemanticsOwner")
                semanticsOwnerField.isAccessible = true
                val semanticsOwner = semanticsOwnerField.invoke(androidComposeView)

                if (semanticsOwner != null) {
                    val rootNode = try {
                        val unmergedProperty = semanticsOwner.javaClass.getDeclaredMethod("getUnmergedRootSemanticsNode")
                        unmergedProperty.isAccessible = true
                        unmergedProperty.invoke(semanticsOwner) as androidx.compose.ui.semantics.SemanticsNode
                    } catch (e: Exception) {
                        val rootSemanticsNodeMethod = semanticsOwner.javaClass.getDeclaredMethod("getRootSemanticsNode")
                        rootSemanticsNodeMethod.isAccessible = true
                        rootSemanticsNodeMethod.invoke(semanticsOwner) as androidx.compose.ui.semantics.SemanticsNode
                    }
                    traverseSemanticsNode(rootNode, items, screenSize)
                }
            }
        } catch (ex: Exception) {
            android.util.Log.e("PassiveBlueprint", "Fallback extraction failed", ex)
        }
    }
    
    return if (showInternalItems) {
        items
    } else {
        items.filter { entry ->
            val currentItem = entry.value
            items.values.none { other -> 
                other != currentItem && other.contains(currentItem)
            }
        }
    }
}

internal fun traverseSemanticsNode(node: androidx.compose.ui.semantics.SemanticsNode, items: MutableMap<String, BlueprintItemData>, screenSize: Size) {
    try {
        val id = node.id
        val config = node.config

        val role = config.getOrNull(SemanticsProperties.Role)
        val hasClickAction = config.contains(SemanticsActions.OnClick)
        val isEditableText = config.contains(SemanticsProperties.EditableText)
        val hasToggleState = config.contains(SemanticsProperties.ToggleableState)
        val isSlider = config.contains(SemanticsProperties.ProgressBarRangeInfo)

        val isRecognizedInteractive = role == Role.Button || 
                                      role == Role.Checkbox || 
                                      role == Role.Switch || 
                                      role == Role.RadioButton || 
                                      isEditableText || 
                                      hasToggleState ||
                                      hasClickAction ||
                                      isSlider

        val layoutInfo = node.layoutInfo
        val useInnerCoordinatesForBounds = isRecognizedInteractive && !isEditableText && !isSlider

        val bounds = if (useInnerCoordinatesForBounds) {
            layoutInfo.coordinates.boundsInRoot()
        } else {
            val outerCoordinates = try {
                val getModifierInfoMethod = layoutInfo.javaClass.getMethod("getModifierInfo")
                getModifierInfoMethod.isAccessible = true
                val modifierInfoList = getModifierInfoMethod.invoke(layoutInfo) as List<*>
                if (modifierInfoList.isNotEmpty()) {
                    val firstModInfo = modifierInfoList.first()!!
                    val coordsMethod = firstModInfo.javaClass.getMethod("getCoordinates")
                    coordsMethod.invoke(firstModInfo) as androidx.compose.ui.layout.LayoutCoordinates
                } else {
                    val outerCoordMethod = layoutInfo.javaClass.getMethod("getOuterCoordinator")
                    outerCoordMethod.isAccessible = true
                    outerCoordMethod.invoke(layoutInfo) as androidx.compose.ui.layout.LayoutCoordinates
                }
            } catch (e: Exception) {
                try {
                    val outerCoordMethod = layoutInfo.javaClass.getMethod("getOuterCoordinator")
                    outerCoordMethod.isAccessible = true
                    outerCoordMethod.invoke(layoutInfo) as androidx.compose.ui.layout.LayoutCoordinates
                } catch (e2: Exception) {
                    layoutInfo.coordinates
                }
            }
            outerCoordinates.boundsInRoot()
        }

        val hasVisualIdentity = try {
            val getModifierInfoMethod = layoutInfo.javaClass.getMethod("getModifierInfo")
            getModifierInfoMethod.isAccessible = true
            val modifierInfoList = getModifierInfoMethod.invoke(layoutInfo) as List<*>
            modifierInfoList.any { modInfo ->
                val getModifierMethod = modInfo?.javaClass?.getMethod("getModifier")
                val modifier = getModifierMethod?.invoke(modInfo)
                val name = modifier?.javaClass?.name ?: ""
                name.contains("Background", ignoreCase = true) || name.contains("Border", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }

        val hasSdkChildren = node.children.any { child ->
            val cc = child.config
            cc.contains(SemanticsProperties.Text) ||
            cc.contains(SemanticsProperties.ContentDescription) ||
            cc.getOrNull(SemanticsProperties.Role) != null ||
            cc.contains(SemanticsProperties.EditableText) ||
            cc.contains(SemanticsProperties.ProgressBarRangeInfo) ||
            cc.contains(SemanticsActions.OnClick)
        }
        
        val isTheStage = if (hasVisualIdentity) {
            val nodeArea = bounds.width * bounds.height
            val rootArea = screenSize.width * screenSize.height
            (nodeArea / rootArea) > 0.95f
        } else false

        var label = "Node $id"
        var hasExplicitLabel = false
        
        val testTag = config.getOrNull(SemanticsProperties.TestTag)
        if (testTag != null) {
            if (testTag == "blueprint_fallback_text" || testTag == "blueprint_internal_overlay") {
                return 
            }
            label = testTag
            hasExplicitLabel = true
        }

        if (!hasExplicitLabel) {
            val textList = config.getOrNull(SemanticsProperties.Text)
            if (!textList.isNullOrEmpty()) {
                label = textList.joinToString(", ")
                if (label == "refresh to see blueprint ☝\uFE0F") return 
                hasExplicitLabel = true
            }
        }

        if (!hasExplicitLabel) {
            val contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)
            if (!contentDescription.isNullOrEmpty()) {
                label = contentDescription.joinToString(", ")
                hasExplicitLabel = true
            }
        }

        if (!hasExplicitLabel) {
            if (isRecognizedInteractive) {
                hasExplicitLabel = true
                val childText = node.children.firstOrNull {
                    it.config.contains(SemanticsProperties.Text)
                }?.config?.getOrNull(SemanticsProperties.Text)?.joinToString(", ")

                label = when {
                    childText != null -> childText
                    isEditableText -> "TextField"
                    isSlider -> "Slider"
                    role == Role.Switch || hasToggleState -> "Switch/Toggle"
                    role == Role.Checkbox -> "Checkbox"
                    role == Role.RadioButton -> "RadioButton"
                    role == Role.Button -> "Button"
                    else -> "Clickable"
                }

                node.children.forEach { child ->
                    if (child.config.contains(SemanticsProperties.Text) ||
                        child.config.contains(SemanticsProperties.ContentDescription) ||
                        child.config.contains(SemanticsProperties.EditableText)) {
                        suppressedNodes.add(child.id)
                    }
                }
            }
        }

        if (!hasExplicitLabel && hasVisualIdentity && hasSdkChildren && !isTheStage) {
            hasExplicitLabel = true
            val titleChildText = node.children.firstOrNull { it.config.contains(SemanticsProperties.Text) }
                ?.config?.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.toString()
                
            label = if (titleChildText != null) {
                val cleaned = if (titleChildText.length > 12) titleChildText.take(12) + "..." else titleChildText
                "$cleaned Container"
            } else {
                "Container"
            }
        }

        if (hasExplicitLabel && testTag != null && node.children.isNotEmpty()) {
            val labeledChildren = node.children.filter { child ->
                val childConfig = child.config
                childConfig.contains(SemanticsProperties.TestTag) ||
                !childConfig.getOrNull(SemanticsProperties.Text).isNullOrEmpty() ||
                !childConfig.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty()
            }

            if (labeledChildren.size == 1) {
                val childToSuppress = labeledChildren.first()
                suppressedNodes.add(childToSuppress.id)
            }
        }

        if (hasExplicitLabel && !suppressedNodes.contains(id) && bounds.width > 2f && bounds.height > 2f && !node.isRoot) {
            val roundedLeft = kotlin.math.round(bounds.left * 10f) / 10f
            val roundedTop = kotlin.math.round(bounds.top * 10f) / 10f
            val roundedWidth = kotlin.math.round(bounds.width * 10f) / 10f
            val roundedHeight = kotlin.math.round(bounds.height * 10f) / 10f
            
            items[id.toString()] = BlueprintItemData(
                id = id.toString(),
                label = label,
                position = Offset(roundedLeft, roundedTop),
                size = Size(roundedWidth, roundedHeight),
                parentConnectionConfig = WherePossible
            )
        }

        val children = node.children
        for (child in children) {
            traverseSemanticsNode(child, items, screenSize)
        }
    } catch (e: Exception) {}
}
