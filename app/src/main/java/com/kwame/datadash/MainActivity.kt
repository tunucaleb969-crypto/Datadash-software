package com.kwame.datadash

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kwame.datadash.ai.AiAssistantActivity
import com.kwame.datadash.databinding.ActivityMainBinding
import com.kwame.datadash.xlsx.XlsxReader
import com.kwame.datadash.xlsx.XlsxWriter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: EntryViewModel
    private lateinit var adapter: EntryAdapter

    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val entries = viewModel.getAllOnce()
                val ok = XlsxWriter.write(this@MainActivity, uri, entries)
                Toast.makeText(
                    this@MainActivity,
                    if (ok) getString(R.string.export_success) else getString(R.string.export_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val imported = XlsxReader.read(this@MainActivity, uri)
                if (imported.isNotEmpty()) {
                    viewModel.insertAll(imported)
                }
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.import_result, imported.size),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[EntryViewModel::class.java]

        adapter = EntryAdapter(
            onLongClick = { entry ->
                viewModel.delete(entry)
                Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                true
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.entries.observe(this) { list ->
            adapter.submitList(list)
            binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEntryActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setQuery(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ai_assistant -> {
                startActivity(Intent(this, AiAssistantActivity::class.java))
                true
            }
            R.id.action_export -> {
                createDocLauncher.launch("datadash_export.xlsx")
                true
            }
            R.id.action_import -> {
                openDocLauncher.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/octet-stream"
                    )
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
