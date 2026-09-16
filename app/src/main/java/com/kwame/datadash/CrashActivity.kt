package com.kwame.datadash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kwame.datadash.databinding.ActivityCrashBinding

class CrashActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val trace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available."
        binding.crashText.text = trace

        binding.buttonClose.setOnClickListener { finishAffinity() }
    }
}
