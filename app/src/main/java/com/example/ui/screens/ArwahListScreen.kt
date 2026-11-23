package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.example.model.DeceasedPrayer
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.example.model.UiState

@Composable
fun ArwahListScreen(
  deceasedState: UiState<List<DeceasedPrayer>>,
  onAddDeceased: (
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    onComplete: (Boolean) -> Unit,
  ) -> Unit,
  onUpdateDeceased: (
    entryId: String,
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean,
    onComplete: (Boolean) -> Unit,
  ) -> Unit,
  onToggleIncluded: (DeceasedPrayer) -> Unit,
  onDeleteDeceased: (String) -> Unit,
  cloudAccount: String,
  cloudStatus: String,
  onGoogleSignIn: (String) -> Unit,
  onSyncCloud: () -> Unit,
  onSignOutCloud: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var showFormSheet by remember { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<DeceasedPrayer?>(null) }
  var entryToDelete by remember { mutableStateOf<DeceasedPrayer?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        DeceasedPrayerCloudCard(
          account = cloudAccount,
          status = cloudStatus,
          onGoogleSignIn = onGoogleSignIn,
          onSync = onSyncCloud,
          onSignOut = onSignOutCloud,
        )
        Text(
          text = "Daftar Nama Arwah",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "Nama yang aktif akan otomatis ditampilkan di bagian Tawasul & Doa Arwah.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari nama Almarhum / Almarhumah...") },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
          },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_arwah_input"),
        )
      }

      when (deceasedState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
        is UiState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = deceasedState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
            )
          }
        }
        is UiState.Success -> {
          val filtered = remember(deceasedState.data, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
              deceasedState.data
            } else {
              deceasedState.data.filter {
                it.fullName.lowercase().contains(q) ||
                  it.parentName.lowercase().contains(q) ||
                  (it.relationNote?.lowercase()?.contains(q) == true)
              }
            }
          }

          if (filtered.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = MaterialTheme.shapes.large,
                  modifier = Modifier.size(64.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.PeopleOutline,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.size(32.dp),
                    )
                  }
                }
                Text(
                  text = if (searchQuery.isBlank()) {
                    "Belum Ada Nama Arwah Tersimpan"
                  } else {
                    "Nama tidak ditemukan"
                  },
                  style = MaterialTheme.typography.titleMedium,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = if (searchQuery.isBlank()) {
                    "Simpan nama orang tua atau keluarga tercinta yang telah berpulang agar selalu tersertakan saat Yasinan & Tahlilan."
                  } else {
                    "Coba kata kunci lain atau tambahkan nama baru."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (searchQuery.isBlank()) {
                  Button(
                    onClick = {
                      editingEntry = null
                      showFormSheet = true
                    },
                    modifier = Modifier.testTag("empty_add_arwah_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Add,
                      contentDescription = null,
                      modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                    )
                    Text("Tambah Nama Arwah")
                  }
                }
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 88.dp,
              ),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(filtered, key = { it.id }) { person ->
                ArwahItemCard(
                  person = person,
                  onToggleIncluded = { onToggleIncluded(person) },
                  onEdit = {
                    editingEntry = person
                    showFormSheet = true
                  },
                  onDelete = { entryToDelete = person },
                )
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = {
        editingEntry = null
        showFormSheet = true
      },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .testTag("add_arwah_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = "Tambah nama Almarhum atau Almarhumah",
      )
    }
  }

  if (showFormSheet) {
    ArwahFormModalSheet(
      initialEntry = editingEntry,
      onDismiss = { showFormSheet = false },
      onSave = { honorific, fullName, parentPrefix, parentName, relationNote, finish ->
        val existing = editingEntry
        if (existing == null) {
          onAddDeceased(honorific, fullName, parentPrefix, parentName, relationNote, finish)
        } else {
          onUpdateDeceased(
            existing.id,
            honorific,
            fullName,
            parentPrefix,
            parentName,
            relationNote,
            existing.includedInPrayer,
            finish,
          )
        }
      },
    )
  }

  val pendingDelete = entryToDelete
  if (pendingDelete != null) {
    AlertDialog(
      onDismissRequest = { entryToDelete = null },
      title = { Text("Hapus Nama Arwah?") },
      text = {
        Text("Hapus \"${pendingDelete.formattedPrayerName()}\" dari daftar doa Anda?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteDeceased(pendingDelete.id)
            entryToDelete = null
          }
        ) {
          Text("Hapus", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { entryToDelete = null }) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun DeceasedPrayerCloudCard(
  account: String,
  status: String,
  onGoogleSignIn: (String) -> Unit,
  onSync: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var localStatus by remember { mutableStateOf("") }
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text("Sinkronisasi antarperangkat", style = MaterialTheme.typography.titleSmall)
      Text(
        text = if (account.isBlank()) {
          "Masuk dengan Google untuk menyimpan daftar arwah di akun Anda dan membukanya di Android maupun web."
        } else {
          "Terhubung sebagai $account"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      if (account.isBlank()) {
        Button(
          onClick = {
            scope.launch {
              try {
                if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank() || BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith("your-")) {
                  error("Atur GOOGLE_WEB_CLIENT_ID di konfigurasi aplikasi terlebih dahulu.")
                }
                val option = GetGoogleIdOption.Builder()
                  .setFilterByAuthorizedAccounts(false)
                  .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                  .build()
                val request = GetCredentialRequest.Builder()
                  .addCredentialOption(option)
                  .build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                onGoogleSignIn(credential.idToken)
                localStatus = ""
              } catch (error: Exception) {
                localStatus = error.localizedMessage ?: "Tidak dapat masuk dengan Google."
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Masuk dengan Google")
        }
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = onSync, modifier = Modifier.weight(1f)) { Text("Sinkronkan") }
          TextButton(onClick = onSignOut) { Text("Keluar") }
        }
      }
      (localStatus.ifBlank { status }).takeIf { it.isNotBlank() }?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
      }
    }
  }
}

@Composable
private fun ArwahItemCard(
  person: DeceasedPrayer,
  onToggleIncluded: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(
      width = if (person.includedInPrayer) 1.4.dp else 1.dp,
      color = if (person.includedInPrayer) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
      },
    ),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = person.formattedPrayerName(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!person.relationNote.isNullOrBlank()) {
            Text(
              text = person.relationNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Switch(
          checked = person.includedInPrayer,
          onCheckedChange = { onToggleIncluded() },
          modifier = Modifier.testTag("toggle_arwah_${person.id}"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (person.includedInPrayer) {
            "Disertakan di Doa Arwah"
          } else {
            "Disembunyikan sementara"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (person.includedInPrayer) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )

        Row {
          IconButton(
            onClick = onEdit,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("edit_arwah_${person.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Ubah ${person.fullName}",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("delete_arwah_${person.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "Hapus ${person.fullName}",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArwahFormModalSheet(
  initialEntry: DeceasedPrayer?,
  onDismiss: () -> Unit,
  onSave: (
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    onComplete: (Boolean) -> Unit,
  ) -> Unit,
) {
  var honorific by remember { mutableStateOf(initialEntry?.honorific ?: "Almarhum") }
  var fullName by remember { mutableStateOf(initialEntry?.fullName ?: "") }
  var parentPrefix by remember { mutableStateOf(initialEntry?.parentPrefix ?: "bin") }
  var parentName by remember { mutableStateOf(initialEntry?.parentName ?: "") }
  var relationNote by remember { mutableStateOf(initialEntry?.relationNote ?: "") }
  var nameError by remember { mutableStateOf<String?>(null) }
  var isSaving by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = if (initialEntry == null) "Tambah Nama Arwah" else "Ubah Nama Arwah",
        style = MaterialTheme.typography.headlineSmall,
      )

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = honorific == "Almarhum",
          onClick = {
            honorific = "Almarhum"
            parentPrefix = "bin"
          },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
          Text("Almarhum (Bin)")
        }
        SegmentedButton(
          selected = honorific == "Almarhumah",
          onClick = {
            honorific = "Almarhumah"
            parentPrefix = "binti"
          },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
          Text("Almarhumah (Binti)")
        }
      }

      OutlinedTextField(
        value = fullName,
        onValueChange = {
          if (it.length <= 80) {
            fullName = it
            nameError = null
          }
        },
        label = { Text("Nama Lengkap*") },
        placeholder = { Text("Contoh: H. Ahmad Sudirman") },
        isError = nameError != null,
        supportingText = nameError?.let { msg -> { Text(msg) } },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("arwah_fullname_input"),
      )

      OutlinedTextField(
        value = parentName,
        onValueChange = { if (it.length <= 80) parentName = it },
        label = { Text("Nama Ayah ($parentPrefix) — Opsional") },
        placeholder = { Text("Contoh: Abdullah") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("arwah_parentname_input"),
      )

      OutlinedTextField(
        value = relationNote,
        onValueChange = { if (it.length <= 100) relationNote = it },
        label = { Text("Catatan / Hubungan — Opsional") },
        placeholder = { Text("Contoh: Ayahanda, Kakek, Haul 12 Rajab") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("arwah_note_input"),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          val trimmedName = fullName.trim()
          if (trimmedName.isEmpty()) {
            nameError = "Nama lengkap wajib diisi."
            return@Button
          }
          isSaving = true
          onSave(
            honorific,
            trimmedName,
            parentPrefix,
            parentName.trim(),
            relationNote.trim().ifEmpty { null },
          ) { ok ->
            isSaving = false
            if (ok) onDismiss()
          }
        },
        enabled = !isSaving,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_arwah_button"),
      ) {
        Text(if (isSaving) "Menyimpan..." else "Simpan Nama Arwah")
      }
    }
  }
}
