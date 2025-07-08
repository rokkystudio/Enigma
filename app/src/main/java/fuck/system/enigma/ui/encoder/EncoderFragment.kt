package fuck.system.enigma.ui.encoder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import fuck.system.enigma.Blowfish
import fuck.system.enigma.R

class EncoderFragment : Fragment()
{
    private lateinit var viewModel: EncoderViewModel

    private lateinit var scrollView: ScrollView
    private lateinit var inputText: EditText
    private lateinit var inputKey: EditText
    private lateinit var spinnerMode: Spinner
    private lateinit var checkboxBase64: CheckBox
    private lateinit var checkboxUserIv: CheckBox
    private lateinit var blockIv: ViewGroup
    private lateinit var inputIv: EditText

    private lateinit var resultLabel: TextView
    private lateinit var resultGroup: ViewGroup

    private lateinit var buttonShare: ImageButton
    private lateinit var buttonCopy: ImageButton
    private lateinit var buttonHide: ImageButton

    private lateinit var outputText: TextView
    private lateinit var buttonEncode: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val view = inflater.inflate(R.layout.fragment_encoder, container, false)
        viewModel = ViewModelProvider(this)[EncoderViewModel::class.java]

        scrollView = view.findViewById(R.id.encoder_scroll_inputs)
        inputText = view.findViewById(R.id.encoder_edit_input_text)
        inputKey = view.findViewById(R.id.encoder_edit_encryption_key)
        spinnerMode = view.findViewById(R.id.encoder_spinner_cipher_mode)
        checkboxBase64 = view.findViewById(R.id.encoder_checkbox_base64)
        checkboxUserIv = view.findViewById(R.id.encoder_checkbox_iv_manual)
        blockIv = view.findViewById(R.id.encoder_group_iv)
        inputIv = view.findViewById(R.id.encoder_edit_iv_input)
        outputText = view.findViewById(R.id.encoder_text_output)
        buttonEncode = view.findViewById(R.id.encoder_button_encode)
        buttonCopy = view.findViewById(R.id.encoder_button_copy_output)
        resultGroup = view.findViewById(R.id.encoder_group_output)
        resultLabel = view.findViewById(R.id.encoder_text_output_label)
        buttonShare = view.findViewById(R.id.encoder_button_share_output)
        buttonHide = view.findViewById(R.id.encoder_button_hide_output)

        setupObservers()
        setupListeners()

        return view
    }

    private fun setupObservers() {
        viewModel.inputText.observe(viewLifecycleOwner) { inputText.setText(it) }
        viewModel.key.observe(viewLifecycleOwner) { inputKey.setText(it) }
        viewModel.ivHex.observe(viewLifecycleOwner) { inputIv.setText(it) }
        viewModel.useBase64.observe(viewLifecycleOwner) { checkboxBase64.isChecked = it }
        viewModel.userIv.observe(viewLifecycleOwner) { onUserIvChecked(it) }
        viewModel.result.observe(viewLifecycleOwner) { outputText.text = it }
        viewModel.isResultVisible.observe(viewLifecycleOwner) { onResultGroupVisible(it) }
    }

    private fun setupListeners() {
        checkboxUserIv.setOnCheckedChangeListener { _, isChecked ->
            viewModel.userIv.value = isChecked
        }

        checkboxBase64.setOnCheckedChangeListener { _, isChecked ->
            viewModel.useBase64.value = isChecked
        }

        buttonEncode.setOnClickListener { onEncodeClicked() }
        buttonCopy.setOnClickListener { onCopyClicked() }
        buttonHide.setOnClickListener {
            val current = viewModel.isResultVisible.value ?: true
            viewModel.isResultVisible.value = !current
        }
        buttonShare.setOnClickListener { onShareClicked() }
    }

    private fun onShareClicked()
    {
        val text = outputText.text.toString()
        val errorPrefix = getString(R.string.encoder_message_error, "").removeSuffix("")

        if (text.isBlank() || text.startsWith(errorPrefix)) {
            Toast.makeText(requireContext(), getString(R.string.toast_nothing_to_share), Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser = android.content.Intent.createChooser(intent,
            getString(R.string.encoder_message_share_via))
        startActivity(chooser)
    }

    private fun onResultGroupVisible(visible: Boolean) {
        val parent = resultGroup.parent as ViewGroup
        TransitionManager.beginDelayedTransition(parent, AutoTransition())

        resultGroup.visibility = if (visible) View.VISIBLE else View.GONE
        resultLabel.fadeVisibility(visible)

        buttonHide.setImageResource(
            if (visible) R.drawable.ic_collapse else R.drawable.ic_expand
        )
    }

    private fun onUserIvChecked(isChecked: Boolean)
    {
        if (isChecked) {
            blockIv.visibility = View.VISIBLE
            blockIv.post {
                scrollView.smoothScrollTo(0, blockIv.bottom)
            }
        } else {
            blockIv.visibility = View.GONE
            inputIv.setText("")
        }
    }

    private fun onEncodeClicked() {
        val key = inputKey.text.toString()
        val text = inputText.text.toString()
        val mode = spinnerMode.selectedItem.toString()
        val ivHex = inputIv.text.toString().takeIf { it.isNotBlank() }

        if (key.isEmpty() || text.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.toast_text_and_key_required),
                Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.inputText.value = text
        viewModel.key.value = key
        viewModel.mode.value = mode
        viewModel.ivHex.value = ivHex

        val userIv = viewModel.userIv.value == true

        val cipher = Blowfish().apply {
            setMode(mode)
            setUseBase64(checkboxBase64.isChecked)
            setEmbedIv(!userIv)
            setIv(if (userIv) ivHex?.let { hexToByteArray(it) } else null)
        }

        try {
            val result = cipher.encode(text, key)
            viewModel.result.value = result
            viewModel.isResultVisible.value = true
        } catch (e: Exception) {
            viewModel.result.value = getString(R.string.encoder_message_error, e.message)
        }
    }

    private fun onCopyClicked() {
        val clipboard = requireContext().getSystemService(android.content.ClipboardManager::class.java)
        val clip = android.content.ClipData.newPlainText(getString(R.string.encoder_message_clipboard), outputText.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.lowercase().replace(Regex("[^0-9a-f]"), "")
        require(clean.length % 2 == 0) { getString(R.string.encoder_message_invalid_iv_hex) }
        return ByteArray(clean.length / 2) {
            clean.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    private fun View.fadeVisibility(show: Boolean, duration: Long = 250) {
        if (show) {
            animate().alpha(1f).setDuration(duration).start()
        } else {
            animate().alpha(0.3f).setDuration(duration).start()
        }
    }
}
