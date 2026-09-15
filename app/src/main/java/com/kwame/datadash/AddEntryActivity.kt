package com.kwame.datadash

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.kwame.datadash.data.Entry
import com.kwame.datadash.databinding.ActivityAddEntryBinding
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
