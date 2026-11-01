package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM documents ORDER BY favourite DESC, updatedAt DESC")
    fun observeAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsOnce(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :docId LIMIT 1")
    suspend fun getDocumentById(docId: Long): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: Long)

    @Query("DELETE FROM documents WHERE isSamplePlaceholder = 1")
    suspend fun deleteSamplePlaceholderDocuments()

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT * FROM document_fields ORDER BY sortOrder ASC, id ASC")
    fun observeAllFields(): Flow<List<DocumentFieldEntity>>

    @Query("SELECT * FROM document_fields WHERE documentId = :docId ORDER BY sortOrder ASC, id ASC")
    suspend fun getFieldsForDocument(docId: Long): List<DocumentFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<DocumentFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: DocumentFieldEntity): Long

    @Update
    suspend fun updateField(field: DocumentFieldEntity)

    @Query("DELETE FROM document_fields WHERE documentId = :docId")
    suspend fun deleteFieldsForDocument(docId: Long)

    @Query("DELETE FROM document_fields")
    suspend fun deleteAllFields()

    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT 50")
    fun observeAuditEvents(): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEventEntity)

    @Query("DELETE FROM audit_events")
    suspend fun clearAuditEvents()
}
