package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import com.example.ui.components.parseHexColor
import com.example.ui.theme.RoseRed

val PRESET_CATEGORY_COLORS = listOf(
    "#3B82F6", // Blue
    "#10B981", // Emerald
    "#8B5CF6", // Purple
    "#F59E0B", // Amber
    "#EF4444", // Rose
    "#06B6D4", // Cyan
    "#EC4899", // Pink
    "#64748B"  // Slate
)

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    brands: List<String> = emptyList(),
    locations: List<String> = emptyList(),
    onSaveCategory: (Category, String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddBrand: (String) -> Unit = {},
    onEditBrand: (String, String) -> Unit = { _, _ -> },
    onDeleteBrand: (String) -> Unit = {},
    onAddLocation: (String) -> Unit = {},
    onEditLocation: (String, String) -> Unit = { _, _ -> },
    onDeleteLocation: (String) -> Unit = {},
    onFilterByStockCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Categorias", "Marcas", "Locais")

    var showStringDialog by remember { mutableStateOf(false) }
    var stringToEdit by remember { mutableStateOf("") }
    var stringType by remember { mutableStateOf("") } // "Marca" or "Local"

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Info Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Configurações Globais",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Categorias, Marcas e Locais",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedTab == 0) {
                            Button(
                                onClick = {
                                    categoryToEdit = null
                                    showDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_add_category_header")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Nova", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (selectedTab == 1) {
                            Button(
                                onClick = { stringType = "Marca"; stringToEdit = ""; showStringDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Marca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { stringType = "Local"; stringToEdit = ""; showStringDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Local", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma categoria cadastrada",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Crie categorias para agrupar seus produtos de forma simples.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    categoryToEdit = null
                                    showDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Criar Primeira Categoria")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            CategoryItemCard(
                                category = cat,
                                onEdit = {
                                    categoryToEdit = cat
                                    showDialog = true
                                },
                                onDelete = { categoryToDelete = cat },
                                onSelectToFilter = { onFilterByStockCategory(cat.name) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            } else if (selectedTab == 1) {
                if (brands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma marca cadastrada",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cadastre marcas para agilizar o preenchimento de novos produtos.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    stringType = "Marca"
                                    stringToEdit = ""
                                    showStringDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cadastrar Primeira Marca")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(brands) { brand ->
                            StringListItem(
                                text = brand,
                                onEdit = { 
                                    stringType = "Marca"
                                    stringToEdit = brand
                                    showStringDialog = true 
                                },
                                onDelete = { onDeleteBrand(brand) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                if (locations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum local cadastrado",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cadastre locais (ex: Prateleira A, Depósito 1) para gerenciar seu espaço.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    stringType = "Local"
                                    stringToEdit = ""
                                    showStringDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cadastrar Primeiro Local")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(locations) { loc ->
                            StringListItem(
                                text = loc,
                                onEdit = {
                                    stringType = "Local"
                                    stringToEdit = loc
                                    showStringDialog = true
                                },
                                onDelete = { onDeleteLocation(loc) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) {
                    categoryToEdit = null
                    showDialog = true
                } else if (selectedTab == 1) {
                    stringType = "Marca"
                    stringToEdit = ""
                    showStringDialog = true
                } else {
                    stringType = "Local"
                    stringToEdit = ""
                    showStringDialog = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_category")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar")
        }
    }

    // Modal Add/Edit Category
    if (showDialog) {
        CategoryEditDialog(
            category = categoryToEdit,
            onDismiss = {
                showDialog = false
                categoryToEdit = null
            },
            onSave = { savedCat, oldName ->
                onSaveCategory(savedCat, oldName)
                showDialog = false
                categoryToEdit = null
            }
        )
    }

    // Modal Add/Edit Brand or Location
    if (showStringDialog) {
        StringEditDialog(
            initialText = stringToEdit,
            title = if (stringToEdit.isEmpty()) "Nova $stringType" else "Editar $stringType",
            onDismiss = { showStringDialog = false },
            onSave = { newString, oldString ->
                if (stringType == "Marca") {
                    if (oldString.isEmpty()) onAddBrand(newString) else onEditBrand(oldString, newString)
                } else {
                    if (oldString.isEmpty()) onAddLocation(newString) else onEditLocation(oldString, newString)
                }
                showStringDialog = false
            }
        )
    }

    // Delete Confirmation
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Excluir Categoria") },
            text = {
                Text(
                    "Deseja excluir a categoria '${cat.name}'? Os ${cat.productCount} produto(s) vinculados serão automaticamente transferidos para 'Geral'."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Excluir Categoria")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CategoryItemCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectToFilter: () -> Unit
) {
    val categoryColor = parseHexColor(category.colorHex)

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_card_${category.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectToFilter() }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = category.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.description.isNotBlank()) {
                        Text(
                            text = category.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${category.productCount} produto(s) vinculado(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Edit and Delete buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = RoseRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category, String) -> Unit
) {
    val isNew = category == null
    val originalName = category?.name ?: ""

    var name by rememberSaveable { mutableStateOf(category?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(category?.description ?: "") }
    var selectedColorHex by rememberSaveable { mutableStateOf(category?.colorHex ?: "#3B82F6") }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_category_edit")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "Nova Categoria" else "Editar Categoria",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Nome da Categoria *") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Informe o nome da categoria") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_category_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    placeholder = { Text("Ex: Alimentos secos, grãos...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Selection
                Text(
                    text = "Cor de Identificação",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PRESET_CATEGORY_COLORS) { colorHex ->
                        val color = parseHexColor(colorHex)
                        val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selecionada",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            val saved = (category ?: Category(name = name.trim())).copy(
                                name = name.trim(),
                                description = description.trim(),
                                colorHex = selectedColorHex
                            )
                            onSave(saved, originalName)
                        },
                        modifier = Modifier.testTag("btn_save_category")
                    ) {
                        Text(if (isNew) "Criar Categoria" else "Salvar")
                    }
                }
            }
        }
    }
}

@Composable
fun StringListItem(text: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RoseRed)
                }
            }
        }
    }
}

@Composable
fun StringEditDialog(
    initialText: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; isError = false },
                    label = { Text("Nome") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (text.isBlank()) isError = true else onSave(text, initialText)
                    }) { Text("Salvar") }
                }
            }
        }
    }
}
