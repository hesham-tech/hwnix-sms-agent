package com.hwnix.cash.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingExplanationScreen(
    isFromMenu: Boolean,
    onStartSetup: () -> Unit,
    onPermissionNeeded: () -> Unit,
    onClose: () -> Unit
) {
    var showPreCheck by remember { mutableStateOf(false) }

    if (showPreCheck) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        LaunchedEffect(Unit) {
            if (hasPermission) {
                onStartSetup()
            } else {
                onPermissionNeeded()
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator
                    Row(
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                            val width = if (pagerState.currentPage == iteration) 24.dp else 8.dp
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(width = width, height = 8.dp)
                            )
                        }
                    }

                    if (!isFromMenu) {
                        if (pagerState.currentPage < 2) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("التالي", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { showPreCheck = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("ابدأ ربط محافظي 🚀", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إغلاق", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> SlideContent(
                    lottieUrl = "https://lottie.host/80dc1d9e-1d54-47fb-89c0-67bc10e9f454/W2nE599rT4.json",
                    title = "عينك الساهرة على أموالك \uD83D\uDC41\uFE0F",
                    description = "يعمل هذا التطبيق بصمت في الخلفية.. يقرأ رسائل الدفع الخاصة بك، ويرسلها فوراً إلى لوحة تحكم الويب لتتمكن من إدارتها بكل سهولة."
                )
                1 -> SlideContent(
                    lottieUrl = "https://lottie.host/549c7fb4-0d35-43a7-b080-60b6b27e8574/hF8bBXXhR1.json",
                    title = "كل شريحة.. محفظة مستقلة \uD83D\uDCF1\uD83D\uDC5B",
                    description = "لنتمكن من تتبع أرصدتك وحدودك اليومية بدقة، سنقوم الآن بربط كل خط هاتف في جهازك بمحفظة مخصصة له."
                )
                2 -> SlideContent(
                    lottieUrl = "https://lottie.host/7dfbc85a-8b15-4ba8-a7be-e283c79dc7c8/Gj2E70bB22.json",
                    title = "أعدّه لمرة واحدة، وانسَ أمره! \u23F1\uFE0F",
                    description = "عملية الإعداد ستستغرق أقل من دقيقة. بمجرد الانتهاء، سيعمل النظام تلقائياً ولن تضطر لتكرار هذه الخطوات أو تغيير هذه الإعدادات مرة أخرى."
                )
            }
        }
    }
}

@Composable
private fun SlideContent(lottieUrl: String, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.Url(lottieUrl))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            if (composition == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}
