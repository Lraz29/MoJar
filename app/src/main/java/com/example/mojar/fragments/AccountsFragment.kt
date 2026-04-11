package com.example.mojar.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mojar.adapters.MemberAdapter
import com.example.mojar.models.User
import com.example.mojar.databinding.FragmentAccountsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
Fragment responsible for managing and displaying household members and sharing the invite code.
 */
class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Listener for real-time updates of household members
    private var membersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserHouseholdData()
    }

    private fun fetchUserHouseholdData() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val householdId = document.getString("householdId") ?: "No Group"

                if (_binding != null) {
                    // הצגת הקוד בתוך ה-TextView של ה-lastShareContainer (שנשנה ב-XML)
                    binding.tvHouseholdIdDisplay.text = householdId
                    binding.btnCopyCode.setOnClickListener {
                        val clipboard =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip =
                            ClipData.newPlainText("Household Code", householdId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                    }
                }

                if (householdId != "No Group") {
                    loadHouseholdMembersLive(householdId)
                }
            }
    }

    //Listens for changes in the 'users' collection for members belonging to the same household.
    private fun loadHouseholdMembersLive(householdId: String) {
        membersListener = db.collection("users")
            .whereEqualTo("householdId", householdId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AccountsFragment", "Error listening for members", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && _binding != null) {
                    val memberList = snapshot.toObjects(User::class.java)

                    setupRecyclerView(memberList)
                }
            }
    }

    private fun setupRecyclerView(memberList: List<User>) {
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberAdapter(memberList)
        }
    }

    override fun onDestroyView() {
        membersListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}