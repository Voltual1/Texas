package com.texas.pyrolysis.ui.settings.signin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.texas.pyrolysis.ui.theme.BBQButton
import com.texas.pyrolysis.ui.theme.BBQOutlinedButton
import com.texas.pyrolysis.ui.theme.SwitchWithText
import com.texas.pyrolysis.ui.theme.BBQSnackbarHost
import com.texas.pyrolysis.ui.theme.BBQSuccessSnackbar
import com.texas.pyrolysis.ui.theme.BBQErrorSnackbar
import com.texas.pyrolysis.ui.theme.BBQInfoSnackbar
import kotlinx.coroutines.launch

@Composable
fun SignInSettingsScreen(
    viewModel: SignInSettingsViewModel = viewModel(),
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val autoSignIn by viewModel.autoSignIn.collectAsState(initial = false)
    val signInState by viewModel.signInState.collectAsState()
    val context = LocalContext.current
    
    // 监听签到状态变化，显示Snackbar
    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is SignInState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            is SignInState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            is SignInState.Info -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            else -> {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 自动签到开关
            SwitchWithText(
                text = "开启自动签到",
                checked = autoSignIn,
                onCheckedChange = { checked ->
                    scope.launch {
                        viewModel.setAutoSignIn(checked)
                        // 如果开启自动签到，立即执行一次签到
                        if (checked) {
                            viewModel.signIn(context)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 签到按钮
                BBQButton(
                    onClick = {
                        viewModel.signIn(context)
                    },
                    enabled = signInState !is SignInState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    text = {
                        if (signInState is SignInState.Loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "立即签到",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                )
            }
        }
        
        // 显示Snackbar
        BBQSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}