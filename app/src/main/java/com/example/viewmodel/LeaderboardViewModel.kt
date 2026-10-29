package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DomainLeaderboard
import com.example.domain.model.RankedLeaderboardEntry
import com.example.domain.model.RankingMode
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.LeaderboardRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedDomainId: String = CareerCanonicalUtil.CAREER_AIML,
    val selectedDomainTitle: String = CareerCanonicalUtil.getCareerTitle(CareerCanonicalUtil.CAREER_AIML),
    val activeRankingMode: RankingMode = RankingMode.XP,
    val availableDomains: List<Pair<String, String>> = CareerCanonicalUtil.CANONICAL_CAREER_IDS.map {
        it to CareerCanonicalUtil.getCareerTitle(it)
    },
    val leaderboard: DomainLeaderboard? = null,
    val currentUserId: String? = null,
    val refreshError: String? = null
) {
    val entries: List<RankedLeaderboardEntry>
        get() = leaderboard?.entries ?: emptyList()

    val currentUserEntry: RankedLeaderboardEntry?
        get() = leaderboard?.currentUserEntry

    val top3Entries: List<RankedLeaderboardEntry>
        get() = entries.take(3)

    val remainingEntries: List<RankedLeaderboardEntry>
        get() = if (entries.size > 3) entries.drop(3) else emptyList()

    val totalParticipants: Int
        get() = leaderboard?.totalParticipants ?: entries.size
}

class LeaderboardViewModel(
    private val leaderboardRepository: LeaderboardRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var hasInitializedDomain = false

    init {
        viewModelScope.launch {
            val user = authRepository?.getCurrentUser()?.firstOrNull()
            val preferredDomain = CareerCanonicalUtil.toCanonicalCareerIdOrNull(user?.recommendedCareerId)
                ?: CareerCanonicalUtil.CAREER_AIML

            _uiState.update {
                it.copy(
                    selectedDomainId = preferredDomain,
                    selectedDomainTitle = CareerCanonicalUtil.getCareerTitle(preferredDomain),
                    currentUserId = user?.id
                )
            }
            hasInitializedDomain = true
            loadLeaderboard()
        }
    }

    fun initializeWithDomain(domainId: String?) {
        val canonical = CareerCanonicalUtil.toCanonicalCareerIdOrNull(domainId)
        if (canonical != null && canonical != _uiState.value.selectedDomainId) {
            selectDomain(canonical)
        }
    }

    fun selectDomain(domainId: String) {
        val canonical = CareerCanonicalUtil.toCanonicalCareerId(domainId)
        if (_uiState.value.selectedDomainId == canonical) return

        _uiState.update {
            it.copy(
                selectedDomainId = canonical,
                selectedDomainTitle = CareerCanonicalUtil.getCareerTitle(canonical),
                isLoading = true,
                error = null
            )
        }
        loadLeaderboard()
    }

    fun selectRankingMode(mode: RankingMode) {
        if (_uiState.value.activeRankingMode == mode) return

        _uiState.update {
            it.copy(
                activeRankingMode = mode,
                isLoading = true,
                error = null
            )
        }
        loadLeaderboard()
    }

    fun clearRefreshError() {
        _uiState.update { it.copy(refreshError = null) }
    }

    fun refresh() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, refreshError = null) }
            try {
                val fresh = leaderboardRepository.refreshLeaderboard(
                    current.selectedDomainId,
                    current.activeRankingMode
                )
                _uiState.update {
                    it.copy(
                        leaderboard = fresh,
                        isRefreshing = false,
                        isLoading = false,
                        refreshError = null
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    if (it.leaderboard != null) {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false,
                            refreshError = e.message ?: "Failed to refresh leaderboard"
                        )
                    } else {
                        it.copy(
                            isRefreshing = false,
                            isLoading = false,
                            error = e.message ?: "Failed to refresh leaderboard"
                        )
                    }
                }
            }
        }
    }

    private fun loadLeaderboard() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            leaderboardRepository.getLeaderboard(
                current.selectedDomainId,
                current.activeRankingMode
            ).catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unable to load leaderboard"
                    )
                }
            }.collect { domainLeaderboard ->
                _uiState.update {
                    it.copy(
                        leaderboard = domainLeaderboard,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}
