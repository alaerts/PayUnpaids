package com.ubimatic.payunpaids.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var helpDialog by remember { mutableStateOf<String?>(null) }

    helpDialog?.let { topic ->
        AlertDialog(
            onDismissRequest = { helpDialog = null },
            title = { Text(helpTopics[topic]?.first ?: "") },
            text = { Text(helpTopics[topic]?.second ?: "") },
            confirmButton = {
                TextButton(onClick = { helpDialog = null }) { Text("OK") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            FieldWithHelp(
                value = state.url,
                onValueChange = viewModel::updateUrl,
                label = "Odoo URL",
                placeholder = "https://mycompany.odoo.com",
                helpKey = "url",
                onHelp = { helpDialog = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            FieldWithHelp(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = "Username (email)",
                placeholder = "user@example.com",
                helpKey = "username",
                onHelp = { helpDialog = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            FieldWithHelp(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKey,
                label = "Password",
                placeholder = "",
                helpKey = "password",
                onHelp = { helpDialog = it },
                isPassword = true,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    enabled = !state.isTesting,
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(18.dp)
                                .width(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    viewModel.save()
                    onBack()
                }) {
                    Text("Save")
                }
            }

            state.testResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = result,
                    color = if (state.testSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun FieldWithHelp(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    helpKey: String,
    onHelp: (String) -> Unit,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = {
            IconButton(onClick = { onHelp(helpKey) }) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

private val helpTopics = mapOf(
    "url" to (
        "Odoo URL" to
        "Enter the full URL of your Odoo instance.\n\n" +
        "Example: https://mycompany.odoo.com\n\n" +
        "The database name is automatically extracted from the subdomain (the part before .odoo.com).\n\n" +
        "Do not include /web or any path after the domain."
    ),
    "username" to (
        "Username" to
        "Enter the login email of your Odoo user account.\n\n" +
        "This is the email shown in Odoo under:\nSettings \u2192 Users & Companies \u2192 Users \u2192 [your user]\n\n" +
        "It may differ from your personal email address."
    ),
    "password" to (
        "Password" to
        "For Odoo Online instances (*.odoo.com), you need to set a local password:\n\n" +
        "1. Log into Odoo as an administrator\n" +
        "2. Go to Settings \u2192 Users & Companies \u2192 Users\n" +
        "3. Click on the user you want to use\n" +
        "4. Click Action \u2192 Change Password\n" +
        "5. Enter a new password and click Change Password\n\n" +
        "Use that password here.\n\n" +
        "Alternatively, you can use an API key:\n" +
        "1. Log into Odoo\n" +
        "2. Click your avatar (top right) \u2192 My Profile\n" +
        "3. Go to the Account Security tab\n" +
        "4. Click New API Key\n" +
        "5. Enter a description (e.g. \"PayUnpaids\")\n" +
        "6. Copy the generated key and paste it here"
    ),
)
