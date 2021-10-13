package com.hossein.runningapp.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hossein.runningapp.db.Run
import com.hossein.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
}