@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.donate

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.billing.BillingState
import com.suvojeet.notenext.billing.PurchaseState
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun DonationScreen(
    onBackClick: () -> Unit,
    viewModel: DonationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingState by viewModel.billingState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val products by viewModel.products.collectAsState()

    var showThankYouDialog by remember { mutableStateOf(false) }

    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Success) {
            showThankYouDialog = true
            viewModel.resetPurchaseState()
        }
    }

    // Thank You Dialog
    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showThankYouDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.thank_you_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    stringResource(R.string.thank_you_msg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showThankYouDialog = false },
                    modifier = Modifier.springPress()
                ) {
                    Text(stringResource(R.string.thank_you_confirm))
                }
            }
        )
    }

    // Failed snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val failedState = purchaseState as? PurchaseState.Failed
    LaunchedEffect(failedState) {
        failedState?.let {
            snackbarHostState.showSnackbar(context.getString(R.string.purchase_failed, it.message))
            viewModel.resetPurchaseState()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.support_notenext),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Hero Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.VolunteerActivism,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    stringResource(R.string.made_with_love_free),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(R.string.donation_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Why Support Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.why_donate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                ReasonCard(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.reason_independent_title),
                    description = stringResource(R.string.reason_independent_desc)
                )

                ReasonCard(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.reason_privacy_title),
                    description = stringResource(R.string.reason_privacy_desc)
                )

                ReasonCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.reason_updates_title),
                    description = stringResource(R.string.reason_updates_desc)
                )
            }

            // Donation Options Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.choose_amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                when (billingState) {
                    is BillingState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.connecting_play_store),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    is BillingState.Error -> {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Column {
                                    Text(
                                        stringResource(R.string.could_not_connect_play_store),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        stringResource(R.string.check_internet_try_again),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    is BillingState.Ready -> {
                        AnimatedVisibility(
                            visible = products.isNotEmpty(),
                            enter = fadeIn() + scaleIn(initialScale = 0.92f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                products.forEach { product ->
                                    val priceStr = product.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                                    val (emoji, label, subtitle) = when (product.productId) {
                                        "donate_small" -> Triple(
                                            "☕",
                                            stringResource(R.string.donate_small_label),
                                            stringResource(R.string.donate_small_desc)
                                        )
                                        "donate_medium" -> Triple(
                                            "🍕",
                                            stringResource(R.string.donate_medium_label),
                                            stringResource(R.string.donate_medium_desc)
                                        )
                                        "donate_large" -> Triple(
                                            "🚀",
                                            stringResource(R.string.donate_large_label),
                                            stringResource(R.string.donate_large_desc)
                                        )
                                        else -> Triple("💙", product.name, "")
                                    }

                                    val isPurchasing = purchaseState is PurchaseState.Pending

                                    DonationOptionCard(
                                        emoji = emoji,
                                        label = label,
                                        subtitle = subtitle,
                                        price = priceStr,
                                        isLoading = isPurchasing,
                                        onClick = {
                                            if (!isPurchasing && activity != null) {
                                                viewModel.donate(activity, product)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (products.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        stringResource(R.string.loading_options),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Text(
                stringResource(R.string.secure_google_play),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
fun ReasonCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DonationOptionCard(
    emoji: String,
    label: String,
    subtitle: String,
    price: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .springPress(),
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 28.sp)
                    }
                }
                
                Column {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        price,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

