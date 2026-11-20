package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.CategoryBudget
import com.example.data.model.CurrencyOption
import com.example.data.model.FinanceFormatters
import com.example.data.model.LoanItem
import com.example.data.model.LoanKind
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringPayment
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing cloud data.")
        if (expectedUid != null && expectedUid != uid) {
            throw IllegalStateException("Signed-in account changed; cancelling write.")
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<TransactionItem>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            userDoc(uid).collection("transactions")
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<CategoryBudget>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/budgets"
        emitAll(
            userDoc(uid).collection("budgets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CategoryBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeRecurringPayments(userId: String): Flow<List<RecurringPayment>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/recurring"
        emitAll(
            userDoc(uid).collection("recurring")
                .orderBy("nextDueMillis", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RecurringPayment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeLoans(userId: String): Flow<List<LoanItem>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/loans"
        emitAll(
            userDoc(uid).collection("loans")
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LoanItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        defaultCurrency: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid)
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "SpendWise User" }.take(80)
            val safeEmail = email.trim().take(120)
            val safeCurrency = CurrencyOption.fromCode(defaultCurrency).code
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "currencyCode" to safeCurrency,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserCurrency(userId: String, currencyCode: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeCurrency = CurrencyOption.fromCode(currencyCode).code
            userDoc(uid).update(
                mapOf(
                    "currencyCode" to safeCurrency,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(
        userId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        dateMillis: Long
    ): Result<Unit> {
        var path = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) { "Title must be 1–80 characters." }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount must be greater than zero." }
            require(cleanCategory.isNotEmpty() && cleanCategory.length <= 40) { "Category is required." }
            require(cleanNote == null || cleanNote.length <= 300) { "Note must be 300 characters or fewer." }
            require(dateMillis > 0L) { "Invalid transaction date." }

            val monthKey = FinanceFormatters.monthKeyFromMillis(dateMillis)
            val docRef = userDoc(uid).collection("transactions").document()
            path = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to cleanCategory,
                "dateMillis" to dateMillis,
                "monthKey" to monthKey,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                payload["note"] = cleanNote
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        userId: String,
        txId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String?,
        dateMillis: Long
    ): Result<Unit> {
        val path = "users/$userId/transactions/$txId"
        return try {
            val uid = requireUserId(userId)
            require(txId.isNotBlank()) { "Transaction ID missing." }
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) { "Title must be 1–80 characters." }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount must be greater than zero." }
            require(cleanCategory.isNotEmpty() && cleanCategory.length <= 40) { "Category is required." }
            require(cleanNote == null || cleanNote.length <= 300) { "Note must be 300 characters or fewer." }
            require(dateMillis > 0L) { "Invalid transaction date." }

            val monthKey = FinanceFormatters.monthKeyFromMillis(dateMillis)
            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to cleanCategory,
                "dateMillis" to dateMillis,
                "monthKey" to monthKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            updates["note"] = cleanNote ?: FieldValue.delete()

            userDoc(uid).collection("transactions").document(txId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, txId: String): Result<Unit> {
        val path = "users/$userId/transactions/$txId"
        return try {
            val uid = requireUserId(userId)
            require(txId.isNotBlank()) { "Transaction ID missing." }
            userDoc(uid).collection("transactions").document(txId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(
        userId: String,
        category: String,
        limitAmount: Double,
        monthKey: String
    ): Result<Unit> {
        val safeCat = category.trim()
        val sanitizedCatId = safeCat.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docId = "budget_${monthKey}_$sanitizedCatId".take(120)
        val path = "users/$userId/budgets/$docId"
        return try {
            val uid = requireUserId(userId)
            require(safeCat.isNotEmpty() && safeCat.length <= 40) { "Category is required." }
            require(limitAmount > 0.0 && limitAmount <= 1_000_000_000.0) { "Budget limit must be positive." }
            require(Regex("^\\d{4}-\\d{2}$").matches(monthKey)) { "Invalid month key." }

            val docRef = userDoc(uid).collection("budgets").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "limitAmount" to limitAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "category" to safeCat,
                        "limitAmount" to limitAmount,
                        "monthKey" to monthKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val path = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireUserId(userId)
            require(budgetId.isNotBlank()) { "Budget ID missing." }
            userDoc(uid).collection("budgets").document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addRecurringPayment(
        userId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        frequency: RecurringFrequency,
        dayOfMonth: Int,
        nextDueMillis: Long,
        reminderDaysBefore: Int
    ): Result<Unit> {
        var path = "users/$userId/recurring"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanCategory = category.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) { "Title must be 1–80 characters." }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount must be positive." }
            require(cleanCategory.isNotEmpty() && cleanCategory.length <= 40) { "Category is required." }
            require(dayOfMonth in 1..31) { "Day of month must be 1–31." }
            require(nextDueMillis > 0L) { "Invalid due date." }
            require(reminderDaysBefore in 0..14) { "Reminder lead days must be 0–14." }

            val docRef = userDoc(uid).collection("recurring").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "amount" to amount,
                    "type" to type.name,
                    "category" to cleanCategory,
                    "frequency" to frequency.name,
                    "dayOfMonth" to dayOfMonth,
                    "nextDueMillis" to nextDueMillis,
                    "reminderDaysBefore" to reminderDaysBefore,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleRecurringActive(userId: String, recurringId: String, active: Boolean): Result<Unit> {
        val path = "users/$userId/recurring/$recurringId"
        return try {
            val uid = requireUserId(userId)
            require(recurringId.isNotBlank()) { "Recurring ID missing." }
            userDoc(uid).collection("recurring").document(recurringId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logRecurringPaymentNow(userId: String, payment: RecurringPayment): Result<Unit> {
        val path = "users/$userId/recurring/${payment.id}"
        return try {
            val uid = requireUserId(userId)
            require(payment.id.isNotBlank()) { "Recurring ID missing." }

            val nowMillis = System.currentTimeMillis()
            val monthKey = FinanceFormatters.monthKeyFromMillis(nowMillis)
            val nextDue = FinanceFormatters.advanceRecurringMillis(
                payment.nextDueMillis,
                payment.parsedFrequency
            )

            val batch = db.batch()
            val txRef = userDoc(uid).collection("transactions").document()
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "title" to payment.title,
                    "amount" to payment.amount,
                    "type" to payment.type,
                    "category" to payment.category,
                    "note" to "Logged from recurring schedule",
                    "dateMillis" to nowMillis,
                    "monthKey" to monthKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val recRef = userDoc(uid).collection("recurring").document(payment.id)
            batch.update(
                recRef,
                mapOf(
                    "nextDueMillis" to nextDue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringPayment(userId: String, recurringId: String): Result<Unit> {
        val path = "users/$userId/recurring/$recurringId"
        return try {
            val uid = requireUserId(userId)
            require(recurringId.isNotBlank()) { "Recurring ID missing." }
            userDoc(uid).collection("recurring").document(recurringId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addLoan(
        userId: String,
        kind: LoanKind,
        personName: String,
        amount: Double,
        dateMillis: Long,
        note: String?
    ): Result<Unit> {
        var path = "users/$userId/loans"
        return try {
            val uid = requireUserId(userId)
            val cleanName = personName.trim()
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanName.isNotEmpty() && cleanName.length <= 80) { "Person name must be 1–80 characters." }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount must be greater than 0." }
            require(dateMillis > 0L) { "Invalid loan date." }
            require(cleanNote == null || cleanNote.length <= 300) { "Note must be 300 characters or fewer." }

            val docRef = userDoc(uid).collection("loans").document()
            path = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "kind" to kind.name,
                "personName" to cleanName,
                "amount" to amount,
                "paidAmount" to 0.0,
                "dateMillis" to dateMillis,
                "settled" to false,
                "repayments" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                payload["note"] = cleanNote
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun repayLoan(
        userId: String,
        loan: LoanItem,
        paymentAmount: Double,
        paymentDateMillis: Long
    ): Result<Unit> {
        val path = "users/$userId/loans/${loan.id}"
        return try {
            val uid = requireUserId(userId)
            require(loan.id.isNotBlank()) { "Loan ID missing." }
            require(paymentAmount > 0.0) { "Repayment amount must be greater than 0." }
            require(paymentDateMillis > 0L) { "Invalid repayment date." }

            val remaining = (loan.amount - loan.paidAmount).coerceAtLeast(0.0)
            val applied = paymentAmount.coerceAtMost(remaining)
            val newPaid = (loan.paidAmount + applied).coerceAtMost(loan.amount)
            val isNowSettled = newPaid >= loan.amount - 0.001
            val updatedList = (loan.repayments + "$paymentDateMillis|$applied").takeLast(100)

            userDoc(uid).collection("loans").document(loan.id).update(
                mapOf(
                    "paidAmount" to newPaid,
                    "settled" to isNowSettled,
                    "repayments" to updatedList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reopenLoan(userId: String, loanId: String): Result<Unit> {
        val path = "users/$userId/loans/$loanId"
        return try {
            val uid = requireUserId(userId)
            require(loanId.isNotBlank()) { "Loan ID missing." }
            userDoc(uid).collection("loans").document(loanId).update(
                mapOf(
                    "paidAmount" to 0.0,
                    "settled" to false,
                    "repayments" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLoan(userId: String, loanId: String): Result<Unit> {
        val path = "users/$userId/loans/$loanId"
        return try {
            val uid = requireUserId(userId)
            require(loanId.isNotBlank()) { "Loan ID missing." }
            userDoc(uid).collection("loans").document(loanId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchActiveRecurringOnce(userId: String): List<RecurringPayment> {
        return userDoc(userId).collection("recurring")
            .get()
            .await()
            .toObjects(RecurringPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.active }
    }

    private fun rethrowOrLog(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
