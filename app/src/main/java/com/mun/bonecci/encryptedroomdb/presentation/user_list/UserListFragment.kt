package com.mun.bonecci.encryptedroomdb.presentation.user_list

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mun.bonecci.encryptedroomdb.R
import com.mun.bonecci.encryptedroomdb.commons.ToolbarButtonVisibilityListener
import com.mun.bonecci.encryptedroomdb.commons.safeNavigate
import com.mun.bonecci.encryptedroomdb.data.models.Session
import com.mun.bonecci.encryptedroomdb.data.models.User
import com.mun.bonecci.encryptedroomdb.data.repository.LogRepositoryImpl
import com.mun.bonecci.encryptedroomdb.data.repository.SessionRepositoryImpl
import com.mun.bonecci.encryptedroomdb.databinding.FragmentUserListBinding
import com.mun.bonecci.encryptedroomdb.presentation.UserState
import com.mun.bonecci.encryptedroomdb.presentation.UserViewModel
import com.mun.bonecci.encryptedroomdb.presentation.adapter.UserListAdapter
import kotlinx.coroutines.launch
import com.mun.bonecci.encryptedroomdb.data.models.Log as LogEntity

/**
 * A fragment to display a list of users.
 */
class UserListFragment : Fragment() {

    private lateinit var viewModel: UserViewModel
    private lateinit var userListAdapter: UserListAdapter
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    private var toolbarButtonVisibilityListener: ToolbarButtonVisibilityListener? = null

    private lateinit var sessionRepository: SessionRepositoryImpl
    private lateinit var logRepository: LogRepositoryImpl

    /**
     * Called when the fragment is created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        sessionRepository = SessionRepositoryImpl(requireContext())
        logRepository = LogRepositoryImpl(requireContext())
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)

        val activity = activity as AppCompatActivity?

        activity?.let {
            val toolbarButton = it.findViewById<ImageButton>(R.id.toolbar_button)
            toolbarButton.setOnClickListener {
                viewModel.addUser()
            }
        }

        return binding.root
    }

    /**
     * Called immediately after onCreateView() and onViewCreated().
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()
        observeUserData()
        viewModel.fetchUserData()
        setToolbarButtonVisibility()

        insertBaseSessions()
        insertBaseLogs()

        fetchAndLogSessions()
        fetchAndLogLogs()
    }

    private fun insertBaseSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Перевіримо, чи вже є записи, щоб не дублювати кожен раз
                val existing = sessionRepository.getAllSessions()
                if (existing.isEmpty()) {
                    val now = System.currentTimeMillis()
                    // Перша сесія для userId = 1
                    val s1 = Session(
                        userId = 1,
                        startedAt = now - 60_000,  // почалась хвилину тому
                        endedAt = now
                    )
                    sessionRepository.insertSession(s1)

                    // Друга сесія для userId = 2 (наприклад)
                    val s2 = Session(
                        userId = 2,
                        startedAt = now - 120_000, // дві хвилини тому
                        endedAt = now - 30_000     // завершилась півхвилини тому
                    )
                    sessionRepository.insertSession(s2)

                    Log.d("SessionRepo", "Вставлено 2 базові Session.")
                }
            } catch (e: Exception) {
                Log.e("SessionRepo", "Помилка при вставці базових Session: ${e.message}")
            }
        }
    }

    /**
     * Вставляємо два «базових» логи.
     */
    private fun insertBaseLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val existing = logRepository.getAllLogs()
                if (existing.isEmpty()) {
                    val now = System.currentTimeMillis()
                    // Перший лог
                    val l1 = LogEntity(
                        message = "App started",
                        timestamp = now - 300_000
                    )
                    logRepository.insertLog(l1)

                    // Другий лог
                    val l2 = LogEntity(
                        message = "User clicked getUsers",
                        timestamp = now - 100_000
                    )
                    logRepository.insertLog(l2)

                    Log.d("LogRepo", "Вставлено 2 базові Log.")
                }
            } catch (e: Exception) {
                Log.e("LogRepo", "Помилка при вставці базових Log: ${e.message}")
            }
        }
    }

    private fun fetchAndLogSessions() {
        // Використовуємо lifecycleScope, щоб працювати з корутинами у фрагменті
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sessions: List<Session> = sessionRepository.getAllSessions()
                if (sessions.isEmpty()) {
                    Log.d("UserListFragment: SessionRepo", "Немає жодного Session в БД.")
                } else {
                    // Виведемо кожен об’єкт Session у лог
                    sessions.forEach { s ->
                        Log.d(
                            "UserListFragment: SessionRepo",
                            "Session ID=${s.id}, userId=${s.userId}, startedAt=${s.startedAt}, endedAt=${s.endedAt}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("UserListFragment: SessionRepo", "Помилка при зчитуванні Session: ${e.message}")
            }
        }
    }

    private fun fetchAndLogLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val logs: List<LogEntity> = logRepository.getAllLogs()
                if (logs.isEmpty()) {
                    Log.d("UserListFragment: LogRepo", "Немає жодного Log в БД.")
                } else {
                    // Виводимо кожен Log у лог
                    logs.forEach { l ->
                        Log.d("UserListFragment: LogRepo", "Log ID=${l.id}, message='${l.message}', timestamp=${l.timestamp}")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserListFragment: LogRepo", "Помилка при зчитуванні Log: ${e.message}")
            }
        }
    }

    /**
     * Called when the fragment is no longer in use.
     */
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * Called when a fragment is first attached to its context.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ToolbarButtonVisibilityListener) {
            toolbarButtonVisibilityListener = context
        } else {
            throw RuntimeException("$context must implement ToolbarButtonVisibilityListener")
        }
    }

    /**
     * Initializes the recycler view.
     */
    private fun initRecycler() {
        userListAdapter =
            UserListAdapter(clickListener = (object : UserListAdapter.OnClickListener {
                override fun onItemClick(user: User, position: Int) {
                    userListAdapter.setSelectedItem(position)
                    findNavController().safeNavigate(
                        R.id.action_userListFragment_to_userDetailFragment,
                        Bundle().apply {
                            putString("id", user.id.toString())
                        })
                }

                override fun onDeletePressed(user: User, position: Int) {
                    viewModel.removeUserData(user.id.toString())
                    userListAdapter.setSelectedItem(-1)
                }

            }), requireActivity())

        binding.userRecyclerView.apply {
            adapter = userListAdapter
            layoutManager = LinearLayoutManager(this.context, RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
        }
    }

    /**
     * Observes the user data changes.
     */
    private fun observeUserData() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            validateUserState(state)
        }
    }

    /**
     * Validates the user state and updates the UI accordingly.
     */
    private fun validateUserState(state: UserState) {
        state.users.let { users ->
            userListAdapter.submitList(users)
        }

        if (state.error.isNotBlank()) {
            Toast.makeText(requireActivity(), state.error, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets the visibility of toolbar button.
     */
    private fun setToolbarButtonVisibility() {
        toolbarButtonVisibilityListener?.onToolbarButtonVisibilityChanged(true)
    }
}