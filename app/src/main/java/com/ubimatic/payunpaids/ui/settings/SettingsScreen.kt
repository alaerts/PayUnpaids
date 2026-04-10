package com.ubimatic.payunpaids.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ubimatic.payunpaids.ui.theme.Amber
import com.ubimatic.payunpaids.ui.theme.AmberDark
import com.ubimatic.payunpaids.ui.theme.DarkBorder
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.Green
import com.ubimatic.payunpaids.ui.theme.TextMuted
import com.ubimatic.payunpaids.ui.theme.TextPrimary
import com.ubimatic.payunpaids.ui.theme.TextSecondary

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
            containerColor = DarkSurface,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings", color = TextPrimary)
                        Text(
                            "First-time setup",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            FieldLabel("Odoo URL")
            FieldWithHelp(
                value = state.url,
                onValueChange = viewModel::updateUrl,
                placeholder = "https://mycompany.odoo.com",
                helpKey = "url",
                onHelp = { helpDialog = it },
            )

            FieldLabel("Username")
            FieldWithHelp(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                placeholder = "user@company.com",
                helpKey = "username",
                onHelp = { helpDialog = it },
            )

            FieldLabel("Password / API Key")
            FieldWithHelp(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKey,
                placeholder = "",
                helpKey = "password",
                onHelp = { helpDialog = it },
                isPassword = true,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Test Connection (right after Odoo parameters)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    enabled = !state.isTesting,
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp,
                            color = Amber,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Connection test result
            state.testResult?.let { result ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\u2022",
                        color = if (state.testSuccess) Green else MaterialTheme.colorScheme.error,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = result,
                        fontSize = 12.sp,
                        color = if (state.testSuccess) Green else MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Banking app selector (further down)
            FieldLabel("Banking App")
            Text(
                text = "Choose your preferred Belgian banking app",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            Column(modifier = Modifier.padding(top = 4.dp)) {
                state.banks.forEach { option ->
                    val bank = option.bank
                    val isSelected = bank == state.selectedBank
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                if (isSelected) Amber.copy(alpha = 0.15f) else DarkSurface,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { viewModel.selectBank(bank) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectBank(bank) },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = Amber,
                                unselectedColor = TextSecondary,
                            ),
                        )
                        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                            Text(
                                text = bank.displayName,
                                color = if (isSelected) Amber else TextPrimary,
                                fontSize = 14.sp,
                            )
                            if (!option.installed) {
                                Text(
                                    text = "Not installed",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        if (option.installed) {
                            Text(
                                text = "\u2713",
                                fontSize = 14.sp,
                                color = Green,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button (amber, full width)
            Button(
                onClick = {
                    viewModel.save()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = AmberDark,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Save & continue",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontSize = 11.sp,
        color = TextPrimary,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun FieldWithHelp(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    helpKey: String,
    onHelp: (String) -> Unit,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            IconButton(onClick = { onHelp(helpKey) }) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = TextMuted,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Amber,
            unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = Amber,
        ),
        shape = RoundedCornerShape(8.dp),
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
        "Password / API Key" to
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
