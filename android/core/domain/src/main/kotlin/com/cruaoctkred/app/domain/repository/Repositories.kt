package com.cruaoctkred.app.domain.repository

import kotlinx.coroutines.flow.Flow
import com.cruaoctkred.app.domain.model.AttributionSnapshot
import com.cruaoctkred.app.domain.model.CardsContent
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.RulesPage
import com.cruaoctkred.app.domain.model.StreamResult

interface CardsRepository {
    fun observePublishedCards(): Flow<StreamResult<CardsContent>>
    fun observeCard(id: String): Flow<StreamResult<LoanCard>>
    suspend fun refresh()
}

interface PagesRepository {
    fun observeRules(): Flow<StreamResult<RulesPage>>
}

interface SelectionRepository {
    val savedIds: Flow<Set<String>>
    val comparedIds: Flow<List<String>>
    suspend fun toggleSaved(id: String)
    suspend fun toggleCompared(id: String): Boolean
}

interface OfferRoutingRepository {
    fun observe(): Flow<com.cruaoctkred.app.domain.model.OfferRoutingSetting>
}

interface NetworkStatusRepository {
    val online: kotlinx.coroutines.flow.StateFlow<Boolean>
    fun isOnline(): Boolean
}

interface AttributionRepository {
    val installStatus: kotlinx.coroutines.flow.StateFlow<com.cruaoctkred.app.domain.model.InstallAttributionStatus>
    fun saveInstallStatus(status: com.cruaoctkred.app.domain.model.InstallAttributionStatus)

    fun read(): AttributionSnapshot
    fun save(snapshot: AttributionSnapshot)
    suspend fun trackingTemplate(): String
}

interface PushTokenRepository {
    suspend fun registerCurrentToken(): Result<Unit>
    suspend fun register(token: String): Result<Unit>
}

