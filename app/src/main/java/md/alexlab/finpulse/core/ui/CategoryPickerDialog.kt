package md.alexlab.finpulse.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import md.alexlab.finpulse.R
import md.alexlab.finpulse.domain.model.Category
import md.alexlab.finpulse.domain.model.CategoryType

/**
 * Modern, responsive Category Picker Dialog.
 * Supports:
 * - Expense and Income type tabs
 * - Real-time search against localized category display names
 * - Frequently used categories section
 * - Visual grouping of parent categories with expandable subcategories
 * - Full Light / Dark theme compatibility
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    selectedCategoryId: String?,
    initialType: CategoryType = CategoryType.EXPENSE,
    allowedType: CategoryType? = null,
    frequentCategories: List<Category> = emptyList(),
    onCategorySelected: (Category) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(allowedType ?: initialType) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedParents = remember { mutableStateMapOf<String, Boolean>() }

    // Filter categories by selected tab
    val currentTypeCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Filter by search query against localized name
    val filteredCategories = remember(currentTypeCategories, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            currentTypeCategories
        } else {
            currentTypeCategories.filter { cat ->
                cat.getDisplayName(context).contains(query, ignoreCase = true)
            }
        }
    }

    // Grouping
    val parentCategories = remember(filteredCategories, searchQuery) {
        if (searchQuery.isNotBlank()) {
            emptyList()
        } else {
            filteredCategories.filter { it.parentCategoryId == null }
        }
    }

    val subcategoriesByParent = remember(currentTypeCategories) {
        currentTypeCategories.filter { it.parentCategoryId != null }
            .groupBy { it.parentCategoryId!! }
    }

    // Frequent categories for current type
    val relevantFrequent = remember(frequentCategories, selectedType) {
        frequentCategories.filter { it.type == selectedType }.take(6)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.quick_add_select_category),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Tabs (Expense / Income)
                if (allowedType == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CategoryType.entries.forEach { type ->
                            val isSelected = selectedType == type
                            val tabLabel = when (type) {
                                CategoryType.EXPENSE -> stringResource(R.string.category_type_expense)
                                CategoryType.INCOME -> stringResource(R.string.category_type_income)
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedType = type
                                        searchQuery = ""
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = tabLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.category_search_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Frequently Used Section (when not searching)
                    if (searchQuery.isBlank() && relevantFrequent.isNotEmpty()) {
                        item(key = "frequent_header") {
                            Text(
                                text = stringResource(R.string.category_frequently_used),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        item(key = "frequent_row") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                relevantFrequent.forEach { cat ->
                                    val isSelected = cat.id == selectedCategoryId
                                    Surface(
                                        modifier = Modifier.clickable {
                                            onCategorySelected(cat)
                                            onDismissRequest()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            Color(cat.colorHex).copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(1.5.dp, Color(cat.colorHex))
                                        } else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIconBadge(
                                                iconName = cat.icon,
                                                colorHex = cat.colorHex,
                                                size = 20.dp,
                                                iconSize = 12.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = cat.getDisplayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "frequent_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // 2. Search Mode (Flat list of matching categories)
                    if (searchQuery.isNotBlank()) {
                        if (filteredCategories.isEmpty()) {
                            item(key = "empty_state") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.category_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredCategories, key = { it.id }) { cat ->
                                CategoryRowItem(
                                    category = cat,
                                    isSelected = cat.id == selectedCategoryId,
                                    parentCategoryName = cat.parentCategoryId?.let { parentId ->
                                        categories.find { it.id == parentId }?.getDisplayName(context)
                                    },
                                    onClick = {
                                        onCategorySelected(cat)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    } else {
                        // 3. Normal Mode: Grouped by Parent Category
                        items(parentCategories, key = { it.id }) { parent ->
                            val subs = subcategoriesByParent[parent.id].orEmpty()
                            val isExpanded = expandedParents[parent.id] ?: true
                            val hasChildren = subs.isNotEmpty()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(vertical = 2.dp)
                            ) {
                                // Parent Row Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (hasChildren) {
                                                expandedParents[parent.id] = !isExpanded
                                            } else {
                                                onCategorySelected(parent)
                                                onDismissRequest()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIconBadge(
                                        iconName = parent.icon,
                                        colorHex = parent.colorHex,
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parent.getDisplayName(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (hasChildren) {
                                            Text(
                                                text = "${subs.size} subcategories",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (parent.id == selectedCategoryId) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (hasChildren) {
                                        IconButton(
                                            onClick = { expandedParents[parent.id] = !isExpanded },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Children Subcategories
                                AnimatedVisibility(
                                    visible = isExpanded && hasChildren,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 8.dp, bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        subs.forEach { sub ->
                                            CategoryRowItem(
                                                category = sub,
                                                isSelected = sub.id == selectedCategoryId,
                                                parentCategoryName = null,
                                                isSubcategory = true,
                                                onClick = {
                                                    onCategorySelected(sub)
                                                    onDismissRequest()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(
    category: Category,
    isSelected: Boolean,
    parentCategoryName: String?,
    isSubcategory: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            Color(category.colorHex).copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(category.colorHex).copy(alpha = 0.8f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSubcategory) 8.dp else 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = category.icon,
                colorHex = category.colorHex,
                size = if (isSubcategory) 26.dp else 30.dp,
                iconSize = if (isSubcategory) 14.dp else 16.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.getDisplayName(),
                    style = if (isSubcategory) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (parentCategoryName != null) {
                    Text(
                        text = parentCategoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(category.colorHex),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
