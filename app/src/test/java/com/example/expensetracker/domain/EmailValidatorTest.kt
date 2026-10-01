package com.paolorossi.expensetracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {
    @Test
    fun `accepts well-formed addresses`() {
        assertTrue(EmailValidator.isValid("paolo@example.com"))
        assertTrue(EmailValidator.isValid("first.last+tag@sub.example.co.uk"))
    }

    @Test
    fun `rejects malformed addresses`() {
        assertFalse(EmailValidator.isValid(""))
        assertFalse(EmailValidator.isValid("not-an-email"))
        assertFalse(EmailValidator.isValid("missing@tld"))
        assertFalse(EmailValidator.isValid("@example.com"))
        assertFalse(EmailValidator.isValid("spaces in@example.com"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertTrue(EmailValidator.isValid("  paolo@example.com  "))
    }
}
