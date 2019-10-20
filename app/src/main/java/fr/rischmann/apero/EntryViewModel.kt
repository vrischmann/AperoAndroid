package fr.rischmann.apero

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EntryViewModel : ViewModel() {
    private val entries: MutableLiveData<List<Entry>> by lazy {
        // Only run once at initialization
        val list = MutableLiveData<List<Entry>>()
        list.value = Entry.ALL
        list
    }

    fun getEntries(): LiveData<List<Entry>> {
        return entries
    }

    fun removeEntry(entry: Entry) {
        requireNotNull(entries.value) { "entry $entry doesn't exist" }

        val list = entries.value?.filterNot { it == entry }
        entries.value = list
    }

    private fun loadEntries() {
        entries.value = Entry.ALL
    }
}