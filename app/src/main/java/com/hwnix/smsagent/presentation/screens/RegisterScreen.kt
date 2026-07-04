package com.hwnix.smsagent.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hwnix.smsagent.presentation.auth.register.RegisterUiState

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onServerUrlChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginToggleClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "إنشاء حساب بوابة جديد",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("رابط السيرفر (Server API URL)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = { Text("الاسم الكامل (المشرف)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.nickname,
            onValueChange = onNicknameChange,
            label = { Text("اسم الشهرة / اللقب") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text("رقم الهاتف") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("البريد الإلكتروني (اختياري)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("كلمة المرور") },
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword, enabled = !state.isLoading) {
                    Icon(
                        imageVector = if (state.passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !state.isLoading
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("إنشاء الحساب والربط")
            }
        }

        TextButton(
            onClick = onLoginToggleClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لديك حساب بالفعل؟ تسجيل الدخول")
        }
    }
}
