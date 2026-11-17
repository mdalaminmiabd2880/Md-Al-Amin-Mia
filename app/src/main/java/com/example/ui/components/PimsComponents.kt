package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.export.PdfAndExportEngine
import com.example.data.export.PdfFieldSelection
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.DiagnosticCheckStatus
import com.example.data.model.WebsiteSettings
import com.example.ui.theme.BengaliFontOption
import com.example.ui.theme.MonoIdFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopBar(
    settings: WebsiteSettings,
    lang: AppLang,
    darkTheme: Boolean,
    roleLabel: String?,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val logoBmp = remember(settings.logoBase64) {
        PdfAndExportEngine.decodeBase64Bitmap(settings.logoBase64)
    }
    val title = if (lang == AppLang.BN) settings.appNameBn else settings.appNameEn

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoBmp != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            bitmap = logoBmp.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.brand_logo),
                            contentDescription = title,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(2.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!roleLabel.isNullOrBlank()) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onToggleLang,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_language_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = AppStrings.pick(lang, "ভাষা পরিবর্তন", "Switch Language")
                )
            }
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_theme_button")
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = AppStrings.pick(lang, "থিম পরিবর্তন", "Switch Theme")
                )
            }
            if (onLogout != null) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = AppStrings.pick(lang, "লগআউট", "Logout")
                    )
                }
            }
        }
    )
}

@Composable
fun ProfileAvatar(
    photoBase64: String,
    contentDescription: String,
    sizeDp: Int = 56
) {
    val bmp: Bitmap? = remember(photoBase64) { PdfAndExportEngine.decodeBase64Bitmap(photoBase64) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_avatar_default),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun PersonalIdChip(personalId: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = personalId,
            fontFamily = MonoIdFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusNotificationBanner(
    banner: Pair<Boolean, String>?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = banner != null) {
        if (banner != null) {
            val (isError, msg) = banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchablePickerField(
    label: String,
    selectedLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    itemSubtitle: ((T) -> String)? = null,
    itemSearchKeywords: ((T) -> String)? = null,
    onSelect: (T) -> Unit,
    onClear: (() -> Unit)? = null,
    emptyMessage: String = "কোনো তথ্য পাওয়া যায়নি",
    enabled: Boolean = true,
    testTag: String = "searchable_picker"
) {
    var open by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onClear != null && selectedLabel.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) {
                    search = ""
                    open = true
                }
        )
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("খুঁজুন / Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                val filtered = remember(items, search) {
                    val q = search.trim()
                    if (q.isBlank()) items
                    else items.filter {
                        com.example.data.geo.BengaliTextSearch.matches(
                            q,
                            itemLabel(it),
                            itemSubtitle?.invoke(it).orEmpty(),
                            itemSearchKeywords?.invoke(it).orEmpty()
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(filtered) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(item)
                                        open = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(itemLabel(item), style = MaterialTheme.typography.bodyLarge)
                                val sub = itemSubtitle?.invoke(item)
                                if (!sub.isNullOrBlank()) {
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBarCard(
    title: String,
    entries: List<Pair<String, Int>>,
    emptyText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            if (entries.isEmpty()) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = entries.maxOf { it.second }.coerceAtLeast(1)
                entries.forEach { (label, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.42f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { count.toFloat() / maxVal.toFloat() },
                            modifier = Modifier
                                .weight(0.45f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.13f).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    lang: AppLang,
    mandatoryFirstLogin: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (current: String, newPass: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!mandatoryFirstLogin) onDismiss() },
        title = {
            Text(
                if (mandatoryFirstLogin)
                    AppStrings.pick(lang, "প্রথম লগইন: পাসওয়ার্ড পরিবর্তন করুন", "First Login: Change Password")
                else
                    AppStrings.pick(lang, "পাসওয়ার্ড পরিবর্তন", "Change Password")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mandatoryFirstLogin) {
                    Text(
                        AppStrings.pick(
                            lang,
                            "নিরাপত্তার জন্য অনুগ্রহ করে আপনার প্রাথমিক পাসওয়ার্ড পরিবর্তন করুন।",
                            "For security, please change your initial password before continuing."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text(AppStrings.pick(lang, "বর্তমান পাসওয়ার্ড", "Current Password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = next,
                    onValueChange = { next = it },
                    label = { Text(AppStrings.pick(lang, "নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)", "New Password (min 8 chars)")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(AppStrings.pick(lang, "পাসওয়ার্ড নিশ্চিত করুন", "Confirm Password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        current.isBlank() -> error = AppStrings.pick(lang, "বর্তমান পাসওয়ার্ড দিন", "Enter current password")
                        next.length < 8 -> error = AppStrings.pick(lang, "নতুন পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে", "Minimum 8 characters")
                        next != confirm -> error = AppStrings.pick(lang, "পাসওয়ার্ড মিলছে না", "Passwords do not match")
                        else -> onSubmit(current, next)
                    }
                },
                modifier = Modifier.testTag("confirm_change_password_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "পরিবর্তন করুন", "Update"))
            }
        },
        dismissButton = {
            if (!mandatoryFirstLogin) {
                TextButton(onClick = onDismiss) {
                    Text(AppStrings.pick(lang, "বাতিল", "Cancel"))
                }
            }
        }
    )
}

@Composable
fun PdfFieldSelectorDialog(
    lang: AppLang,
    onDismiss: () -> Unit,
    onConfirm: (PdfFieldSelection) -> Unit
) {
    var basic by remember { mutableStateOf(true) }
    var edu by remember { mutableStateOf(true) }
    var goals by remember { mutableStateOf(true) }
    var org by remember { mutableStateOf(true) }
    var addr by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.pick(lang, "PDF রিপোর্টের তথ্য নির্বাচন", "Select PDF Fields")) },
        text = {
            Column {
                FieldCheckboxRow(AppStrings.pick(lang, "ক. মৌলিক তথ্য", "A. Basic Info"), basic) { basic = it }
                FieldCheckboxRow(AppStrings.pick(lang, "খ. শিক্ষাগত তথ্য", "B. Educational Info"), edu) { edu = it }
                FieldCheckboxRow(AppStrings.pick(lang, "গ. ব্যক্তিগত লক্ষ্য", "C. Personal Goals"), goals) { goals = it }
                FieldCheckboxRow(AppStrings.pick(lang, "ঘ. সাংগঠনিক তথ্য", "D. Organizational Info"), org) { org = it }
                FieldCheckboxRow(AppStrings.pick(lang, "ঙ. ঠিকানা", "E. Address"), addr) { addr = it }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(PdfFieldSelection(basic, edu, goals, org, addr)) }) {
                Text(AppStrings.pick(lang, "PDF তৈরি করুন", "Generate PDF"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.pick(lang, "বাতিল", "Cancel"))
            }
        }
    )
}

@Composable
private fun FieldCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

data class SearchCategoryOption(
    val key: String,
    val label: String,
    val icon: ImageVector? = null
)

@Composable
fun ProfileSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    lang: AppLang,
    modifier: Modifier = Modifier,
    testTag: String = "profile_search_bar",
    onToggleFilters: (() -> Unit)? = null,
    filtersExpanded: Boolean = false,
    activeFilterCount: Int = 0,
    onSearchSubmit: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasActiveQuery = query.isNotBlank()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (hasActiveQuery || filtersExpanded) 1.5.dp else 1.dp,
            color = if (hasActiveQuery || filtersExpanded) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = AppStrings.pick(lang, "অনুসন্ধান", "Search"),
                        tint = if (hasActiveQuery) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(visible = hasActiveQuery) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${testTag}_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = AppStrings.pick(lang, "অনুসন্ধান মুছুন", "Clear search"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearchSubmit?.invoke()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )

            if (onToggleFilters != null) {
                Box(
                    modifier = Modifier.padding(end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onToggleFilters,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTag}_filter_toggle")
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(activeFilterCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = AppStrings.pick(lang, "উন্নত ফিল্টার", "Advanced Filters"),
                                tint = if (filtersExpanded || activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChipRow(
    categories: List<SearchCategoryOption>,
    selectedKey: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "category_chip"
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(categories, key = { it.key }) { option ->
            val isSelected = selectedKey == option.key
            FilterChip(
                selected = isSelected,
                onClick = {
                    val nextKey = if (isSelected && option.key.isNotEmpty()) "" else option.key
                    onSelectCategory(nextKey)
                },
                label = {
                    Text(
                        text = option.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = if (isSelected || option.icon != null) {
                    {
                        when {
                            isSelected -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                            option.icon != null -> Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_${option.key.ifBlank { "all" }}")
            )
        }
    }
}

fun profileDocumentCategoryOptions(lang: AppLang): List<SearchCategoryOption> = listOf(
    SearchCategoryOption("", AppStrings.pick(lang, "সকল", "All")),
    SearchCategoryOption("ROLE:ADMIN", AppStrings.roleLabel(lang, "ADMIN")),
    SearchCategoryOption("ROLE:SUPER_ADMIN", AppStrings.roleLabel(lang, "SUPER_ADMIN")),
    SearchCategoryOption("ROLE:EDITOR", AppStrings.roleLabel(lang, "EDITOR")),
    SearchCategoryOption("ROLE:VIEWER", AppStrings.roleLabel(lang, "VIEWER")),
    SearchCategoryOption("ROLE:NORMAL_USER", AppStrings.roleLabel(lang, "NORMAL_USER")),
    SearchCategoryOption("STATUS:ACTIVE", AppStrings.pick(lang, "সক্রিয়", "Active")),
    SearchCategoryOption("STATUS:INACTIVE", AppStrings.pick(lang, "নিষ্ক্রিয়", "Inactive")),
    SearchCategoryOption("INST:UNIVERSITY", AppStrings.institutionTypeLabel(lang, "UNIVERSITY")),
    SearchCategoryOption("INST:COLLEGE", AppStrings.institutionTypeLabel(lang, "COLLEGE")),
    SearchCategoryOption("INST:SCHOOL", AppStrings.institutionTypeLabel(lang, "SCHOOL")),
    SearchCategoryOption("INST:MADRASA", AppStrings.institutionTypeLabel(lang, "MADRASA")),
    SearchCategoryOption("INST:TECHNICAL", AppStrings.institutionTypeLabel(lang, "TECHNICAL"))
)

private enum class DiagnosticTone {
    OK,
    WARN,
    ERROR,
    NEUTRAL
}

@Composable
fun AuthPermissionDiagnosticCard(
    diagnostics: AuthPermissionDiagnostics,
    lang: AppLang,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "auth_diag"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "Auth ও Firestore Permission ডায়াগনস্টিকস",
                                "Auth & Firestore Permission Diagnostics"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "নিরাপদ Read-Only সেশন যাচাই (কোনো ডেটা পরিবর্তন বা Secret প্রদর্শন করে না)",
                                "Secure Read-Only runtime check (zero writes, no secrets/tokens)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !diagnostics.isLoading,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${testTagPrefix}_refresh_button")
                ) {
                    if (diagnostics.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = AppStrings.pick(lang, "পুনরায় পরীক্ষা করুন", "Refresh Diagnostics"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "READ-ONLY মোড: শুধু বর্তমান লগইন করা সেশনের লাইভ Firebase তথ্য প্রদর্শিত হচ্ছে।",
                            "READ-ONLY MODE: Showing live Firebase runtime status for your active signed-in session only."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            HorizontalDivider()

            // 1. Google Email
            DiagnosticKeyValueRow(
                indexLabel = "১",
                label = AppStrings.pick(lang, "Google Email", "Signed-in Google Email"),
                value = diagnostics.googleEmail.ifBlank {
                    AppStrings.pick(lang, "পাওয়া যায়নি", "Not available")
                },
                tone = if (diagnostics.googleEmail.isNotBlank()) DiagnosticTone.NEUTRAL else DiagnosticTone.WARN,
                testTag = "${testTagPrefix}_email"
            )

            // 2. Firebase User UID
            DiagnosticKeyValueRow(
                indexLabel = "২",
                label = AppStrings.pick(lang, "Firebase User UID", "Firebase User UID"),
                value = diagnostics.firebaseUid.ifBlank {
                    AppStrings.pick(lang, "সাইন-ইন করা নেই", "Not signed in")
                },
                monospace = true,
                tone = if (diagnostics.firebaseUid.isNotBlank()) DiagnosticTone.NEUTRAL else DiagnosticTone.WARN,
                testTag = "${testTagPrefix}_uid"
            )

            // 3. Email Verified
            DiagnosticKeyValueRow(
                indexLabel = "৩",
                label = AppStrings.pick(lang, "Email Verified", "Email Verified"),
                value = if (diagnostics.emailVerified) {
                    AppStrings.pick(lang, "Yes (যাচাইকৃত)", "Yes")
                } else {
                    AppStrings.pick(lang, "No (অযাচাইকৃত)", "No")
                },
                tone = if (diagnostics.emailVerified) DiagnosticTone.OK else DiagnosticTone.ERROR,
                testTag = "${testTagPrefix}_email_verified"
            )

            // 4. Firebase Auth Status
            val providerSuffix = diagnostics.authProviders
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
                ?.let { " ($it)" }
                ?: ""
            DiagnosticKeyValueRow(
                indexLabel = "৪",
                label = AppStrings.pick(lang, "Firebase Auth Status", "Firebase Authentication Status"),
                value = if (diagnostics.isFirebaseSignedIn) {
                    AppStrings.pick(lang, "Signed In / সফল$providerSuffix", "Signed In$providerSuffix")
                } else {
                    AppStrings.pick(lang, "Signed Out / লগইন নেই", "Not Signed In")
                },
                tone = if (diagnostics.isFirebaseSignedIn) DiagnosticTone.OK else DiagnosticTone.ERROR,
                testTag = "${testTagPrefix}_auth_status"
            )

            // 5. Bootstrapped Admin Email Match
            DiagnosticKeyValueRow(
                indexLabel = "৫",
                label = AppStrings.pick(lang, "Bootstrapped Admin Email ম্যাচ", "Bootstrapped Admin Email Match"),
                value = if (diagnostics.emailMatchesBootstrappedAdmin) {
                    AppStrings.pick(lang, "হ্যাঁ (মিলেছে / Matches)", "Yes (Matches)")
                } else {
                    AppStrings.pick(lang, "না (মিলছে না / Mismatch)", "No (Does not match)")
                },
                tone = if (diagnostics.emailMatchesBootstrappedAdmin) DiagnosticTone.OK else DiagnosticTone.WARN,
                testTag = "${testTagPrefix}_bootstrapped_match"
            )

            // 6. Current User Role
            DiagnosticKeyValueRow(
                indexLabel = "৬",
                label = AppStrings.pick(lang, "Current User Role", "Current User Role"),
                value = if (lang == AppLang.BN) diagnostics.effectiveUserRoleBn else diagnostics.effectiveUserRole,
                tone = if (diagnostics.adminPermissionGranted) DiagnosticTone.OK else DiagnosticTone.NEUTRAL,
                testTag = "${testTagPrefix}_role"
            )

            // 7. Firestore Admin Permission
            DiagnosticKeyValueRow(
                indexLabel = "৭",
                label = AppStrings.pick(lang, "Firestore Admin Permission", "Firestore Admin Permission"),
                value = if (diagnostics.adminPermissionGranted) {
                    AppStrings.pick(lang, "Granted (অনুমোদিত)", "Granted")
                } else {
                    AppStrings.pick(lang, "Denied (অনুমতি নেই)", "Denied")
                },
                tone = if (diagnostics.adminPermissionGranted) DiagnosticTone.OK else DiagnosticTone.ERROR,
                testTag = "${testTagPrefix}_admin_permission"
            )

            // 8. /admins/{uid} document presence
            val adminDocLabel = when (diagnostics.adminsDocExists) {
                null -> AppStrings.pick(lang, "পড়া যায়নি / অনুপলব্ধ", "Unable to verify")
                true -> {
                    val activeText = when (diagnostics.adminsDocActive) {
                        true -> "active=true"
                        false -> "active=false"
                        null -> "active=?"
                    }
                    val roleText = diagnostics.adminsDocRole.ifBlank { "?" }
                    AppStrings.pick(
                        lang,
                        "আছে ($roleText, $activeText)",
                        "Exists ($roleText, $activeText)"
                    )
                }
                false -> AppStrings.pick(lang, "নেই (Not Found)", "Not Found")
            }
            val adminDocTone = when {
                diagnostics.adminsDocExists == true && diagnostics.adminsDocActive == true -> DiagnosticTone.OK
                diagnostics.adminsDocExists == true -> DiagnosticTone.WARN
                diagnostics.emailMatchesBootstrappedAdmin -> DiagnosticTone.NEUTRAL
                else -> DiagnosticTone.WARN
            }
            DiagnosticKeyValueRow(
                indexLabel = "৮",
                label = AppStrings.pick(lang, "/admins/{uid} ডকুমেন্ট", "/admins/{uid} Document"),
                value = adminDocLabel,
                tone = adminDocTone,
                testTag = "${testTagPrefix}_admins_doc"
            )

            // 9. ensureWorkspaceBootstrapped() Status
            val (bsValue, bsTone) = when (diagnostics.bootstrapStatus) {
                DiagnosticCheckStatus.CHECKING ->
                    AppStrings.pick(lang, "যাচাই চলছে...", "Checking...") to DiagnosticTone.NEUTRAL
                DiagnosticCheckStatus.SUCCESS ->
                    AppStrings.pick(lang, "Success (সফল)", "Success") to DiagnosticTone.OK
                DiagnosticCheckStatus.FAILED ->
                    AppStrings.pick(lang, "Failed (ব্যর্থ)", "Failed") to DiagnosticTone.ERROR
            }
            DiagnosticKeyValueRow(
                indexLabel = "৯",
                label = "ensureWorkspaceBootstrapped()",
                value = bsValue,
                tone = bsTone,
                testTag = "${testTagPrefix}_bootstrap_status"
            )

            // 10. Last Firestore Error Code & Brief Reason
            HorizontalDivider()
            val hasErrorHighlight = diagnostics.lastFirestoreErrorCode.isNotBlank() &&
                diagnostics.lastFirestoreErrorCode != "NONE"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasErrorHighlight) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_last_error")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasErrorHighlight) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "১০. সর্বশেষ Firestore Error Code: ${diagnostics.lastFirestoreErrorCode.ifBlank { "NONE" }}",
                                "10. Last Firestore Error Code: ${diagnostics.lastFirestoreErrorCode.ifBlank { "NONE" }}"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = MonoIdFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (hasErrorHighlight) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    val reasonText = if (lang == AppLang.BN) {
                        diagnostics.lastFirestoreErrorReasonBn
                    } else {
                        diagnostics.lastFirestoreErrorReasonEn
                    }
                    if (reasonText.isNotBlank()) {
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasErrorHighlight) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    val bsDetail = if (lang == AppLang.BN) diagnostics.bootstrapDetailBn else diagnostics.bootstrapDetailEn
                    if (bsDetail.isNotBlank() && bsDetail != reasonText) {
                        Text(
                            text = AppStrings.pick(lang, "Bootstrap তথ্য: $bsDetail", "Bootstrap info: $bsDetail"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticKeyValueRow(
    indexLabel: String,
    label: String,
    value: String,
    tone: DiagnosticTone,
    monospace: Boolean = false,
    testTag: String
) {
    val badgeBg = when (tone) {
        DiagnosticTone.OK -> MaterialTheme.colorScheme.primaryContainer
        DiagnosticTone.WARN -> MaterialTheme.colorScheme.secondaryContainer
        DiagnosticTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        DiagnosticTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeFg = when (tone) {
        DiagnosticTone.OK -> MaterialTheme.colorScheme.onPrimaryContainer
        DiagnosticTone.WARN -> MaterialTheme.colorScheme.onSecondaryContainer
        DiagnosticTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        DiagnosticTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$indexLabel. $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(0.46f)
                .padding(end = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeBg,
            modifier = Modifier.weight(0.54f)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = if (monospace) MonoIdFontFamily else MaterialTheme.typography.labelMedium.fontFamily,
                fontWeight = FontWeight.SemiBold,
                color = badgeFg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BengaliFontSelectorCard(
    selectedFont: BengaliFontOption,
    lang: AppLang,
    onSelectFont: (BengaliFontOption) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "bengali_font"
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Filled.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = AppStrings.pick(lang, "বাংলা ফন্ট স্টাইল", "Bengali Typography"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "অ্যাপের ইউআই ও PDF রিপোর্টের জন্য পছন্দসই বাংলা ফন্ট বেছে নিন",
                            "Select preferred Bengali font for UI & PDF reports"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BengaliFontOption.entries.forEach { option ->
                    val isSelected = option == selectedFont
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFont(option) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${testTagPrefix}_${option.name.lowercase()}"),
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = AppStrings.pick(lang, option.labelBn, option.labelEn),
                                fontFamily = option.fontFamily(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_preview")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "সক্রিয় ফন্ট: ${selectedFont.labelBn} (${selectedFont.descriptionBn})",
                            "Active Font: ${selectedFont.labelEn}"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = selectedFont.fontFamily(),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আমার সোনার বাংলা — সুন্দরগঞ্জ উপজেলা, গাইবান্ধা (১২৩৪৫৬৭৮৯০)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = selectedFont.fontFamily(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

