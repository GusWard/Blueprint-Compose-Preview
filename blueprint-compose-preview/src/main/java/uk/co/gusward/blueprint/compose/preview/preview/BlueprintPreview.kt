package uk.co.gusward.blueprint.compose.preview.preview

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors
import uk.co.gusward.blueprint.compose.preview.grid.BlueprintGrid
import uk.co.gusward.blueprint.compose.preview.items.BlueprintItemData
import uk.co.gusward.blueprint.compose.preview.items.WherePossible
import java.text.DecimalFormat

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.currentCompositeKeyHashCode
import java.util.LinkedHashMap

// THE SURVIVOR CACHE: IMMUNE TO LAYOUTLIB'S RE-COMPOSITION WIPES
// Bounded to 50 entries to prevent memory leaks in the IDE's long-running JVM
private val staticBlueprintCache = object : LinkedHashMap<Long, Map<String, BlueprintItemData>>(50, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<Long, Map<String, BlueprintItemData>>): Boolean {
        return size > 50
    }
}

@Composable
fun BlueprintPreview(
    backgroundAlpha: Float = 1f,
    contentAlpha: Float = 1f,
    showInternalItems: Boolean = false,
    content: @Composable () -> Unit
) {
    BlueprintTheme(backgroundAlpha = backgroundAlpha) {
        val compositeKey = currentCompositeKeyHashCode
        // Initialize state directly from the static cache to bypass Layoutlib zoom wipes
        var blueprintItemDataState by remember {
            mutableStateOf(staticBlueprintCache[compositeKey] ?: emptyMap())
        }
        val view = LocalView.current
        val isInspectionMode = LocalInspectionMode.current

        // This effect acts as a debounce/recovery mechanism.
        DisposableEffect(view, showInternalItems) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                if (view.width > 0 && view.height > 0) {
                    try {
                        val newMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
                        if (newMap.isNotEmpty() && newMap != blueprintItemDataState) {
                            blueprintItemDataState = newMap
                            staticBlueprintCache[compositeKey] = newMap // Anchor to cache
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

        // CRITICAL IDE HACK: Forcing a synchronous semantics extraction directly during the Compose phase.
        if (isInspectionMode) {
            try {
                val immediateMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
                if (immediateMap.isNotEmpty() && immediateMap != blueprintItemDataState) {
                    blueprintItemDataState = immediateMap
                    staticBlueprintCache[compositeKey] = immediateMap // Anchor to cache
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Reset cache if we're in diagnostic mode or just to be safe during these changes
        // staticBlueprintCache.clear() 

        Box(
            modifier = Modifier
                .clipToBounds()
                .onGloballyPositioned { layoutCoordinates ->
                    if (layoutCoordinates.size.width > 0 && layoutCoordinates.size.height > 0) {
                        val newMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
                        if (newMap.isNotEmpty() && newMap != blueprintItemDataState) {
                            blueprintItemDataState = newMap
                            staticBlueprintCache[compositeKey] = newMap
                        }
                    }
                }
        ) {
            // 1. Draw the actual content first, faded
            Box(
                modifier = Modifier
                    .alpha(contentAlpha)
            ) {
                content()
            }

            // 2. Draw the Blueprint Grid and Overlay on top, anchored to the content bounds
            Box(modifier = Modifier.matchParentSize()) {
                BlueprintGrid(
                    gridSize = 24.dp,
                    blueprintItems = blueprintItemDataState,
                    alpha = backgroundAlpha
                ) {
                if (blueprintItemDataState.isEmpty()) {
                    // Fallback state...
                    DisposableEffect(view, showInternalItems) {
                        val listener = ViewTreeObserver.OnPreDrawListener {
                            try {
                                val recoveredMap = extractBlueprintItemsFromSemantics(view, showInternalItems)
                                if (recoveredMap.isNotEmpty() && recoveredMap != blueprintItemDataState) {
                                    blueprintItemDataState = recoveredMap
                                    staticBlueprintCache[compositeKey] = recoveredMap
                                }
                            } catch (e: Exception) {}
                            true
                        }
                        view.viewTreeObserver.addOnPreDrawListener(listener)
                        onDispose { view.viewTreeObserver.removeOnPreDrawListener(listener) }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "refresh to see blueprint ☝\uFE0F",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .semantics { testTag = "blueprint_fallback_text" }
                                .background(SemanticColors.BlueprintBackground)
                                .border(1.dp, Color.White)
                                .padding(16.dp)
                        )
                    }
                } else {
                    // Draw the visual boxes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { 
                                testTag = "blueprint_internal_overlay" 
                            }
                    ) {
                        blueprintItemDataState.values.forEach { item ->
                            PassiveBlueprintItemOverlay(item, backgroundAlpha)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PassiveBlueprintItemOverlay(itemData: BlueprintItemData, backgroundAlpha: Float) {
    val decimalFormat = remember { DecimalFormat("0") }

    Box(
        modifier = Modifier
            .offset { IntOffset(itemData.position.x.toInt(), itemData.position.y.toInt()) }
            .size(
                width = LocalDensity.current.run { itemData.size.width.toDp() },
                height = LocalDensity.current.run { itemData.size.height.toDp() }
            )
            .clearAndSetSemantics { } // Hide from semantics tree to prevent infinite loops!
            .border(width = 2.dp, color = Color.White.copy(alpha = backgroundAlpha))
            .background(SemanticColors.BlueprintBackground.copy(alpha = backgroundAlpha))
    ) {
        // Draw repeated 45 degree lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.lineTo(0f, 0f)

            clipPath(path = path, clipOp = ClipOp.Intersect) {
                val spacing = size.width / 10
                var start = spacing
                do {
                    drawLine(
                        start = Offset(x = 0f, y = start),
                        end = Offset(x = start, y = 0f),
                        color = Color.White.copy(alpha = backgroundAlpha * 0.5f),
                        strokeWidth = 2f,
                    )
                    start += spacing
                } while (start < size.width + size.height)
            }
        }

        // Draw label
        val itemSize = itemData.size
        // Tiered scaling: reduce vertical padding first, then scale font
        val density = LocalDensity.current
        val itemDpHeight = density.run { itemSize.height.toDp() }
        val itemDpWidth = density.run { itemSize.width.toDp() }

        var fontSize = 12.sp
        var vPadding = 2.dp
        var hPadding = 2.dp
        
        val fullHeightNeeded = 26.dp
        val textOnlyHeight = 18.dp
        val fullWidthNeeded = 60.dp

        // 1. Height-based scaling (Linear Progress)
        if (itemDpHeight < fullHeightNeeded) {
            if (itemDpHeight >= textOnlyHeight) {
                // Stage 1: Linearly reduce padding from 2dp to 0dp
                val progress = (itemDpHeight - textOnlyHeight) / (fullHeightNeeded - textOnlyHeight)
                vPadding = (2 * progress).dp
            } else {
                // Stage 2: Padding is gone, scale font
                vPadding = 0.dp
                val heightScale = (itemDpHeight / textOnlyHeight).coerceAtLeast(0.4f)
                fontSize = (12 * heightScale).sp
                hPadding = (2 * heightScale).dp
            }
        }

        // 2. Width-based scaling (Simple)
        if (itemDpWidth < fullWidthNeeded) {
            val widthScale = (itemDpWidth / fullWidthNeeded).coerceAtLeast(0.4f)
            val currentFontValue = fontSize.value
            val widthScaledFontValue = 12 * widthScale
            if (widthScaledFontValue < currentFontValue) {
                fontSize = widthScaledFontValue.sp
                hPadding = (2 * (widthScaledFontValue / 12f)).dp
            }
        }

        if (itemSize.width > (itemSize.height * 2)) {
            Row(
                modifier = Modifier
                    .background(SemanticColors.BlueprintBackground.copy(alpha = 1f))
                    .border(1.dp, Color.White.copy(alpha = 1f))
                    .padding(horizontal = hPadding, vertical = vPadding)
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    fontSize = fontSize,
                    color = Color.White.copy(alpha = 1f),
                    fontWeight = FontWeight.Medium,
                    text = density.run {
                        val width = decimalFormat.format(itemSize.width.toDp().value)
                        val height = decimalFormat.format(itemSize.height.toDp().value)
                        "${width}x${height}"
                    },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .background(SemanticColors.BlueprintBackground.copy(alpha = 1f))
                    .border(1.dp, Color.White.copy(alpha = 1f))
                    .padding(horizontal = hPadding, vertical = vPadding)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    fontSize = fontSize,
                    color = Color.White.copy(alpha = 1f),
                    fontWeight = FontWeight.Medium,
                    text = density.run {
                        val width = decimalFormat.format(itemSize.width.toDp().value)
                        val height = decimalFormat.format(itemSize.height.toDp().value)
                        "${width}x${height}"
                    },
                )
            }
        }
    }
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

// A thread-local or temporary set to track nodes that should be ignored in the current traversal
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
            // Prefer unmerged tree to see all blueprint items
            val rootNode = try {
                val unmergedProperty = semanticsOwner.javaClass.getDeclaredMethod("getUnmergedRootSemanticsNode")
                unmergedProperty.isAccessible = true
                unmergedProperty.invoke(semanticsOwner) as androidx.compose.ui.semantics.SemanticsNode
            } catch (e: Exception) {
                semanticsOwner.rootSemanticsNode
            }
            traverseSemanticsNode(rootNode, items, screenSize)
            
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
            } else {
                android.util.Log.e("PassiveBlueprint", "Could not find AndroidComposeView in hierarchy via reflection")
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

        // Pre-determine interactivity to decide which bounds to use
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
        
        // TextFields and Sliders visually occupy their full container size, so they need outer bounds.
        // Other interactive elements (Button, Switch, Checkbox) have invisible minimum touch targets (48dp),
        // so we use inner bounds for them.
        // Everything else (Text, Images, Layouts) uses outer bounds to correctly capture backgrounds and paddings.
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

        android.util.Log.d("PassiveBlueprint", "Node $id: bounds=$bounds, semanticBounds=${node.boundsInRoot}, isInteractive=$isRecognizedInteractive")

        // Molecule Detection: Containers with backgrounds/borders and SDK children
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
        
        // Ignore "The Stage" - if a molecule occupies nearly the full screen, it's just a background container
        val isTheStage = if (hasVisualIdentity) {
            val nodeArea = bounds.width * bounds.height
            val rootArea = screenSize.width * screenSize.height
            (nodeArea / rootArea) > 0.95f
        } else false

        var label = "Node $id"
        var hasExplicitLabel = false
        
        // Priority 1: TestTag (includes blueprintId) - Developers use this for explicit naming
        val testTag = config.getOrNull(SemanticsProperties.TestTag)
        if (testTag != null) {
            if (testTag == "blueprint_fallback_text" || testTag == "blueprint_internal_overlay") {
                return // PRUNE THE SUBTREE
            }
            label = testTag
            hasExplicitLabel = true
        }

        // Priority 2: Text content - Fallback for untagged items
        if (!hasExplicitLabel) {
            val textList = config.getOrNull(SemanticsProperties.Text)
            if (!textList.isNullOrEmpty()) {
                label = textList.joinToString(", ")
                if (label == "refresh to see blueprint ☝\uFE0F") return // Ignore our own UI
                hasExplicitLabel = true
            }
        }

        // Priority 3: Content Description
        if (!hasExplicitLabel) {
            val contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)
            if (!contentDescription.isNullOrEmpty()) {
                label = contentDescription.joinToString(", ")
                hasExplicitLabel = true
            }
        }

        // Priority 4: Interactive elements (Buttons, TextFields, Switches, etc.) - Implicit recognition
        if (!hasExplicitLabel) {
            if (isRecognizedInteractive) {
                hasExplicitLabel = true

                // Try to find a label from its children (e.g. the Text inside the button or label of a TextField)
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

                // Since we've absorbed the meaning into this outer container, suppress labeled children
                node.children.forEach { child ->
                    if (child.config.contains(SemanticsProperties.Text) ||
                        child.config.contains(SemanticsProperties.ContentDescription) ||
                        child.config.contains(SemanticsProperties.EditableText)) {
                        suppressedNodes.add(child.id)
                    }
                }
            }
        }

        // Priority 5: Molecule Detection (Layouts with Background + SDK children)
        if (!hasExplicitLabel && hasVisualIdentity && hasSdkChildren && !isTheStage) {
            hasExplicitLabel = true
            
            // Try to find a hint for the label from the first text child
            val titleChildText = node.children.firstOrNull { it.config.contains(SemanticsProperties.Text) }
                ?.config?.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.toString()
                
            label = if (titleChildText != null) {
                val cleaned = if (titleChildText.length > 12) titleChildText.take(12) + "..." else titleChildText
                "$cleaned Container"
            } else {
                "Container"
            }
        }

        // --- ID PREFERENCE & REDUNDANCY FILTER ---
        // If this node has an EXPLICIT TestTag AND it's a layout (not a Leaf node),
        // we check for a single labeled child to "absorb" to prevent double blueprints.
        // We MUST verify testTag != null, otherwise untagged parents might accidentally
        // inherit a child's label and suppress other children.
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
            // Round bounds to 1 decimal place to prevent sub-pixel recomposition loops
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

        // ALWAYS traverse children
        val children = node.children
        for (child in children) {
            traverseSemanticsNode(child, items, screenSize)
        }
    } catch (e: Exception) {
        android.util.Log.e("PassiveBlueprint", "Failed traversing node", e)
    }
}