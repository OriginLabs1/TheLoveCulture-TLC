package culture.thelove.tlc.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import culture.thelove.tlc.android.BuildConfig
import culture.thelove.tlc.android.R
import culture.thelove.tlc.android.ui.ModuleRow
import culture.thelove.tlc.android.ui.SectionCard
import culture.thelove.tlc.domain.DoctrineCopy

@Composable
public fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 48.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "brand") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_wordmark),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = FontFamily.Serif,
                    )
                }
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.about_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        item(key = "purpose") {
            SectionCard(title = stringResource(R.string.about_purpose_title)) {
                Text(
                    text = stringResource(R.string.about_purpose),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item(key = "never") {
            SectionCard(
                title = stringResource(R.string.about_not_money_title),
                container = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = DoctrineCopy.FINANCIAL_NOTICE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        item(key = "modules") {
            SectionCard(title = stringResource(R.string.about_modules_title)) {
                Text(
                    text = stringResource(R.string.about_modules_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ModuleRow(
                    index = 1,
                    title = stringResource(R.string.module_journal),
                    body = stringResource(R.string.module_journal_body),
                )
                ModuleRow(
                    index = 2,
                    title = stringResource(R.string.module_grants),
                    body = stringResource(R.string.module_grants_body),
                )
                ModuleRow(
                    index = 3,
                    title = stringResource(R.string.module_dues),
                    body = stringResource(R.string.module_dues_body),
                )
                ModuleRow(
                    index = 4,
                    title = stringResource(R.string.module_circle),
                    body = stringResource(R.string.module_circle_body),
                )
                ModuleRow(
                    index = 5,
                    title = stringResource(R.string.module_verify),
                    body = stringResource(R.string.module_verify_body),
                )
                ModuleRow(
                    index = 6,
                    title = stringResource(R.string.module_backup),
                    body = stringResource(R.string.module_backup_body),
                )
            }
        }

        item(key = "now") {
            SectionCard(title = stringResource(R.string.about_now_title)) {
                Text(
                    text = stringResource(R.string.about_now),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item(key = "next") {
            SectionCard(title = stringResource(R.string.about_next_title)) {
                Text(
                    text = stringResource(R.string.about_next),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "contact") {
            SectionCard(title = stringResource(R.string.about_contact_title)) {
                Text(
                    text = stringResource(R.string.about_contact_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ContactRow(
                    label = stringResource(R.string.about_website),
                    value = stringResource(R.string.about_website_value),
                    onClick = { uriHandler.openUri("https://sircollins.coreconcept1.net") },
                )
                ContactRow(
                    label = stringResource(R.string.about_telegram),
                    value = stringResource(R.string.about_telegram_value),
                    onClick = { uriHandler.openUri("https://t.me/CollinsRoyalMedium") },
                )
                ContactRow(
                    label = stringResource(R.string.about_email),
                    value = stringResource(R.string.about_email_value),
                    onClick = { uriHandler.openUri("mailto:tlcfounder@protonmail.com") },
                )
            }
        }

        item(key = "meta") {
            SectionCard(title = stringResource(R.string.about_meta_title)) {
                Text(
                    text = stringResource(
                        R.string.about_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = stringResource(R.string.about_licence),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_source),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_stores),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One deliberate way to reach the person behind TLC, opened by the system. No details are stored. */
@Composable
private fun ContactRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
