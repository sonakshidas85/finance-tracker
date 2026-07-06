package com.budgettracker.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * DataStore extension property - a single Preferences DataStore for the whole app, storing one
 * JSON-serialized (and, as of the security hardening pass, Keystore-encrypted) BudgetState string
 * under one key. This is simpler than Proto DataStore (no .proto schema/codegen step) and still
 * fully local; see README "Assumptions & tradeoffs" for the Preferences+JSON vs Proto DataStore
 * discussion.
 */
private val Context.dataStore by preferencesDataStore(name = "budget_tracker_prefs")

/**
 * ENCRYPTION-AT-REST DESIGN NOTE: the app's whole persisted state is one JSON string in one
 * Preferences DataStore key (see [BudgetRepository.Keys]). Swapping the storage engine to
 * EncryptedFile or EncryptedSharedPreferences would mean re-plumbing DataStore's Flow-based read
 * path (used by MainViewModel, both widgets' provideGlance, and the rollover worker) into a
 * polling/callback shape instead - a much bigger disruption for the same outcome. Instead we keep
 * Preferences DataStore exactly as-is and manually encrypt/decrypt the JSON *string* with a
 * javax.crypto.Cipher backed by an AES-256-GCM secret key that lives in the Android Keystore
 * (never in app memory/disk in plaintext, never extractable even with root on most devices/API
 * levels). The key is generated/retrieved directly via the platform `KeyGenerator` +
 * `KeyGenParameterSpec` APIs (the same mechanism Jetpack Security's own EncryptedFile/
 * EncryptedSharedPreferences use under the hood) under a fixed alias owned by this app, so the
 * ciphertext can be stored as a single Base64 string, matching the existing "one JSON blob" shape
 * with minimal disruption. (An earlier version of this went through `androidx.security.crypto
 * .MasterKey` to provision the same kind of key, but reflecting into its `keyAlias` to hand the
 * key back to a manually-driven Cipher isn't supported public API on the version pinned in this
 * project - generating the Keystore key ourselves sidesteps that entirely.)
 */
private object BudgetCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "budget_tracker_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    /** Returns the app's Keystore-resident AES-256-GCM key, generating it on first call. */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun cipherFor(): Pair<Cipher, SecretKey> =
        Cipher.getInstance(TRANSFORMATION) to getOrCreateSecretKey()

    /** Encrypts [plainText] and returns a single Base64 string of IV + ciphertext, safe to store as one DataStore string value. */
    fun encrypt(context: Context, plainText: String): String {
        val (cipher, secretKey) = cipherFor()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Inverse of [encrypt]. Returns null (never throws) if [encoded] is malformed/undecryptable - caller falls back to seed defaults. */
    fun decrypt(context: Context, encoded: String): String? = runCatching {
        val (cipher, secretKey) = cipherFor()
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrNull()
}

/**
 * Repository backing the whole app + both widgets. Exposes a single source-of-truth
 * `Flow<BudgetState>` derived from DataStore. Also owns the rollover-check-on-read logic so that
 * ANY reader (ViewModel on app open, widget on provideGlance, worker on midnight tick) gets an
 * up-to-date, non-stale BudgetState merely by calling [budgetState] / [currentState] /
 * [applyRolloverIfNeeded].
 */
class BudgetRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val BUDGET_STATE_JSON = stringPreferencesKey("budget_state_json")
        // Plain (unencrypted) UI preferences, not part of the BudgetState JSON blob - neither is
        // financial data, so they don't need the Keystore-backed encryption BudgetState gets.
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val MASK_WIDGET_AMOUNTS = booleanPreferencesKey("mask_widget_amounts")
    }

    /** Whether the optional biometric/device-credential unlock gate is enabled. Defaults to false (off). */
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.APP_LOCK_ENABLED] = enabled }
    }

    /** Whether home-screen widgets should render masked placeholders instead of real amounts. Defaults to false (off). */
    val maskWidgetAmounts: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MASK_WIDGET_AMOUNTS] ?: false
    }

    suspend fun setMaskWidgetAmounts(masked: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.MASK_WIDGET_AMOUNTS] = masked }
    }

    /**
     * Raw flow of BudgetState from DataStore, seeding defaults if nothing has been saved yet, or
     * if the stored value can't be decrypted/parsed (e.g. corrupted, or Keystore key unavailable -
     * same "fall back to seed defaults" behavior as pre-encryption first-run handling).
     */
    val budgetState: Flow<BudgetState> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.BUDGET_STATE_JSON]
        if (raw.isNullOrBlank()) {
            seedDefaultState()
        } else {
            val decrypted = BudgetCrypto.decrypt(context, raw)
            if (decrypted == null) {
                seedDefaultState()
            } else {
                runCatching { json.decodeFromString(BudgetState.serializer(), decrypted) }
                    .getOrElse { seedDefaultState() }
            }
        }
    }

    private fun seedDefaultState(): BudgetState = BudgetDefaults.seedBudgetState(
        weeklyStamp = PeriodStamps.currentWeekStamp(),
        monthlyStamp = PeriodStamps.currentMonthStamp()
    )

    /** One-shot read, useful from Workers / non-Compose call sites. */
    suspend fun currentState(): BudgetState = budgetState.first()

    private suspend fun persist(state: BudgetState) {
        val plainJson = json.encodeToString(BudgetState.serializer(), state)
        val encrypted = BudgetCrypto.encrypt(context, plainJson)
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_STATE_JSON] = encrypted
        }
    }

    suspend fun updateSalary(newSalary: Double) {
        val clamped = BudgetCalculations.clampSalary(newSalary)
        val current = currentState()
        persist(current.copy(monthlySalary = clamped))
    }

    suspend fun updateSavingsGoalPercent(newPercent: Float) {
        val clamped = BudgetCalculations.clampSavingsPercent(newPercent)
        val current = currentState()
        persist(current.copy(savingsGoalPercent = clamped))
    }

    /** Weekly allotment is a plain user-entered number, validated the same way as salary (reject negative). */
    suspend fun updateWeeklyAllotment(newAllotment: Double) {
        val clamped = BudgetCalculations.clampWeeklyAllotment(newAllotment)
        val current = currentState()
        persist(current.copy(weeklyAllotment = clamped))
    }

    suspend fun updateCategory(period: BudgetPeriod, category: Category) {
        val current = currentState()
        val clamped = category.copy(
            percent = BudgetCalculations.clampCategoryPercent(category.percent),
            spent = BudgetCalculations.clampSpent(category.spent)
        )
        val updated = current.mutatePeriod(period) { pb ->
            pb.copy(categories = pb.categories.map { if (it.id == clamped.id) clamped else it })
        }
        persist(updated)
    }

    suspend fun addCategory(period: BudgetPeriod) {
        val current = currentState()
        val newCategory = Category(
            id = java.util.UUID.randomUUID().toString(),
            name = "New Category",
            percent = 0f,
            spent = 0.0
        )
        val updated = current.mutatePeriod(period) { pb ->
            pb.copy(categories = pb.categories + newCategory)
        }
        persist(updated)
    }

    suspend fun deleteCategory(period: BudgetPeriod, categoryId: String) {
        val current = currentState()
        val updated = current.mutatePeriod(period) { pb ->
            pb.copy(categories = pb.categories.filterNot { it.id == categoryId })
        }
        persist(updated)
    }

    /** Wipes DataStore back to first-run seeded defaults (used by Settings > Clear all data). */
    suspend fun clearAllData() {
        persist(seedDefaultState())
    }

    /**
     * Runs the rollover check: compares stored periodStamp vs freshly computed one for weekly
     * ISO-week and monthly year-month. If stale, zeroes `spent` on all categories for that period
     * (keeping `percent`/`name` untouched) and updates the stamp. Never touches `percent`.
     * Safe/cheap to call redundantly (app open, widget provideGlance, worker tick) - it's a no-op
     * if both stamps are already current, and persists nothing if nothing changed.
     *
     * @return the up-to-date BudgetState after any rollover has been applied.
     */
    suspend fun applyRolloverIfNeeded(): BudgetState {
        val current = currentState()
        val freshWeekStamp = PeriodStamps.currentWeekStamp()
        val freshMonthStamp = PeriodStamps.currentMonthStamp()

        var changed = false
        var weekly = current.weekly
        var monthly = current.monthly

        if (weekly.periodStamp != freshWeekStamp) {
            weekly = weekly.copy(
                categories = weekly.categories.map { it.copy(spent = 0.0) },
                periodStamp = freshWeekStamp
            )
            changed = true
        }
        if (monthly.periodStamp != freshMonthStamp) {
            monthly = monthly.copy(
                categories = monthly.categories.map { it.copy(spent = 0.0) },
                periodStamp = freshMonthStamp
            )
            changed = true
        }

        return if (changed) {
            val updated = current.copy(weekly = weekly, monthly = monthly)
            persist(updated)
            updated
        } else {
            current
        }
    }

    /**
     * Shared "apply a weekly spend" logic used by the QuickAddSpendActivity popup (and any future
     * entry point), so the rollover-check-then-write flow exists in exactly one place. Steps:
     *  1. Run the same [applyRolloverIfNeeded] check every other reader (app open, provideGlance,
     *     RolloverWorker) already runs, so a spend logged right at a week boundary lands in the
     *     correct (current) week's bucket rather than a stale one.
     *  2. Look up [categoryId] in the now-current `weekly.categories` list.
     *  3. Add [amount] to that category's `spent` (clamped the same way [updateCategory] does)
     *     and persist through the normal encrypted-write path.
     *
     * No-ops (returns without persisting again) if [categoryId] isn't found in the current weekly
     * period - e.g. the category was deleted from the app after the widget/popup was rendered.
     */
    suspend fun applyWeeklySpend(categoryId: String, amount: Double) {
        val current = applyRolloverIfNeeded()
        val target = current.weekly.categories.find { it.id == categoryId } ?: return
        val updatedCategory = target.copy(spent = BudgetCalculations.clampSpent(target.spent + amount))
        val updated = current.mutatePeriod(BudgetPeriod.WEEKLY) { pb ->
            pb.copy(categories = pb.categories.map { if (it.id == categoryId) updatedCategory else it })
        }
        persist(updated)
    }

    private fun BudgetState.mutatePeriod(period: BudgetPeriod, transform: (PeriodBudget) -> PeriodBudget): BudgetState =
        when (period) {
            BudgetPeriod.WEEKLY -> copy(weekly = transform(weekly))
            BudgetPeriod.MONTHLY -> copy(monthly = transform(monthly))
        }

    companion object {
        @Volatile
        private var INSTANCE: BudgetRepository? = null

        fun getInstance(context: Context): BudgetRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgetRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
