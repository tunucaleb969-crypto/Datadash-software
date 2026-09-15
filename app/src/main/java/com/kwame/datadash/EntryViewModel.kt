package com.kwame.datadash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kwame.datadash.data.AppDatabase
import com.kwame.datadash.data.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).entryDao()

    private val query = MutableStateFlow("")

    val entries: LiveData<List<Entry>> = query
        .flatMapLatest { q -> if (q.isBlank()) dao.getAll() else dao.search(q) }
        .asLiveData()

    fun setQuery(q: String) {
        query.value = q
    }

    fun insert(entry: Entry) = viewModelScope.launch { dao.insert(entry) }

    fun delete(entry: Entry) = viewModelScope.launch { dao.delete(entry) }

    suspend fun getAllOnce(): List<Entry> = dao.getAllOnce()

    fun insertAll(entries: List<Entry>) = viewModelScope.launch { dao.insertAll(entries) }
}
