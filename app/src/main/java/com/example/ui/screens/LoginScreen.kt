package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StockViewModel
import com.example.ui.theme.*

enum class AuthState {
    LOGIN, SIGNUP, RECOVERY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: StockViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var state by remember { mutableStateOf(AuthState.LOGIN) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var recoveryStep by remember { mutableStateOf(1) }
    var generatedCode by remember { mutableStateOf("") }
    var enteredCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // Pulsing glow transition for background visual appeal
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateDark, Color(0xFF070B14))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background nebula glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CyanNeon.copy(alpha = 0.12f * pulseScale),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Brand Header & Animated Logo
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                border = BorderStroke(1.5.dp, CyanNeon),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Stock3D Logo",
                        tint = CyanNeon,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Text(
                text = "STOCK3D LOGISTIQUE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = Color.White,
                fontSize = 22.sp
            )

            Text(
                text = "Gestion de Stock Intelligente en 3D & IA",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main input sheet
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                border = BorderStroke(1.dp, SlateCardBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (state) {
                            AuthState.LOGIN -> "Connexion Sécurisée"
                            AuthState.SIGNUP -> "Créer un Compte Administrateur"
                            AuthState.RECOVERY -> "Réinitialiser le Mot de passe"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Error Message Panel
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CrimsonRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, CrimsonRed, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Erreur", tint = CrimsonRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Success Message Panel
                    successMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmeraldGlow.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, EmeraldGlow, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Succès", tint = EmeraldGlow, modifier = Modifier.size(18.dp))
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Field: Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("E-mail Professionnel") },
                        placeholder = { Text("adresse@stock3d.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color.Gray) },
                        colors = loginInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (state == AuthState.RECOVERY && recoveryStep == 1) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        enabled = (state != AuthState.RECOVERY || recoveryStep == 1)
                    )

                    // For Password Recovery Step 2: Verification Code & New Password Fields
                    if (state == AuthState.RECOVERY && recoveryStep == 2) {
                        OutlinedTextField(
                            value = enteredCode,
                            onValueChange = {
                                enteredCode = it
                                errorMessage = null
                            },
                            label = { Text("Code de Vérification (6 chiffres)") },
                            placeholder = { Text("Saisissez le code reçu") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = "Code Icon", tint = CyanNeon) },
                            colors = loginInputColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                errorMessage = null
                            },
                            label = { Text("Nouveau Mot de passe") },
                            placeholder = { Text("Min. 6 caractères") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Basculer la visibilité du mot de passe",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = loginInputColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = {
                                confirmNewPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirmer le Nouveau Mot de passe") },
                            placeholder = { Text("Ressaisir à l'identique") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = "Confirm Lock Icon", tint = Color.Gray) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = loginInputColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true
                        )
                    }

                    // Field: Password (hidden in recovery mode)
                    if (state != AuthState.RECOVERY) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Mot de passe") },
                            placeholder = { Text("Min. 8 caractères") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Basculer la visibilité du mot de passe",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = loginInputColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (state == AuthState.SIGNUP) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true
                        )
                    }

                    // Field: Confirm Password (only in signup mode)
                    if (state == AuthState.SIGNUP) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirmer le mot de passe") },
                            placeholder = { Text("Ressaisir à l'identique") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = "Confirm Lock Icon", tint = Color.Gray) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = loginInputColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true
                        )
                    }

                    // Recovery link
                    if (state == AuthState.LOGIN) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                "Mot de passe oublié ?",
                                color = CyanNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        state = AuthState.RECOVERY
                                        recoveryStep = 1
                                        generatedCode = ""
                                        enteredCode = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons Area
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = CyanNeon,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(32.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                successMessage = null

                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = "Veuillez saisir une adresse e-mail professionnelle valide."
                                    return@Button
                                }

                                if (state != AuthState.RECOVERY && password.length < 6) {
                                    errorMessage = "Le mot de passe doit comporter au moins 6 caractères."
                                    return@Button
                                }

                                isLoading = true
                                when (state) {
                                    AuthState.LOGIN -> {
                                        viewModel.authService.login(email, password, isForceDemo = false) { isSuccess, statusText ->
                                            isLoading = false
                                            if (isSuccess) {
                                                Toast.makeText(context, statusText ?: "Authentifié", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = statusText ?: "Échec de connexion"
                                            }
                                        }
                                    }
                                    AuthState.SIGNUP -> {
                                        if (password != confirmPassword) {
                                            isLoading = false
                                            errorMessage = "Les deux mots de passe ne correspondent pas."
                                            return@Button
                                        }
                                        viewModel.authService.signUp(email, password) { isSuccess, statusText ->
                                            isLoading = false
                                            if (isSuccess) {
                                                // Registered successfully
                                                 viewModel.pushNotification("Nouvel utilisateur créé ! Bienvenue dans votre nouvelle boutique.")
                                                 Toast.makeText(context, "Bienvenue ! Votre boutique a été créée.", Toast.LENGTH_LONG).show()
                                                 onLoginSuccess() // Navigate automatically
                                                // state = AuthState.LOGIN
                                            } else {
                                                errorMessage = statusText ?: "Échec d'inscription"
                                            }
                                        }
                                    }
                                    AuthState.RECOVERY -> {
                                        if (recoveryStep == 1) {
                                            val hasLocalUser = viewModel.authService.hasUser(email)
                                            if (!hasLocalUser) {
                                                isLoading = false
                                                errorMessage = "Aucun administrateur enregistré avec cet e-mail. Veuillez d'abord créer un compte."
                                                return@Button
                                            }

                                            // Generate random 6-digit verification code
                                            val codeDigit = (100000..999999).random().toString()
                                            generatedCode = codeDigit
                                            isLoading = false
                                            recoveryStep = 2
                                            successMessage = "Code de validation envoyé à votre e-mail ! Veuillez le saisir ci-dessous avec votre nouveau mot de passe."
                                            
                                            // Push a system notification and show Toast with the code
                                            viewModel.pushNotification("🔑 Code de récupération pour $email : $codeDigit")
                                            Toast.makeText(context, "Code envoyé par e-mail : $codeDigit", Toast.LENGTH_LONG).show()
                                        } else {
                                            if (enteredCode.trim() != generatedCode) {
                                                isLoading = false
                                                errorMessage = "Code de vérification incorrect. Veuillez vérifier le code reçu."
                                                return@Button
                                            }
                                            if (newPassword.length < 6) {
                                                isLoading = false
                                                errorMessage = "Le nouveau mot de passe doit comporter au moins 6 caractères d'écriture."
                                                return@Button
                                            }
                                            if (newPassword != confirmNewPassword) {
                                                isLoading = false
                                                errorMessage = "Les deux nouveaux mots de passe ne correspondent pas."
                                                return@Button
                                            }

                                            val isSuccessful = viewModel.authService.resetPasswordDirectly(email, newPassword)
                                            isLoading = false
                                            if (isSuccessful) {
                                                Toast.makeText(context, "Mot de passe modifié avec succès !", Toast.LENGTH_LONG).show()
                                                viewModel.pushNotification("🔒 Changement de mot de passe réussi pour $email.")
                                                
                                                state = AuthState.LOGIN
                                                recoveryStep = 1
                                                generatedCode = ""
                                                enteredCode = ""
                                                password = newPassword // pre-fill for convenience
                                                errorMessage = null
                                                successMessage = "Votre mot de passe a bien été modifié. Vous pouvez vous connecter !"
                                            } else {
                                                errorMessage = "Une erreur s'est produite lors de l'enregistrement."
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = when (state) {
                                    AuthState.LOGIN -> "SE CONNECTER AVEC PORTAL"
                                    AuthState.SIGNUP -> "CRÉER UN COMPTE ADMIN"
                                    AuthState.RECOVERY -> if (recoveryStep == 1) "ENVOYER LE CODE PAR MAIL" else "CONFIRMER LE NOUVEAU MOT DE PASSE"
                                },
                                color = SlateDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Direct Custom Storefront Initializer / Access section
            if (state == AuthState.LOGIN) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var customDemoEmail by remember { mutableStateOf("") }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "✨ ACCÉDER OU CRÉER UNE BOUTIQUE",
                                color = Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Entrez un nom pour créer ou ouvrir instantanément votre espace de gestion personnalisé.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            OutlinedTextField(
                                value = customDemoEmail,
                                onValueChange = { customDemoEmail = it },
                                placeholder = { Text("Nom de votre boutique (ex: moussa)") },
                                colors = loginInputColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Storefront, null, tint = CyanNeon, modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (customDemoEmail.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val formattedEmail = if (customDemoEmail.contains("@")) {
                                                    customDemoEmail.trim()
                                                } else {
                                                    "${customDemoEmail.trim().lowercase().replace(" ", "")}@stock3d.com"
                                                }
                                                email = formattedEmail
                                                password = "password123"
                                                focusManager.clearFocus()
                                                isLoading = true
                                                errorMessage = null
                                                successMessage = null
                                                
                                                viewModel.authService.login(email, password, isForceDemo = true) { isSuccess, statusText ->
                                                    isLoading = false
                                                    if (isSuccess) {
                                                        viewModel.pushNotification("Accès à la boutique : $formattedEmail")
                                                        onLoginSuccess()
                                                    } else {
                                                        errorMessage = statusText
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ArrowForward, "Accéder", tint = CyanNeon)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom toggle navigation links
            Row(
                modifier = Modifier
                    .fillPaddingSafe()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (state) {
                        AuthState.LOGIN -> "Nouveau sur Stock3D ?"
                        AuthState.SIGNUP -> "Déjà membre ?"
                        AuthState.RECOVERY -> "Prêt à vous connecter ?"
                    },
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (state) {
                        AuthState.LOGIN -> "Créer un compte"
                        AuthState.SIGNUP -> "Se connecter"
                        AuthState.RECOVERY -> "Se connecter"
                    },
                    color = CyanNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable {
                            state = when (state) {
                                AuthState.LOGIN -> AuthState.SIGNUP
                                AuthState.SIGNUP -> AuthState.LOGIN
                                AuthState.RECOVERY -> AuthState.LOGIN
                            }
                            recoveryStep = 1
                            generatedCode = ""
                            enteredCode = ""
                            newPassword = ""
                            confirmNewPassword = ""
                            errorMessage = null
                            successMessage = null
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

private fun Modifier.fillPaddingSafe(): Modifier = this.padding(6.dp)

@Composable
fun loginInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = SlateCardBorder,
    focusedLabelColor = CyanNeon,
    unfocusedLabelColor = Color.Gray,
    focusedContainerColor = SlateDark,
    unfocusedContainerColor = SlateDark,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = Color.DarkGray,
    unfocusedPlaceholderColor = Color.DarkGray
)
