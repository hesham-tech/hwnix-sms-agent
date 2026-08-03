package com.hwnix.cash.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwnix.cash.core.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CompanyItem(val id: Long, val name: String)

class CompanySelectionViewModel : ViewModel() {
    private val sessionManager = ServiceLocator.sessionManager
    private val apiService = ServiceLocator.apiService

    private val _companies = MutableStateFlow<List<CompanyItem>>(emptyList())
    val companies: StateFlow<List<CompanyItem>> = _companies

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedCompanyId = MutableStateFlow<Long?>(sessionManager.getCompanyId().takeIf { it != -1L })
    val selectedCompanyId: StateFlow<Long?> = _selectedCompanyId

    init {
        loadCompanies()
    }

    private fun loadCompanies() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getCompanies()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val data = body.getAsJsonArray("data")
                    val list = mutableListOf<CompanyItem>()
                    for (i in 0 until data.size()) {
                        val obj = data.get(i).asJsonObject
                        list.add(CompanyItem(id = obj.get("id").asLong, name = obj.get("name").asString))
                    }
                    _companies.value = list
                    
                    // Auto select first if none selected
                    if (sessionManager.getCompanyId() == -1L && list.isNotEmpty()) {
                        selectCompany(list.first().id)
                    }
                } else {
                    _error.value = "Failed to load companies"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCompany(companyId: Long) {
        val oldId = sessionManager.getCompanyId()
        if (oldId != companyId) {
            // Clear old cache and reload settings
            sessionManager.saveCompanyId(companyId)
            _selectedCompanyId.value = companyId
            // TODO: Triggers clear cache and reload settings globally
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySelectionScreen(
    viewModel: CompanySelectionViewModel = viewModel(),
    onNavigateNext: () -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedId by viewModel.selectedCompanyId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("اختيار الشركة") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(companies) { company ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.selectCompany(company.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedId == company.id) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = company.name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateNext,
                    enabled = selectedId != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("متابعة")
                }
            }
        }
    }
}
