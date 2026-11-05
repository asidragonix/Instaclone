package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.InstaCloneRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AdaptiveMainScaffold
import com.example.ui.theme.InstaCloneTheme
import com.example.ui.viewmodel.InstaCloneViewModel
import com.example.util.ThemeMode
import com.example.util.ThemePreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themePrefs = remember(context) { ThemePreferences(context.applicationContext) }
            val themeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            val scope = rememberCoroutineScope()

            InstaCloneTheme(darkTheme = useDarkTheme) {
                InstaCloneAppRoot(
                    isDarkTheme = useDarkTheme,
                    onToggleTheme = {
                        scope.launch {
                            themePrefs.toggleTheme(useDarkTheme)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InstaCloneAppRoot(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        // Strict Auth Gate: Feature ViewModels and Firestore listeners are NOT instantiated while unauthenticated
        AuthScreen(onAuthSuccess = {})
    } else {
        AuthenticatedAppSession(
            user = user,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )
    }
}

@Composable
private fun AuthenticatedAppSession(
    user: FirebaseUser,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: InstaCloneViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                InstaCloneViewModel(
                    repository = InstaCloneRepository(db),
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName,
                    initialEmailPrefix = user.email?.substringBefore('@'),
                    initialPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    AdaptiveMainScaffold(
        viewModel = viewModel,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onSignOut = {
            signOutUser(
                credentialManager = credentialManager,
                onSignOutComplete = {},
                scope = scope
            )
        }
    )
}
