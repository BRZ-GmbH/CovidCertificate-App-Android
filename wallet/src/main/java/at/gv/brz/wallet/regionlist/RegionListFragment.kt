package at.gv.brz.wallet.regionlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.gv.brz.wallet.R
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
        val region = Region.getRegionFromIdentifier(secureStorage.getSelectedValidationRegion())
        if (region == null) {
            binding.regionListToolbar.setNavigationIcon(null)
        }
        binding.regionListToolbar.setNavigationOnClickListener { v: View? ->
            if (region != null) {
                parentFragmentManager.popBackStack()
            }
        }
        setupRecyclerView()
        view.announceForAccessibility(getString(R.string.wallet_region_selection_title_loaded))
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

        adapter.setItems(Region.values().sortedWith { r1, r2 ->
            resources.getString(r1.getName()).compareTo(resources.getString(r2.getName()))
        }.map { RegionItem(it, secureStorage.getSelectedValidationRegion()) })
    }
}