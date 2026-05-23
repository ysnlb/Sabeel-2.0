package com.example.ui.screens.adhkar

import androidx.lifecycle.ViewModel
import com.example.domain.model.DhikrCategory
import com.example.domain.model.DhikrItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class AdhkarUiState(
    val selectedCategory: DhikrCategory = DhikrCategory.MORNING,
    val allAdhkar: List<DhikrItem> = emptyList()
) {
    val displayedAdhkar: List<DhikrItem>
        get() = allAdhkar.filter { it.category == selectedCategory }
}

class AdhkarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdhkarUiState())
    val uiState: StateFlow<AdhkarUiState> = _uiState.asStateFlow()

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        _uiState.update { it.copy(allAdhkar = com.example.domain.model.DhikrData.ADHKAR_LIST.map { dhikr -> dhikr.copy() }) }
    }

    fun selectCategory(category: DhikrCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun incrementDhikr(id: String) {
        _uiState.update { state ->
            val updatedList = state.allAdhkar.map { dhikr ->
                if (dhikr.id == id && dhikr.currentCount < dhikr.targetCount) {
                    dhikr.copy(currentCount = dhikr.currentCount + 1)
                } else {
                    dhikr
                }
            }
            state.copy(allAdhkar = updatedList)
        }
    }
}
