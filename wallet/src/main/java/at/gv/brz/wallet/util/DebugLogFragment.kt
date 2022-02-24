package at.gv.brz.wallet.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.gv.brz.eval.utils.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import at.gv.brz.eval.utils.prettyPrint
import at.gv.brz.wallet.R
import at.gv.brz.wallet.databinding.FragmentDebugLogBinding
import at.gv.brz.wallet.databinding.ItemDebugLogBinding
import java.time.Instant

/**
 * Debug utility Fragment that display the entries that were logged via DebugLogUtil - only to be displayed in test builds
 */
class DebugLogFragment: Fragment() {

    companion object {
        fun newInstance(): DebugLogFragment = DebugLogFragment()
    }

    private  var _binding: FragmentDebugLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebugLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.debugLogListToolbar.setNavigationOnClickListener { v: View? ->
            parentFragmentManager.popBackStack()
        }
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.debugLogListRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
        val adapter = DebugLogListAdapter()
        recyclerView.adapter = adapter

        adapter.setItems(DebugLogUtil.lastLogs(requireContext()).reversed())
    }
}

class DebugLogListAdapter(): RecyclerView.Adapter<DebugLogListViewHolder>() {
    private val items: MutableList<Pair<Instant, String>> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugLogListViewHolder {
        return DebugLogListViewHolder.inflate(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: DebugLogListViewHolder, position: Int) {
        holder.bindItem(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(items: List<Pair<Instant, String>>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
}

class DebugLogListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): DebugLogListViewHolder {
            val itemView = inflater.inflate(R.layout.item_debug_log, parent, false)
            return DebugLogListViewHolder(itemView)
        }
    }

    fun bindItem(logEntry: Pair<Instant, String>) {
        val binding = ItemDebugLogBinding.bind(itemView)
        binding.logDateTv.text = logEntry.first.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)
        binding.logTextTv.text = logEntry.second
    }

}