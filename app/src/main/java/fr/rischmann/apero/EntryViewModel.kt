package fr.rischmann.apero

import android.util.Log
import androidx.lifecycle.*
import fr.rischmann.apero.Logging.TAG
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class EntryViewModelFactory(private val client: EntryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EntryViewModel(client) as T
    }
}

class EntryViewModel(private val repository: EntryRepository) : ViewModel() {
    private val reload = MutableLiveData<Boolean>()

    val entries: LiveData<Entries> = Transformations.switchMap(reload) {
        repository.getEntries()
    }


    fun moveEntry(entry: Entry): ByteArray? {
        val ld = repository.moveEntry(entry)
        return resultFromFuture(entry, ld)
    }

    fun pasteEntry(entry: Entry): ByteArray? {
        val ld = repository.pasteEntry(entry)
        return resultFromFuture(entry, ld)
    }

    private fun resultFromFuture(entry: Entry, future: CompletableFuture<ByteArray>): ByteArray? {
        return try {
            future.get(4, TimeUnit.SECONDS)?.also {
                // If we did paste the item trigger a reload of the list
                reloadEntries()
            }
        } catch (e: TimeoutException) {
            Log.e(TAG, "unable to move entry $entry", e)
            null
        } catch (e: CancellationException) {
            Log.e(TAG, "unable to move entry $entry", e)
            null
        }
    }

    fun reloadEntries() {
        reload.value = true
    }
}