package com.mustafa.notiflog.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mustafa.notiflog.data.AppDatabase
import com.mustafa.notiflog.databinding.ActivityConversationDetailBinding

class ConversationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationDetailBinding
    private lateinit var adapter: MessageAdapter
    private var showAll = false
    private lateinit var conversationTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationTitle = intent.getStringExtra(EXTRA_CONVERSATION_TITLE) ?: run {
            finish(); return
        }
        binding.toolbarTitle.text = conversationTitle
        binding.btnBack.setOnClickListener { finish() }

        adapter = MessageAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.toggleShowAll.setOnCheckedChangeListener { _, checked ->
            showAll = checked
            observeMessages()
        }

        observeMessages()
    }

    private fun observeMessages() {
        val dao = AppDatabase.getInstance(applicationContext).messageDao()
        val liveData = if (showAll) dao.getAllForConversation(conversationTitle)
                       else dao.getDeletedForConversation(conversationTitle)

        liveData.observe(this) { list ->
            adapter.submitList(list)
            binding.emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.emptyView.text = if (showAll)
                "Bu kişiden/gruptan henüz mesaj yakalanmadı."
            else
                "Bu kişinin/grubun sildiği bir mesaj yok (henüz).\nÜstteki anahtarı açarak tüm mesajları görebilirsin."
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_TITLE = "extra_conversation_title"
    }
}
