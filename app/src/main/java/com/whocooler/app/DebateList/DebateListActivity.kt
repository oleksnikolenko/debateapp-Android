package com.whocooler.app.DebateList

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whocooler.app.Common.Models.Category
import com.whocooler.app.Common.Models.Debate
import com.whocooler.app.Common.Models.DebateSide
import com.whocooler.app.Common.Models.DebatesResponse
import com.whocooler.app.Common.Services.DebateService
import com.whocooler.app.Common.Utilities.dip
import com.whocooler.app.DebateList.Adapters.DebateListAdapter
import com.whocooler.app.DebateList.Adapters.DebateListCategoryAdapter
import com.whocooler.app.R
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class DebateListActivity : AppCompatActivity(), DebateListContracts.PresenterViewContract {

    lateinit var interactor: DebateListContracts.ViewInteractorContract
    lateinit var router: DebateListContracts.RouterInterface
    lateinit var debateAdapter: DebateListAdapter
    private var reloadPosition: Int? = null
    private var selectedCategoryId: String = Category.Constant.ALL.id
    private var selectedSorting: String = "popular"

    private fun setup() {
        var activity = this
        var interactor = DebateListInteractor()
        var presenter = DebateListPresenter()
        var router = DebateListRouter()

        activity.interactor = interactor
        activity.router = router
        interactor.output = presenter
        presenter.output = activity
        router.activity = activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        setContentView(R.layout.activity_main)
        setupToolbar()

        interactor.getDebates(DebateListModels.DebateListRequest(Category.Constant.ALL.id, selectedSorting,true))
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.listToolBar)
        setSupportActionBar(toolbar)
        toolbar.setBackgroundColor(Color.WHITE)

        val toolbarTitle = toolbar.findViewById<TextView>(R.id.toolbar_title)
        toolbarTitle.text = "Who's Cooler"

        val toolbarProfile = toolbar.findViewById<Button>(R.id.toolbar_profile)
        toolbarProfile.setOnClickListener {
            router?.navigateToUserProfile()
        }

        val toolbarSorting = toolbar.findViewById<TextView>(R.id.toolbar_sorting)
        toolbarSorting.text = "Sorting: Popular"
        toolbarSorting.setOnClickListener {
            showSortingAlert()
        }
    }

    private fun showSortingAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sorting")

        val sortings = arrayOf("Popular", "Newest", "Oldest")

        builder.setItems(sortings) { _, which ->
            updateSorting(sortings[which])
        }

        val dialog = builder.create()

        dialog.show()
    }

    private fun updateSorting(sorting: String) {
        val toolbarSorting = findViewById<TextView>(R.id.toolbar_sorting)
        toolbarSorting.text = "Sorting: $sorting"

        selectedSorting = sorting.toLowerCase(Locale.ROOT)
        interactor.getDebates(DebateListModels.DebateListRequest(selectedCategoryId, selectedSorting))
    }

    override fun setupCategoryAdapter(categories: ArrayList<Category>) {
        val categoryClickHandler: (Category) -> Unit = { category ->
            debateAdapter.response.debates.clear()
            debateAdapter.notifyDataSetChanged()
            selectedCategoryId = category.id
            interactor.getDebates(DebateListModels.DebateListRequest(selectedCategoryId, selectedSorting))
        }

        listCategoriesRecyclerView.adapter = DebateListCategoryAdapter(categories, categoryClickHandler)

        listCategoriesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun setupDebateAdapter(response: DebatesResponse) {
        DebateService.debates = response.debates

        val voteClickHandler: (Debate, DebateSide, Int) -> PublishSubject<Debate> = { debate, debateSide, position ->
            interactor.vote(debate.id, debateSide.id, position)
        }

        val debateClickHandler: (Debate, Int) -> Unit = { debate, adapterPosition ->
            reloadPosition = adapterPosition
            router?.navigateToDebate(debate, adapterPosition)
        }

        val authRequiredHandler: () -> Unit = {
            router?.navigateToAuthorization()
        }

        debateAdapter =
            DebateListAdapter(
                response,
                voteClickHandler,
                debateClickHandler,
                authRequiredHandler
            )

        debateAdapter.notifyDataSetChanged()

        listRecyclerView.adapter = debateAdapter
        val layoutManager = LinearLayoutManager(this)
        listRecyclerView.layoutManager = layoutManager
    }

    override fun onRestart() {
        super.onRestart()

        if (reloadPosition != null)  {
            debateAdapter.response.debates = DebateService.debates
            debateAdapter.notifyItemChanged(reloadPosition!!)
        } else {
            interactor.getDebates(DebateListModels.DebateListRequest(selectedCategoryId, selectedSorting,true))
        }
    }
}