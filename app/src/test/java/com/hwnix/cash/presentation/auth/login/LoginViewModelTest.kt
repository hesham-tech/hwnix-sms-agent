package com.hwnix.cash.presentation.auth.login

import com.hwnix.cash.data.local.SessionManager
import com.hwnix.cash.domain.usecase.LoginUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
/* تعليق عربي مختصر: كلاس اختبارات الوحدة المحسن لـ LoginViewModel لتغطية كافة الأفرع والفرع الشرطية والنجاح والفشل */
class LoginViewModelTest {

    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockLoginUseCase: LoginUseCase
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockSessionManager = mockk(relaxed = true)
        every { mockSessionManager.getBaseUrl() } returns "https://sms.hwnix.com/api/"
        mockLoginUseCase = mockk(relaxed = true)
        viewModel = LoginViewModel(mockSessionManager, mockLoginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onServerUrlChange_updatesUiStateAndClearsError() {
        viewModel.onServerUrlChange("https://new.hwnix.com")

        assertEquals("https://new.hwnix.com", viewModel.uiState.value.serverUrl)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onLoginInputChange_updatesUiState() {
        viewModel.onLoginInputChange("admin@hwnix.com")

        assertEquals("admin@hwnix.com", viewModel.uiState.value.loginInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onPasswordInputChange_updatesUiState() {
        viewModel.onPasswordInputChange("secret123")

        assertEquals("secret123", viewModel.uiState.value.passwordInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun togglePasswordVisibility_flipsBooleanState() {
        assertFalse(viewModel.uiState.value.passwordVisible)

        viewModel.togglePasswordVisibility()

        assertTrue(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun login_whenServerUrlIsBlank_showsValidationErrorMessage() {
        viewModel.onServerUrlChange("   ")
        viewModel.onLoginInputChange("admin")
        viewModel.onPasswordInputChange("pass")

        viewModel.login()

        assertEquals("يرجى ملء جميع الحقول المطلوبة.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_whenLoginInputIsBlank_showsValidationErrorMessage() {
        viewModel.onServerUrlChange("https://sms.hwnix.com")
        viewModel.onLoginInputChange("   ")
        viewModel.onPasswordInputChange("pass")

        viewModel.login()

        assertEquals("يرجى ملء جميع الحقول المطلوبة.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_whenPasswordInputIsBlank_showsValidationErrorMessage() {
        viewModel.onServerUrlChange("https://sms.hwnix.com")
        viewModel.onLoginInputChange("admin")
        viewModel.onPasswordInputChange("   ")

        viewModel.login()

        assertEquals("يرجى ملء جميع الحقول المطلوبة.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_whenCredentialsValidAndApiSucceeds_updatesStateToSuccess() = runTest {
        viewModel.onServerUrlChange("https://sms.hwnix.com")
        viewModel.onLoginInputChange("admin@hwnix.com")
        viewModel.onPasswordInputChange("password123")

        coEvery { mockLoginUseCase.execute("admin@hwnix.com", "password123") } returns Result.success(Unit)

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_whenApiFails_updatesStateWithErrorMessage() = runTest {
        viewModel.onServerUrlChange("https://sms.hwnix.com")
        viewModel.onLoginInputChange("admin@hwnix.com")
        viewModel.onPasswordInputChange("wrongpass")

        coEvery { mockLoginUseCase.execute("admin@hwnix.com", "wrongpass") } returns Result.failure(Exception("بيانات الاعتماد غير صحيحة"))

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("بيانات الاعتماد غير صحيحة", viewModel.uiState.value.errorMessage)
    }
}
