package com.kwame.datadash.ai

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kwame.datadash.EntryViewModel
import com.kwame.datadash.R
import com.kwame.datadash.data.Entry
import com.kwame.datadash.databinding.ActivityAiAssistantBinding
import kotlinx.coroutines.launch

class AiAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiAssistantBinding
    private lateinit var viewModel: EntryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.ai_assistant)

        viewModel = ViewModelProvider(this)[EntryViewModel::class.java]

        binding.buttonAsk.setOnClickListener {
            val question = binding.inputQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                ask(question)
            }
        }

        binding.buttonInsights.setOnClickListener {
            ask(
                "Analyze all these business records and give me 3 to 5 short, practical " +
                    "insights (trends, top categories, anything unusual). Keep it brief and " +
                    "in plain language, as a bulleted list."
            )
        }
    }

    private fun ask(question: String) {
        setLoading(true)
        lifecycleScope.launch {
            val entries = viewModel.getAllOnce()
            val prompt = buildPrompt(question, entries)
            val result = GeminiClient.generate(prompt)
            setLoading(false)
            result.fold(
                onSuccess = { binding.responseText.text = it },
                onFailure = {
                    binding.responseText.text = getString(
                        R.string.ai_error,
                        it.message ?: "unknown error"
                    )
                }
            )
        }
    }

    private fun buildPrompt(question: String, entries: List<Entry>): String {
        val dataSummary = if (entries.isEmpty()) {
            "No records yet."
        } else {
            entries.joinToString("\n") { e ->
                "${e.date} | ${e.name} | ${e.category} | GHS ${e.amount} | ${e.phone} | ${e.notes}"
            }
        }
        return "You are a helpful business data assistant for a small business owner. " +
            "Here are their records (date | name | category | amount | phone | notes):\n" +
            "$dataSummary\n\nQuestion: $question\n\nAnswer briefly and clearly."
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonAsk.isEnabled = !loading
        binding.buttonInsights.isEnabled = !loading
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
