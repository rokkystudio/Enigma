package fuck.system.enigma.ui.encoder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EncoderViewModel : ViewModel()
{
    val inputText = MutableLiveData<String>()
    val key = MutableLiveData<String>()
    val mode = MutableLiveData<String>()
    val useBase64 = MutableLiveData<Boolean>().apply { value = true }
    val userIv = MutableLiveData<Boolean>().apply { value = false }
    val ivHex = MutableLiveData<String>()
    val result = MutableLiveData<String>()
    val isResultVisible = MutableLiveData<Boolean>().apply { value = false }
}