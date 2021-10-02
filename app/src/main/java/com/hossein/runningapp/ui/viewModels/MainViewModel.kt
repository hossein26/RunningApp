package com.hossein.runningapp.ui.viewModels

import androidx.lifecycle.ViewModel
import com.hossein.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {

}