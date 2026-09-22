package com.aperture.camera.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aperture.camera.document.DocumentEnhancementFilter
import com.aperture.camera.document.DocumentScannerHelper
import com.aperture.camera.document.ScannedDocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

enum class DocumentEditorStep {
    CROP_CORNERS,
    ENHANCE_AND_SAVE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentScanEditorScreen(
    initialBitmap: Bitmap,
    onSavePdf: (ByteArray, String) -> Unit,
    onSaveImage: (ByteArray, String) -> Unit,
    onAddAnotherPage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(DocumentEditorStep.CROP_CORNERS) }
    var isProcessing by remember { mutableStateOf(false) }

    // Pages list
    val pages = remember {
        mutableStateListOf(
            ScannedDocumentPage(
                originalBitmap = initialBitmap,
                corners = DocumentScannerHelper.detectDocumentCorners(initialBitmap)
            )
        )
    }
    var activePageIndex by remember { mutableIntStateOf(0) }
    val activePage = pages.getOrNull(activePageIndex) ?: return

    // Warped preview for enhancement step
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(DocumentEnhancementFilter.MAGIC_COLOR) }
    var rotationAngle by remember { mutableIntStateOf(0) }

    // Update warped preview when entering enhance step or modifying filter/rotation
    fun refreshWarpedPreview() {
        scope.launch {
            isProcessing = true
            val warped = DocumentScannerHelper.cropAndWarpDocument(activePage.originalBitmap, activePage.corners)
            val rotated = DocumentScannerHelper.rotateBitmap(warped, rotationAngle)
            val enhanced = DocumentScannerHelper.applyEnhancement(rotated, selectedFilter)
            processedBitmap = enhanced
            isProcessing = false
        }
    }

    LaunchedEffect(currentStep, selectedFilter, rotationAngle, activePageIndex) {
        if (currentStep == DocumentEditorStep.ENHANCE_AND_SAVE) {
            refreshWarpedPreview()
        }
    }

    Scaffold(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentStep == DocumentEditorStep.CROP_CORNERS) "Adjust Document Corners" else "Document Preview & Filters",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == DocumentEditorStep.ENHANCE_AND_SAVE) {
                            currentStep = DocumentEditorStep.CROP_CORNERS
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (currentStep == DocumentEditorStep.CROP_CORNERS) {
                        IconButton(onClick = {
                            val autoCorners = DocumentScannerHelper.detectDocumentCorners(activePage.originalBitmap)
                            pages[activePageIndex] = activePage.copy(corners = autoCorners)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Auto Detect Corners",
                                tint = Color(0xFFFFD600)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0E11))
            )
        },
        containerColor = Color(0xFF0D0E11)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentStep) {
                DocumentEditorStep.CROP_CORNERS -> {
                    DocumentCornerCropView(
                        page = activePage,
                        onCornersUpdated = { updatedCorners ->
                            pages[activePageIndex] = activePage.copy(corners = updatedCorners)
                        },
                        onNextStep = {
                            currentStep = DocumentEditorStep.ENHANCE_AND_SAVE
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DocumentEditorStep.ENHANCE_AND_SAVE -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Processed Document Preview
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (processedBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = processedBitmap!!.asImageBitmap(),
                                    contentDescription = "Document Page",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                )
                            }
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color(0xFFFFD600))
                            }
                        }

                        // 2. Filter Selector Chips
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (filter in DocumentEnhancementFilter.values()) {
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter.title, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD600),
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // 3. Page Strip & Rotation Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    rotationAngle = (rotationAngle + 90) % 360
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "Rotate",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate 90°", color = Color.White, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onAddAnotherPage,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Page",
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Page (${pages.size})", color = Color(0xFFFFD600), fontSize = 12.sp)
                            }
                        }

                        // 4. Save & Export Actions Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val bmp = processedBitmap ?: return@launch
                                        val jpegBytes = DocumentScannerHelper.compressToJpeg(bmp, 95)
                                        onSaveImage(jpegBytes, "SCAN_${System.currentTimeMillis()}.jpg")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save JPG", color = Color.White, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val bmp = processedBitmap ?: return@launch
                                        val pdfBytes = DocumentScannerHelper.generatePdf(listOf(bmp))
                                        onSavePdf(pdfBytes, "DOC_${System.currentTimeMillis()}.pdf")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCornerCropView(
    page: ScannedDocumentPage,
    onCornersUpdated: (List<PointF>) -> Unit,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = page.originalBitmap
    var currentCorners by remember(page.id) { mutableStateOf(page.corners) }
    var draggingCornerIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            val imgWidth = bitmap.width.toFloat()
            val imgHeight = bitmap.height.toFloat()

            // Calculate fit scale and image offsets inside Box
            val scale = min(viewWidth / imgWidth, viewHeight / imgHeight)
            val displayWidth = imgWidth * scale
            val displayHeight = imgHeight * scale
            val offsetX = (viewWidth - displayWidth) / 2f
            val offsetY = (viewHeight - displayHeight) / 2f

            // Helper to map between Bitmap coordinates and Screen pixels
            fun bitmapToScreen(pt: PointF): Offset {
                return Offset(offsetX + pt.x * scale, offsetY + pt.y * scale)
            }

            fun screenToBitmap(screenX: Float, screenY: Float): PointF {
                val minEdgePad = imgWidth * 0.02f
                val minEdgePadY = imgHeight * 0.02f
                val bx = ((screenX - offsetX) / scale).coerceIn(minEdgePad, imgWidth - minEdgePad)
                val by = ((screenY - offsetY) / scale).coerceIn(minEdgePadY, imgHeight - minEdgePadY)
                return PointF(bx, by)
            }

            val density = LocalDensity.current
            val boxWidthDp = with(density) { displayWidth.toDp() }
            val boxHeightDp = with(density) { displayHeight.toDp() }

            Box(
                modifier = Modifier
                    .size(
                        width = boxWidthDp,
                        height = boxHeightDp
                    )
            ) {
                // Background image
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Original Capture",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Draggable Quadrilateral Outline Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val touchBmp = screenToBitmap(startOffset.x + offsetX, startOffset.y + offsetY)
                                    val threshold = 95f / scale
                                    var closestIdx: Int? = null
                                    var closestDist = Float.MAX_VALUE

                                    for ((idx, corner) in currentCorners.withIndex()) {
                                        val dist = hypot(corner.x - touchBmp.x, corner.y - touchBmp.y)
                                        if (dist < threshold && dist < closestDist) {
                                            closestDist = dist
                                            closestIdx = idx
                                        }
                                    }
                                    draggingCornerIndex = closestIdx
                                },
                                onDrag = { change, _ ->
                                    val idx = draggingCornerIndex ?: return@detectDragGestures
                                    change.consume()
                                    val newBmpPt = screenToBitmap(change.position.x + offsetX, change.position.y + offsetY)
                                    val updated = currentCorners.toMutableList()
                                    updated[idx] = newBmpPt
                                    currentCorners = updated
                                },
                                onDragEnd = {
                                    draggingCornerIndex = null
                                    onCornersUpdated(currentCorners)
                                },
                                onDragCancel = {
                                    draggingCornerIndex = null
                                }
                            )
                        }
                ) {
                    if (currentCorners.size == 4) {
                        val p0 = Offset(currentCorners[0].x * scale, currentCorners[0].y * scale)
                        val p1 = Offset(currentCorners[1].x * scale, currentCorners[1].y * scale)
                        val p2 = Offset(currentCorners[2].x * scale, currentCorners[2].y * scale)
                        val p3 = Offset(currentCorners[3].x * scale, currentCorners[3].y * scale)

                        // Draw bounding quad polygon line
                        val path = Path().apply {
                            moveTo(p0.x, p0.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            close()
                        }
                        drawPath(path, Color(0xFFFFD600), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))

                        // Draw 4 corner handles
                        val pts = listOf(p0, p1, p2, p3)
                        for ((idx, pt) in pts.withIndex()) {
                            val isSelected = idx == draggingCornerIndex
                            drawCircle(
                                color = if (isSelected) Color(0xFFFF3B30) else Color(0xFFFFD600),
                                radius = if (isSelected) 14.dp.toPx() else 10.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }
        }

        // Instructions and Done Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Drag the 4 yellow corners to fit the document boundaries",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNextStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Straighten & Enhance Document", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
