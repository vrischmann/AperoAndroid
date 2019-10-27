package fr.rischmann.apero

import android.util.Log
import androidx.lifecycle.*
import fr.rischmann.apero.Logging.TAG
import java.util.concurrent.CancellationException
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

    val entries: LiveData<AperoResponse<Entries>> = Transformations.switchMap(reload) {
        repository.list()
    }


    fun moveEntry(entry: Entry): AperoResponse<ByteArray>? {
        val ld = repository.move(entry)
        return resultFromFuture(entry, ld)
    }

    fun pasteEntry(entry: Entry): AperoResponse<ByteArray>? {
        val ld = repository.paste(entry)
        return resultFromFuture(entry, ld)
    }

    private fun resultFromFuture(entry: Entry, future: AperoCompletableFuture<ByteArray>): AperoResponse<ByteArray>? {
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
        } catch (e: IllegalStateException) {
            Log.w(TAG, "unable to execute future", e)
            null
        }
    }

    fun reloadEntries() {
        reload.value = true
    }
}