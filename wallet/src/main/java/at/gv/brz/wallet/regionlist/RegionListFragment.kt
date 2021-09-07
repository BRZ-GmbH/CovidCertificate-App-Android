package at.gv.brz.wallet.regionlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.wallet.data.Region
import at.gv.brz.wallet.data.WalletSecureStorage
import at.gv.brz.wallet.databinding.FragmentRegionListBinding

class RegionListFragment : Fragment() {

    companion object {
        fun newInstance(): RegionListFragment = RegionListFragment()
    }

    val secureStorage by lazy { WalletSecureStorage.getInstance(this.requireContext()) }

    private  var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.regionListToolbar.setNavigationOnClickListener { v: View? ->
            parentFragmentManager.popBackStack()
        }
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.regionListRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
        val adapter = RegionListAdapter { region ->
            secureStorage.setSelectedValidationRegion(region.identifier)
            parentFragmentManager.popBackStack()
        }
        recyclerView.adapter = adapter

        adapter.setItems(Region.values().map { RegionItem(it, secureStorage.getSelectedValidationRegion()) })
    }
}