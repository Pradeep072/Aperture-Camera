package com.aperture.camera.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aperture.camera.data.model.CameraSpec
import com.aperture.camera.data.model.FullDeviceDiagnostics
import com.aperture.camera.data.model.HardwareSensorInfo
import com.aperture.camera.data.model.LensType
import com.aperture.camera.data.model.MemoryDiagnostics
import com.aperture.camera.data.model.StorageDiagnostics
import com.aperture.camera.data.model.SystemInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutCameraScreen(
    viewModel: AboutCameraViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    var selectedFilterTab by remember { mutableStateOf("ALL_PHYSICAL") }

    Scaffold(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hardware & Sensors Info",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHardwareSpecs() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0E11)
                )
            )
        },
        containerColor = Color(0xFF0D0E11)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD600))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Probing camera and hardware sensors...",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            } else if (uiState.errorMessage != null && uiState.specs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hardware Detection Error",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadHardwareSpecs() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                    ) {
                        Text("Retry Probe", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val diag = uiState.diagnostics
                val hasPhysicalBack = uiState.specs.any { it.isPhysical && !it.isFront }
                val logicalSpecs = uiState.specs.filter { !it.isPhysical && (it.physicalCameraIds.isNotEmpty() || (hasPhysicalBack && it.cameraId == "0")) }
                val physicalSpecs = uiState.specs.filter { it !in logicalSpecs }

                val filteredSpecs = when (selectedFilterTab) {
                    "ALL_PHYSICAL" -> physicalSpecs
                    "REAR" -> physicalSpecs.filter { !it.isFront }
                    "FRONT" -> physicalSpecs.filter { it.isFront }
                    "PIPELINE" -> logicalSpecs
                    else -> uiState.specs
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Device & System Summary Card
                    if (diag != null) {
                        item {
                            SystemProfileCard(
                                system = diag.system,
                                onCopySpecs = { copyToClipboard(context, uiState.markdownReport) },
                                onShareSpecs = { shareSpecs(context, uiState.markdownReport) }
                            )
                        }

                        // 2. RAM and Storage Usage Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MemoryResourceCard(
                                    memory = diag.memory,
                                    modifier = Modifier.weight(1f)
                                )
                                StorageResourceCard(
                                    storage = diag.storage,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 3. Camera Subsystem Overview Card
                        item {
                            CameraSubsystemCard(overview = diag.cameraOverview)
                        }

                        // 4. Camera-Relevant Hardware Sensors
                        item {
                            HardwareSensorsSection(sensors = diag.hardwareSensors)
                        }
                    }

                    // 5. Camera Sensors Specs Header & Filters
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CAMERA SENSORS (${filteredSpecs.size} SHOWN)",
                                    color = Color(0xFFFFD600),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedFilterTab == "ALL_PHYSICAL",
                                    onClick = { selectedFilterTab = "ALL_PHYSICAL" },
                                    label = { Text("Physical Cameras (${physicalSpecs.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD600),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = selectedFilterTab == "REAR",
                                    onClick = { selectedFilterTab = "REAR" },
                                    label = { Text("Rear (${physicalSpecs.count { !it.isFront }})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD600),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = selectedFilterTab == "FRONT",
                                    onClick = { selectedFilterTab = "FRONT" },
                                    label = { Text("Front (${physicalSpecs.count { it.isFront }})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD600),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                if (logicalSpecs.isNotEmpty()) {
                                    FilterChip(
                                        selected = selectedFilterTab == "PIPELINE",
                                        onClick = { selectedFilterTab = "PIPELINE" },
                                        label = { Text("Fusion Pipeline (${logicalSpecs.size})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFD600),
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                                FilterChip(
                                    selected = selectedFilterTab == "ALL",
                                    onClick = { selectedFilterTab = "ALL" },
                                    label = { Text("All (${uiState.specs.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD600),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // 6. Camera Specification Cards
                    items(filteredSpecs, key = { it.cameraId + it.isPhysical + it.parentLogicalId }) { spec ->
                        val key = spec.cameraId + spec.isPhysical + spec.parentLogicalId
                        val isExpanded = expandedStates[key] ?: (spec == filteredSpecs.firstOrNull())
                        CameraSpecCard(
                            spec = spec,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedStates[key] = !isExpanded
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemProfileCard(
    system: SystemInfo,
    onCopySpecs: () -> Unit,
    onShareSpecs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD600).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${system.manufacturer} ${system.model}",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android ${system.androidVersion} (API ${system.apiLevel})",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF282B34))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Codename: ${system.deviceCodename}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SoC: ${system.socModel}",
                    color = Color(0xFFFFD600),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Display: ${system.displayResolution} @ %.0fHz".format(Locale.US, system.refreshRateHz),
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopySpecs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = onShareSpecs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MemoryResourceCard(
    memory: MemoryDiagnostics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = Color(0xFF64D2FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RAM Usage",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "%.1f / %.1f GB".format(Locale.US, memory.usedRamGb, memory.totalRamGb),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (memory.ramUsagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (memory.ramUsagePercent > 85) Color(0xFFFF453A) else Color(0xFF64D2FF),
                trackColor = Color(0xFF282B34)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${memory.ramUsagePercent}% Used (${String.format(Locale.US, "%.1f", memory.availableRamGb)} GB Free)",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StorageResourceCard(
    storage: StorageDiagnostics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Storage",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "%.1f / %.1f GB".format(Locale.US, storage.usedStorageGb, storage.totalStorageGb),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (storage.storageUsagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (storage.storageUsagePercent > 90) Color(0xFFFF453A) else Color(0xFF30D158),
                trackColor = Color(0xFF282B34)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${storage.storageUsagePercent}% Used (${String.format(Locale.US, "%.1f", storage.freeStorageGb)} GB Free)",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraSubsystemCard(overview: com.aperture.camera.data.model.CameraSubsystemOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Camera Subsystem Overview",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewStatBadge("Physical Cameras", "${overview.totalSensorsCount}")
                OverviewStatBadge("Rear Sensors", "${overview.backCamerasCount}")
                OverviewStatBadge("Front Sensors", "${overview.frontCamerasCount}")
                OverviewStatBadge("Logical Pipeline", "${overview.logicalCamerasCount}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeatureBadge("Multi-Camera HAL", overview.isMultiCameraSupported)
                FeatureBadge("Concurrent Dual Stream", overview.isConcurrentDualSupported)
                FeatureBadge("10-bit HDR Profiles", overview.is10BitHdrSupported)
            }
        }
    }
}

@Composable
private fun OverviewStatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color(0xFFFFD600),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HardwareSensorsSection(sensors: List<HardwareSensorInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Camera-Relevant Sensors (${sensors.count { it.isAvailable }}/${sensors.size} Active)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            for (sensor in sensors) {
                SensorItemRow(sensor = sensor)
                if (sensor != sensors.last()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF252830))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SensorItemRow(sensor: HardwareSensorInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sensor.typeName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (sensor.isAvailable) {
                    Surface(
                        color = Color(0xFF30D158).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "AVAILABLE",
                            color = Color(0xFF30D158),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "N/A",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = sensor.purpose,
                color = Color.LightGray,
                fontSize = 11.sp
            )

            if (sensor.isAvailable && sensor.name != "Not Available") {
                Text(
                    text = "${sensor.name} • ${sensor.vendor}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraSpecCard(
    spec: CameraSpec,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181F)),
        border = if (isExpanded) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Camera ID & Lens Type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (spec.isFront) Color(0xFFFF9F0A).copy(alpha = 0.2f)
                                else Color(0xFFFFD600).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${spec.cameraId}",
                            color = if (spec.isFront) Color(0xFFFF9F0A) else Color(0xFFFFD600),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        val isLogicalPipeline = !spec.isPhysical && (spec.physicalCameraIds.isNotEmpty() || spec.isLogicalMultiCamera || spec.cameraId == "0")
                        val cameraTitle = when {
                            isLogicalPipeline -> "${spec.facing} Fusion Pipeline"
                            spec.isFront -> "Front Selfie Camera"
                            spec.lensType == LensType.WIDE -> "Rear Primary Wide"
                            spec.lensType == LensType.ULTRA_WIDE -> "Rear Ultra-Wide / Macro"
                            spec.lensType == LensType.DEPTH -> "Rear Depth Sensor"
                            spec.lensType == LensType.MACRO -> "Rear Macro Lens"
                            spec.lensType == LensType.TELEPHOTO -> "Rear Telephoto Lens"
                            else -> "${spec.facing} ${spec.lensType.displayName}"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cameraTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isLogicalPipeline) {
                                Surface(
                                    color = Color(0xFFBF5AF2).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LOGICAL PIPELINE",
                                        color = Color(0xFFD084F8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF0A84FF).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PHYSICAL SENSOR",
                                        color = Color(0xFF64D2FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val subtitle = if (isLogicalPipeline) {
                            "Fuses Sensors #${spec.physicalCameraIds.joinToString(", #")} • Logical HAL ID"
                        } else {
                            if (spec.isQuadBayer) {
                                "%.0f MP Quad-Bayer (%.1f MP Output) • ~%.0fmm • f/%.1f".format(
                                    Locale.US,
                                    spec.sensorMegaPixels,
                                    spec.binnedOutputMegaPixels,
                                    spec.focalLength35mm,
                                    spec.apertures.firstOrNull() ?: 1.8f
                                )
                            } else {
                                "%.1f MP • ~%.0fmm 35mm-eq • f/%.1f".format(
                                    Locale.US,
                                    spec.sensorMegaPixels,
                                    spec.focalLength35mm,
                                    spec.apertures.firstOrNull() ?: 1.8f
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = Color(0xFF282B34))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Optics & Sensor Matrix
                    SectionHeader("OPTICS & SENSOR MATRIX")
                    if (spec.isQuadBayer) {
                        SpecDetailRow("Hardware Sensor Matrix", "%.1f MP (%s)".format(Locale.US, spec.sensorMegaPixels, spec.maxPixelArraySize ?: spec.sensorPixelArraySize))
                        SpecDetailRow("Default Output (Binned)", "%.1f MP (%s)".format(Locale.US, spec.binnedOutputMegaPixels, spec.sensorPixelArraySize))
                        SpecDetailRow("Pixel Binning", "4-in-1 Quad-Bayer Array")
                    } else {
                        SpecDetailRow("Sensor Megapixels", "%.1f MP".format(Locale.US, spec.sensorMegaPixels))
                        SpecDetailRow("Pixel Array", spec.sensorPixelArraySize)
                    }
                    SpecDetailRow("Physical Sensor Size", spec.sensorPhysicalSize)
                    SpecDetailRow("Focal Lengths", spec.focalLengths.joinToString(", ") { "%.2f mm".format(Locale.US, it) })
                    SpecDetailRow("35mm Equivalent", "~%.1f mm (Crop: %.2fx)".format(Locale.US, spec.focalLength35mm, spec.cropFactor))
                    if (spec.fovHorizontal > 0f) {
                        SpecDetailRow("Field of View (FOV)", "%.1f° Horizontal × %.1f° Vertical".format(Locale.US, spec.fovHorizontal, spec.fovVertical))
                    }
                    SpecDetailRow("Apertures", spec.apertures.joinToString(", ") { "f/%.2f".format(Locale.US, it) })
                    SpecDetailRow("Hardware Level", spec.hardwareLevel)
                    if (spec.minFocusDistanceCm != null) {
                        SpecDetailRow("Min Focus Distance", "~%.1f cm (Macro capable)".format(Locale.US, spec.minFocusDistanceCm))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Pro Exposure & Controls
                    SectionHeader("PRO EXPOSURE & FOCUS")
                    spec.isoRange?.let {
                        SpecDetailRow("ISO Sensitivity Range", "ISO ${it.lower} .. ${it.upper}")
                    }
                    spec.exposureTimeRangeNs?.let {
                        val minSec = it.lower / 1_000_000_000.0
                        val maxSec = it.upper / 1_000_000_000.0
                        SpecDetailRow("Shutter Speed Range", "1/%.0fs .. %.2fs".format(Locale.US, 1.0 / minSec, maxSec))
                    }
                    spec.exposureCompensationRange?.let {
                        SpecDetailRow("Exposure Comp (EV)", "${it.lower} .. ${it.upper} (Step: %.2f EV)".format(Locale.US, spec.exposureCompensationStep))
                    }
                    SpecDetailRow("Autofocus Modes", spec.autoFocusModes.joinToString(", ").ifBlank { "Fixed Focus" })

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Capabilities & Features
                    SectionHeader("CAPABILITIES & STABILIZATION")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeatureBadge("Optical Stabilization (OIS)", spec.hasOis)
                        FeatureBadge("Video EIS", spec.hasEis)
                        FeatureBadge("Flash Unit", spec.hasFlash)
                        FeatureBadge("RAW Capture", spec.isRawSupported)
                        FeatureBadge("Manual Pro Control", spec.isManualSensorSupported)
                        FeatureBadge("10-Bit HDR", spec.is10BitHdrSupported)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Output Resolutions
                    SectionHeader("SUPPORTED RESOLUTIONS")
                    if (spec.photoResolutions.isNotEmpty()) {
                        SpecDetailRow("Photo (JPEG)", spec.photoResolutions.take(4).joinToString(", ") { "${it.width}x${it.height}" })
                    }
                    if (spec.rawResolutions.isNotEmpty()) {
                        SpecDetailRow("RAW DNG", spec.rawResolutions.take(2).joinToString(", ") { "${it.width}x${it.height}" })
                    }
                    if (spec.videoFormats.isNotEmpty()) {
                        SpecDetailRow("Video Formats", spec.videoFormats.take(3).joinToString(", ") { "${it.size.width}x${it.size.height}" })
                    }
                    if (spec.highSpeedVideoFormats.isNotEmpty()) {
                        SpecDetailRow("High-Speed Slow Mo", spec.highSpeedVideoFormats.joinToString(", ") { "${it.size.width}x${it.size.height} (${it.maxFps}fps)" })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFFFD600),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SpecDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FeatureBadge(label: String, isSupported: Boolean) {
    Surface(
        color = if (isSupported) Color(0xFF30D158).copy(alpha = 0.15f) else Color(0xFF282B34),
        shape = RoundedCornerShape(6.dp),
        border = if (isSupported) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30D158).copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSupported) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isSupported) Color(0xFF30D158) else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSupported) Color.White else Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Camera Hardware Specs", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Hardware report copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun shareSpecs(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Camera Specifications Report")
    context.startActivity(shareIntent)
}
