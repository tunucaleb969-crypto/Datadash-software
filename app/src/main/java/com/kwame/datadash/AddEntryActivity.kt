package com.kwame.datadash

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kwame.datadash.ai.GeminiClient
import com.kwame.datadash.data.Entry
import com.kwame.datadash.databinding.ActivityAddEntryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEntryBinding
    private lateinit var viewModel: EntryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.add_entry)

        viewModel = ViewModelProvider(this)[EntryViewModel::class.java]

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.inputDate.setText(today)

        binding.buttonSuggestCategory.setOnClickListener { suggestCategory() }

        binding.buttonSave.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val phone = binding.inputPhone.text.toString().trim()
            val category = binding.inputCategory.text.toString().trim()
            val amountText = binding.inputAmount.text.toString().trim()
            val date = binding.inputDate.text.toString().trim()
            val notes = binding.inputNotes.text.toString().trim()

            if (name.isEmpty()) {
                binding.inputName.error = getString(R.string.required_field)
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull() ?: 0.0

            viewModel.insert(
                Entry(
                    name = name,
                    phone = phone,
                    category = category,
                    amount = amount,
                    date = date,
                    notes = notes
                )
            )
            Toast.makeText(this, R.string.entry_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun suggestCategory() {
        val name = binding.inputName.text.toString().trim()
        val notes = binding.inputNotes.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputName.error = getString(R.string.required_field)
            return
        }

        binding.buttonSuggestCategory.isEnabled = false
        lifecycleScope.launch {
            val prompt = "Suggest a single short category label (1 to 3 words, no quotes, " +
                "no explanation, just the label) for this small-business record. " +
                "Name: $name. Notes: $notes."
            val result = GeminiClient.generate(prompt)
            binding.buttonSuggestCategory.isEnabled = true
            result.onSuccess { suggestion ->
                val clean = suggestion.trim().trim('"').lines().first().trim()
                binding.inputCategory.setText(clean)
            }
            result.onFailure {
                Toast.makeText(
                    this@AddEntryActivity,
                    getString(R.string.ai_error, it.message ?: "unknown error"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
