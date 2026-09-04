import re

file_path = "app/src/main/java/com/hwnix/cash/presentation/screens/StatusScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Remove the 'onAddWalletClick' from StatusScreen parameters and add 'onAddWalletForLineClick: (Int) -> Unit'
content = content.replace("onAddWalletClick: () -> Unit,", "onAddWalletForLineClick: (Int) -> Unit,")

# Replace the linesSummary text block completely
old_block_summary = \"\"\"                        if (linesSummary.isNotBlank()) {
                            Text(
                                text = linesSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            
                            if (state.deviceLines.isNotEmpty()) {\"\"\"

new_block_summary = \"\"\"                        if (state.deviceLines.isNotEmpty()) {\"\"\"

content = content.replace(old_block_summary, new_block_summary)

old_card = \"\"\"                                                    Text(
                                                        text = if (lineWallet != null) "\ (\)" else "?? \",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {\"\"\"

new_card = \"\"\"                                                    Text(
                                                        text = if (lineWallet != null) "\ (\)" else "?? \",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text("?????? ??????", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                        Text("\ ?.?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("?????? ???????", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                        Text("\ ?.?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (lineWallet != null && (lineWallet.dailyWithdrawLimit != null || lineWallet.dailyDepositLimit != null)) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Column {
                                                            Text("?? ????? ??????", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("\ ?.?", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text("?? ??????? ??????", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                            Text("\ ?.?", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {\"\"\"

content = content.replace(old_card, new_card)

old_add_btn = \"\"\"                                                    } else {
                                                        OutlinedButton(
                                                            onClick = onAddWalletClick,
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("?????", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }\"\"\"

new_add_btn = \"\"\"                                                    } else {
                                                        OutlinedButton(
                                                            onClick = { onAddWalletForLineClick(slot) },
                                                            modifier = Modifier.weight(2f),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("????? ????? ???? ????", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }\"\"\"

content = content.replace(old_add_btn, new_add_btn)

# Remove the bottom Create Wallet Button
old_bottom_btn = \"\"\"                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onAddWalletClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0D47A1)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFF0D47A1).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "????? ????? ?????",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }\"\"\"

content = content.replace(old_bottom_btn, "")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Patched StatusScreen.kt successfully!")
