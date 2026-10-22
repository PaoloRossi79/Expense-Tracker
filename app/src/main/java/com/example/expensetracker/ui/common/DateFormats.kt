package com.paolorossi.expensetracker.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val UK_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.UK)
private val TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("d - EEEE", Locale.UK)

fun LocalDate.toUkDate(): String = format(UK_DATE_FORMATTER)

fun LocalDate.toTransactionDate(): String = format(TRANSACTION_DATE_FORMATTER)
