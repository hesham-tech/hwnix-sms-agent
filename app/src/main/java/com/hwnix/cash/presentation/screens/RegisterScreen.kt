package com.hwnix.cash.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hwnix.cash.presentation.auth.register.RegisterUiState

/* تعليق عربي مختصر: شاشة تسجيل شركة سحابية جديدة وتجهيز حساب المدير والفرع الرئيسي تلقائياً بدون حقول عناوين زائدة */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onCompanyNameChange: (String) -> Unit,
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
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "تسجيل شركة سحابية جديدة",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "تجهيز خادم النظام والفرع الرئيسي والمخزن تلقائياً",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = state.companyName,
            onValueChange = onCompanyNameChange,
            label = { Text("اسم الشركة / المؤسسة") },
            leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = { Text("اسم صاحب الشركة / المدير") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text("رقم هاتف المدير المسؤول") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("البريد الإلكتروني (اختياري)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("كلمة المرور (8 أحرف على الأقل)") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword, enabled = !state.isLoading) {
                    Icon(
                        imageVector = if (state.passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            enabled = !state.isLoading
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = onRegisterClick,
            enabled = !state.isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(bottom = 8.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("تسجيل الشركة وتجهيز النظام 🚀", fontWeight = FontWeight.Bold)
            }
        }

        TextButton(
            onClick = onLoginToggleClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("لديك حساب شركة بالفعل؟ تسجيل الدخول")
        }
    }
}
